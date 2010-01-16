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
        if ( ! map[ key ] ) { map[ key ] = new L() }
        L      counter = map[ key ];
        return counter;
    }


    /**
     * Gets a counter bound to the key1 => key2 specified in the map passed
     * (creates a new one if it doesn't exist yet)
     */
    private static L get( Map<String, Map<String, L>> map, String key, String secondKey )
    {
        if ( ! map[ key ] ) { map[ key ] = new HashMap<String, L>() }
        Map<String, L> secondMap = map[ key ];
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
                (( byteCount != '-' ) ? Integer.valueOf( byteCount ) : 0 ),
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
    * Returns a list of 3 "top N counters" maps.
    * Each map:
    * Key   - a counter in the "top N" counters
    * Value - list of String values corresponding to that counter
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
         * Nuking the raw data the moment it's no longer needed
         */
        setArticlesToHits ( null );
        setUriToByteCounts ( null );
        setUriTo404 ( null );

        return result;
    }


    Map<String, L> filterWithArticles( int n, Set<String> topArticles )
    {
        /**
         * Maps (client address => articles hits) of clients accessing "hot articles"
         * One map for each "hot article"
         */
        List<Map<String, L>> topArticlesClients = getArticlesToClients().keySet().
                                                  // Filtering all known articles URIs with "top articles"
                                                  findAll{ String articleUri -> topArticles.contains( articleUri )}.
                                                  // Collecting a Map<String, L> (client address => article hits counter)
                                                  // of clients for each filtered "article URI"
                                                  collect{ String articleUri -> getArticlesToClients()[ articleUri ] };

        Map<String, L> clientsMap = new HashMap<String,L>();
        topArticlesClients.each
        {
            Map<String, L> map ->
            map.each
            {
                String clientAddress, L counter ->

                if ( ! clientsMap[ clientAddress ] ) { clientsMap[ clientAddress ] = new L() }
                clientsMap[ clientAddress ].add( counter.counter());
            }
        }

        return clientsMap;
    }
}
