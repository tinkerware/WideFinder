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
    final Map<String, ? extends UriData> data = new NoRehashMap<String, ? extends UriData>( 10 * 1024 )


   /**
    * Determines if URI specified is that of an article
    */
    boolean isArticle ( String uri )
    {
         (( uri.startsWith( ARTICLE_PREFIX )) &&
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
                 String  uri,
                 String  statusCode,
                 int     byteCount,
                 String  referrer )
    {

        UriData uriData = getData()[ uri ]
        boolean is404   = ( statusCode == '404' )

        if ( uriData == null ) { uriData = ( getData()[ uri ] = ( isArticle ? new ArticleUriData() : new UriData())) }

        if ( isArticle )
        {
            (( ArticleUriData ) uriData ).update( byteCount,
                                                  is404,
                                                  clientAddress,
                                                  (( referrer && ( referrer.length() > 1 )) ? referrer : null ))
        }
        else
        {
            uriData.update( byteCount, is404 )
        }
    }
}
