package widefinder

import java.util.regex.Pattern
import java.util.regex.Matcher

/**
 * Statistics class
 */
@Typed
class Stat extends Thread
{

    Stat ( Runnable r )
    {
        super( r )
    }



   /**
    * Gets a counter bound to the key specified in the map passed
    * (creates a new one if it doesn't exist yet)
    */
    private static L get( Map<String, L> map, String key )
    {
        L      counter = map[ key ];
        if ( counter == null) { map[ key ] = (counter = new L()) }
        return counter;
    }


    /**
     * Gets a counter bound to the key1 => key2 specified in the map passed
     * (creates a new one if it doesn't exist yet)
     */
    private static L get( Map<String, Map<String, L>> map, String key, String secondKey )
    {
        Map<String, L> secondMap = map[ key ];
        if ( map[key] == null ) { map[ key ] = (secondMap = new HashMap<String, L>()) }
        return get( secondMap, secondKey );
    }


    /**
     * Article URI pattern: "/ongoing/When/200x/2007/06/17/Web3S"
     */
    private static final String  ARTICLE_PREFIX  = '/ongoing/When/';
    private        final Matcher ARTICLE_MATCHER = Pattern.compile( "^$ARTICLE_PREFIX\\d{3}x/\\d{4}/\\d{2}/\\d{2}/[^ .]+\$" ).
                                                   matcher( "" );

   /**
    * Maps holding all statistical data
    */
    Map<String, L>              articlesToHits      = new HashMap<String, L>();
    Map<String, L>              uriToByteCounts     = new HashMap<String, L>();
    Map<String, L>              uriTo404            = new HashMap<String, L>();
    Map<String, Map<String, L>> articlesToClients   = new HashMap<String, Map<String, L>>();
    Map<String, Map<String, L>> articlesToReferrers = new HashMap<String, Map<String, L>>();


   /**
    * Statistical maps counters convenience accessors
    */
    private L articlesToHitsCounter      ( String articleUri )                       { get( this.@articlesToHits,      articleUri  ) }
    private L uriToByteCountsCounter     ( String uri        )                       { get( this.@uriToByteCounts,     uri         ) }
    private L uriTo404Counter            ( String uri        )                       { get( this.@uriTo404,            uri         ) }
    private L clientsToArticlesCounter   ( String articleUri, String clientAddress ) { get( this.@articlesToClients,   articleUri, clientAddress ) }
    private L referrersToArticlesCounter ( String articleUri, String referrer      ) { get( this.@articlesToReferrers, articleUri, referrer      ) }



   /**
    * Updates statistics according to benchmark needs:
    * - http://wikis.sun.com/display/WideFinder/The+Benchmark
    * - http://groovy.codehaus.org/Regular+Expressions
    */
    void update( String clientAddress, String httpMethod, String uri, String statusCode, String byteCount, String referrer )
    {
        boolean isArticle = (( httpMethod == 'GET' )             &&
                             ( uri.startsWith( ARTICLE_PREFIX )) &&
                             ( ! uri.endsWith( '.png' ))         &&
                             ( ARTICLE_MATCHER.reset( uri ).lookingAt()));

        if ( isArticle )
        {
            addArticle( uri,
                        clientAddress,
                        ((( ! referrer.isEmpty()) && ( referrer != '-' )) ? referrer : null ));
        }

        addUri( uri,
                ( byteCount.contains( '-' ) ? 0 : Integer.parseInt( byteCount, 10 )),
                ( statusCode == '404' ));
    }


    /**
     * Adds new articles to statistics
     *
     * @param articleUri    URI of article
     * @param clientAddress address of requesting client
     * @param referrer      request referrer
     */
    private void addArticle( String articleUri, String clientAddress, String referrer )
    {
        articlesToHitsCounter( articleUri ).increment();
        clientsToArticlesCounter( articleUri, clientAddress ).increment();

        if ( referrer )
        {
            referrersToArticlesCounter( articleUri, referrer ).increment()
        }
    }


    /**
     * Adds new URI to statistics
     *
     * @param uri   request URI
     * @param bytes request total bytes returned
     * @param is404 whether a response was 404
     */
    private void addUri( String uri, int bytes, boolean is404 )
    {
        if ( bytes > 0 ) { uriToByteCountsCounter( uri ).add( bytes ) }
        if ( is404     ) { uriTo404Counter( uri ).increment()         }
    }



   /**
    * Calculates "top N" counters for:
    * - 'articles to hits' map
    * - 'uri to bytes' map
    * - 'uri to missed (404)' map
    *
    * Returns a list of 3 Map<Long, Collection<String>>.
    * Each map:
    * Key   - a counter in the "top N" counters
    * Value - collection of String values corresponding to that counter
    */
    List<Map<Long, Collection<String>>> calculateTop( int n )
    {
        List<Map<Long, Collection<String>>> result =
        [
            StatUtils.topCountersMap( n, getArticlesToHits()),
            StatUtils.topCountersMap( n, getUriToByteCounts()),
            StatUtils.topCountersMap( n, getUriTo404())
        ]

        /**
         * Nuking raw data the moment it's no longer needed
         */
        setArticlesToHits ( null );
        setUriToByteCounts ( null );
        setUriTo404 ( null );

        return result;
    }



   /**
    * Filters article-related Maps:
    * - 'articles to clients' map
    * - 'articles to referrers' map
    *
    * Returns a list of 2 Map<String, L>
    * Each map:
    * Key   - client address / referrer (accessing one of "top articles")
    * Value - counter, a number of time this access was made
    */
    List<Map<String, L>> filterWithArticles( Set<String> topArticles )
    {
        List<Map<String, L>> result =
        [
            filterWithArticles( topArticles, getArticlesToClients()),
            filterWithArticles( topArticles, getArticlesToReferrers()),
        ]

        /**
         * Nuking raw data the moment it's no longer needed
         */
        setArticlesToClients( null );
        setArticlesToReferrers( null );

        return result;
    }



   /**
    * Filters an article-related "valuesMaps" (like map of clients or referrers) with "top articles" specified
    */
    private Map<String, L> filterWithArticles( Set<String> topArticles, Map<String, Map<String, L>> valuesMaps )
    {
        /**
         * Maps (String value => articles hits) of "top articles" accesses
         * One map for each "top article" - they're summarized below into one bigger Map<String, L>
         */
        List<Map<String, L>> topArticlesMaps = valuesMaps.keySet().
                                              /**
                                               * Filtering out URIs which are not from "top articles" Set
                                               */
                                               findAll{ String articleUri -> topArticles.contains( articleUri )}.
                                              /**
                                               * Collecting a Map<String, L> (String value => article hits counter)
                                               * for each URI that is left after "top articles" filtering
                                               */
                                               collect{ String articleUri -> valuesMaps[ articleUri ] };

        /**
         * Result map (String value => articles hits):
         * summarizing all above (smaller) maps for all "top articles" (one smaller map per "top article")
         * into one bigger Map<String, L>
         */
        Map<String, L> resultMap = new HashMap<String,L>();
        topArticlesMaps.each
        {
            Map<String, L> map ->
            map.each
            {
                String value, L counter ->

                if ( ! resultMap[ value ] ) { resultMap[ value ] = new L() }
                resultMap[ value ].add( counter.counter());
            }
        }

        return resultMap;
    }
}
