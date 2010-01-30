@Typed
package widefinder

import java.util.regex.Pattern
import java.util.regex.Matcher

/**
 * Statistics class
 */
class Stat extends Thread
{

    Stat ( Runnable r )
    {
        super( r )
        println "Thread [${ getName()}] created"
    }


    /**
     * Article URI prefix and matcher (one per thread instance):
     * "/ongoing/When/200x/2007/06/17/Web3S"
     */
    private static final String  ARTICLE_PREFIX  = '/ongoing/When/';
    private        final Matcher ARTICLE_MATCHER = Pattern.compile( "^$ARTICLE_PREFIX\\d{3}x/\\d{4}/\\d{2}/\\d{2}/[^ .]+\$" ).
                                                   matcher( "" );


   /**
    * Data Map
    */
    private final Map<String, UriData> data = new NoRehashMap<String, UriData>( 1024 * 1024 )
    private       Map<String, UriData> data(){ this.@data }


   /**
    * Determines if URI specified is that of an article
    */
    boolean isArticle ( String uri, String httpMethod )
    {
         (( httpMethod == 'GET' )             &&
          ( uri.startsWith( ARTICLE_PREFIX )) &&
          ( ! uri.endsWith( '.png' ))         &&
          ( ARTICLE_MATCHER.reset( uri ).lookingAt()))
    }


   /**
    * Updates statistics according to benchmark needs:
    * - http://wikis.sun.com/display/WideFinder/The+Benchmark
    * - http://groovy.codehaus.org/Regular+Expressions
    */
    void update( boolean isArticle,
                 String  clientAddress,
                 String  httpMethod,
                 String  uri,
                 String  statusCode,
                 String  byteCount,
                 String  referrer )
    {

        UriData uriData = data()[ uri ]
        if ( uriData == null ) { uriData = ( data()[ uri ] = new UriData()) }

        uriData.update( isArticle,
                        ( byteCount.contains( '-' ) ? 0 : Integer.parseInt( byteCount, 10 )),
                        ( statusCode == '404' ),
                        clientAddress,
                        ((( referrer != null ) && ( ! referrer.isEmpty()) && ( referrer != '-' )) ? referrer : null ))
    }
}
