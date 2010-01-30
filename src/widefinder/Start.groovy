@Typed
package widefinder

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor


class Start
{
   /**
    * Top N counter
    */
    static final int N = 10;


   /**
    * Buffer size (in megabytes) for reading the file
    */
    private static final int BUFFER_SIZE = ( 10 * 1024 * 1024 );


   /**
    * 1MB = 1024 * 1024 (2^20)
    * 1GB = 1024 * 1024 * 1024 (2^30)
    */
    private static final long MB = ( 2 ** 20 );
    private static final long GB = ( 2 ** 30 );


   /**
    * Number of available processors
    */
    private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();


    /**
     * Character constants
     */
    private static final byte CR    = 0x0D; // "\r"
    private static final byte LF    = 0x0A; // "\n"
    private static final byte SPACE = 0x20; // " "


    public static void main ( String ... args )
    {
        final File file    = new File( args[ 0 ] );
        assert file.isFile(), "File [$file] is not available" ;

        final long               t           = System.currentTimeMillis();
        final ThreadPoolExecutor pool        =
            ( ThreadPoolExecutor ) Executors.newFixedThreadPool( AVAILABLE_PROCESSORS, { Runnable r -> new Stat( r ) });
        final int                bufferSize  = Math.min( file.size(), BUFFER_SIZE );
        final ByteBuffer         buffer      = ByteBuffer.allocate( bufferSize );
        final FileInputStream    fis         = new FileInputStream( file );
        final FileChannel        channel     = fis.getChannel();

        println "File size [${ ( int )( channel.size() / GB ) }] Gb, [$AVAILABLE_PROCESSORS] processors";

        processChannel( channel, buffer, pool );

        channel.close();
        fis.close();

        reportTopResults( N, pool );

        pool.shutdown();
        println "[${ System.currentTimeMillis() - t }] ms"
    }



   /**
    * Reports all results
    */
    private static void reportTopResults ( int n, ThreadPoolExecutor pool )
    {
        List<Future> futures = [];

        pool.getCorePoolSize().times
        {
            futures << pool.submit(
            {
               final Stat        stat = ( Stat ) Thread.currentThread()
               final NoRehashMap data = stat.getData()

               println "Thread [$stat], data: size - [${ data.size() }], maps - [${ data.mapsNumber() }]"
            })
        }


/*
        pool.getCorePoolSize().times
        {
            */
/**
             * Each thread calculates it's own "top n" maps
             *//*

            futures << pool.submit({ (( Stat ) Thread.currentThread()).calculateTop( n ) })
        }

        List<List<Map<Long, Collection<String>>>> topMaps           = futures*.get()
        Map<String, Long>                         topArticlesToHits = StatUtils.sumAndTop( n, topMaps*.get( 0 ));
        Map<String, Long>                         topUrisToBytes    = StatUtils.sumAndTop( n, topMaps*.get( 1 ));
        Map<String, Long>                         topUrisTo404      = StatUtils.sumAndTop( n, topMaps*.get( 2 ));

        futures = [];
        pool.getCorePoolSize().times
        {
            */
/**
             * Each thread calculates it's own "top n clients/referrers" maps
             * (according to "top articles" calculated previously)
             *//*

            futures << pool.submit({ (( Stat ) Thread.currentThread()).filterWithArticles( topArticlesToHits.keySet()) })
        }

        List<List<Map<String, L>>> topArticlesMaps        = futures*.get();
        Map<String, Long>          topClientsToArticles   = StatUtils.sumAndTop2( n, topArticlesMaps*.get( 0 ));
        Map<String, Long>          topReferrersToArticles = StatUtils.sumAndTop2( n, topArticlesMaps*.get( 1 ));

        report( "Top $n articles (by hits)",          topArticlesToHits      );
        report( "Top $n URIs (by bytes count)",       topUrisToBytes         );
        report( "Top $n URIs (by 404 responses)",     topUrisTo404           );
        report( "Top $n clients (by top articles)",   topClientsToArticles   );
        report( "Top $n referrers (by top articles)", topReferrersToArticles );
*/
    }


    static void report( String title, Map<String, Long> map )
    {
        println ">>> $title <<<: \n* ${ map.entrySet().collect{ Map.Entry entry -> "${ entry.key } : ${ entry.value }" }.join( "\n* " ) }"
    }


   /**
    * Reads number of lines in the channel specified
    */
    private static void processChannel ( FileChannel channel, ByteBuffer buffer, ThreadPoolExecutor pool )
    {
        buffer.rewind();

        long prevPosition = 0;
        long prevTime     = System.currentTimeMillis();

        /**
         * Reading from file channel into buffer (until it ends)
         */
        while ( channel.position() < channel.size())
        {
            final long currentPosition = channel.position()

//            println ">>> [${ ( currentPosition / MB )}] Mb"

            if (( currentPosition - prevPosition ) > GB )
            {
                final long currentTime = System.currentTimeMillis();
                println "[${ ( int )( currentPosition / GB ) }] Gb - [${ ( currentTime - prevTime ) / 1000 }] sec";

                prevPosition = currentPosition;
                prevTime     = currentTime;
            }

            int     bytesRead = channel.read( buffer );
            byte[]  array     = buffer.array();
            boolean isEof     = ( currentPosition == channel.size());

            /**
             * Iterating through buffer, giving each thread it's own byte[] chunk to analyze:
             *
             * "startIndex" - byte[] index where chunk starts (inclusive)
             * "endIndex"   - byte[] index where chunk ends (exclusive)
             * "chunkSize"  - approximate size of byte[] chunk to be given to each thread
             * "chunk"      - array[ startIndex ] - array[ endIndex - 1 ]
             */
            int startIndex = 0;
            int chunkSize  = ( buffer.position() / pool.getCorePoolSize());

           /**
            * When chunk size is too small - we leave only a single chunk for a single thread
            */
            if ( chunkSize < 1024 ) { chunkSize = buffer.position() }

            /**
             * List of Futures from processing threads
             */
            List<Future> futures = [];

            for ( int endIndex = chunkSize; ( endIndex <= buffer.position()); endIndex += chunkSize )
            {
                if ((( buffer.position() - endIndex ) < chunkSize ) && ( isEof ))
                {
                    /**
                     * We're too close to end of buffer and there will be no more file reads
                     * (that normally would collect bytes left from the previous read) - expanding
                     * "endIndex" to the end current buffer
                     */
                    endIndex = buffer.position();
                }
                else
                {
                    /**
                     * Looking for closest "end of line" bytes sequence (that may spread over multiple bytes)
                     * so that array[ endIndex - 1 ] is an *end* of "end of line" bytes sequence
                     */

                    while (( endIndex < buffer.position()) && (   endOfLine( array[ endIndex     ] ))) { endIndex++ }
                    while (( endIndex > 0 )                && ( ! endOfLine( array[ endIndex - 1 ] ))) { endIndex-- }
                }

                /**
                 * Each thread analyzes it's own byte[] area and updates Stat instance (which is the thread itself)
                 * Creating final copies: in dynamic Groovy without finals - variables were bounded differently
                 */
                // http://groups.google.com/group/groovyplusplus/browse_thread/thread/61ed96c6e40b7c4a
                // final int threadStartIndex = startIndex;
                // final int threadEndIndex   = endIndex;

                futures << pool.submit(
                {
                    final long  t    = System.currentTimeMillis()
                    final Stat  stat = ( Stat ) Thread.currentThread()
                    final float mb   = (( endIndex - startIndex ) / MB )
                    processLines( array, startIndex, endIndex, stat )
//                    println "Thread [${ stat.getName()}] - [${ System.currentTimeMillis() - t }] ms ([$mb] Mb)"
                })

                startIndex = endIndex;
            }

            /**
             * Blocking till each thread finishes
             */
            futures*.get();

            /**
             * Moving buffer's position back to "startIndex" (last known "endIndex")
             * and copying (compacting) remaining (unread) bytes to the beginning of buffer -
             * next file read will be added to them
             */
            buffer.position( startIndex );
            buffer.compact();
        }
    }



   /**
    * This is where each thread gets it's own byte[] chunk to analyze:
    * - it starts at index "startIndex"
    * - it ends   at index "endIndex" - 1
    * - it contains a number of complete rows (no half rows)
    */
    static void processLines( byte[] array, int startIndex, int endIndex, Stat stat )
    {
        boolean isArticle     = false;
        boolean found         = false;
        String  clientAddress = null;
        String  httpMethod    = null;
        String  uri           = null;
        String  statusCode    = null;
        String  byteCount     = null;
        String  referrer      = null;

        int     tokenStart    = startIndex;
        int     tokenCounter  = 0;

        for ( int index = startIndex; index < endIndex; index++ ) // "index" is incremented manually - Range doesn't fit here
        {
            if ( space( array[ index ] ))
            {
                switch ( tokenCounter++ )
                {
                    case 0  : clientAddress = string( array, tokenStart, index );
                              break;
                    case 5  : httpMethod    = string( array, tokenStart + 1, index ); // Getting rid of starting '"'
                              break;
                    case 6  : uri           = string( array, tokenStart, index );
                              isArticle     = stat.isArticle( uri, httpMethod )
                              break;
                    case 8  : statusCode    = string( array, tokenStart, index );
                              break;
                    case 9  : byteCount     = string( array, tokenStart, index );
                              found         = ( ! isArticle ) // If not article - we've found everything we need
                                                              // If article     - we need to find referrer (next token)
                              break;
                    case 10 : referrer      = string( array, tokenStart + 1, index - 1 ); // Getting rid of wrapping '"'
                              found         = true
                              break;
                }

                if ( ! found )
                {
                    /**
                     * Not all tokens are found yet - keep looking for the next one
                     * (skipping "space" sequence)
                     */
                    while ( space( array[ index ] )){ index++ }
                    tokenStart = index;
                }
                else
                {
                    /**
                     * We've found all tokens - updating statistics:
                     * adding the data read from the current line
                     */
                    stat.update( isArticle, clientAddress, httpMethod, uri, statusCode, byteCount, referrer );

                    /**
                     * Finding and skipping "end of line" sequence
                     */
                     while (( index < endIndex ) && ( ! endOfLine( array[ index ] ))){ index++ }
                     while (( index < endIndex ) && (   endOfLine( array[ index ] ))){ index++ }

                     tokenStart    = index;
                     tokenCounter  = 0;

                     isArticle     = false;
                     found         = false;
                     clientAddress = null;
                     httpMethod    = null;
                     uri           = null;
                     statusCode    = null;
                     byteCount     = null;
                     referrer      = null;
                }
            }
        }
    }


   /**
    * Creates a String using the array specified and "UTF-8" charset.
    * String starts at index "start" and ends at index "end - 1"
    */
    private static String string( byte[] array, int start, int end )
    {
        int     length  = ( end - start );
        boolean tooLong = ( length > 300 );
        if    ( tooLong ) { length = 300 }
        new String( array, 0, start, length ) + ( tooLong ? "...(truncated)" : "" )
    }


   /**
    * Determines if byte specified is a space character
    */
    private static boolean space( byte b )
    {
        ( b == SPACE );
    }


   /**
    * Determines if byte specified is an end-of-line character
    */
    private static boolean endOfLine( byte b )
    {
        ( b == LF )
    }
}