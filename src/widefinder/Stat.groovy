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
        assert( key && ( map != null ));

        if ( ! map[ key ] ) { map[ key ] = new L() }
        L      counter = map[ key ];
        assert counter;
        return counter;
    }


    /**
     * Gets a counter bound to the key1 => key2 specified in the map passed
     * (creates a new one if it doesn't exist yet)
     */
    private static L get( Map<String, Map<String, L>> map, String key, String secondKey )
    {
        assert( key && secondKey && ( map != null ));

        if ( ! map[ key ] ) { map[ key ] = new HashMap<String, L>() }

        Map<String, L> secondMap = map[ key ];
        assert       ( secondMap != null );

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
    final Map<String, L>              articlesToHits      = new HashMap<String, L>();
    final Map<String, L>              uriToByteCounts     = new HashMap<String, L>();
    final Map<String, L>              uriTo404            = new HashMap<String, L>();
    final Map<String, Map<String, L>> articlesToClients   = new HashMap<String, Map<String, L>>();
    final Map<String, Map<String, L>> articlesToReferrers = new HashMap<String, Map<String, L>>();


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
        assert ( clientAddress &&
                 httpMethod    &&
                 uri           &&
                 statusCode    &&
                 byteCount     &&
                 ( referrer != null )); // "referrer" may be empty

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
        assert( articleUri && clientAddress );

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
        assert( uri );

        if ( bytes > 0 ) { uriToByteCountsCounter( uri ).add( bytes ) }
        if ( is404     ) { uriTo404Counter( uri ).increment()         }
    }
}
