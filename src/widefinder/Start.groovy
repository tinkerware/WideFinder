package widefinder

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.*


@Typed
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
     * Character constants
     */
    private static final byte CR    = 0x0D; // "\r"
    private static final byte LF    = 0x0A; // "\n"
    private static final byte SPACE = 0x20; // " "


    public static void main ( String ... args )
    {
        final File file    = new File( args[ 0 ] );
        assert file.isFile(), "File [$file] is not available" ;

        final long               t          = System.currentTimeMillis();
        final int                coreNum    = Runtime.getRuntime().availableProcessors()
        final ThreadPoolExecutor pool       = ( ThreadPoolExecutor ) Executors.newFixedThreadPool( coreNum, { Runnable r -> new Stat( r ) } );
        final int                bufferSize = Math.min( file.size(), BUFFER_SIZE );
        final ByteBuffer         buffer     = ByteBuffer.allocate( bufferSize );
        final FileInputStream    fis        = new FileInputStream( file );
        final FileChannel        channel    = fis.getChannel();

        processChannel( channel, buffer, pool, coreNum );

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
        pool.getPoolSize().times
        {
            /**
             * Each thread calculates it's own "top n" maps
             */
            futures << pool.submit({ (( Stat ) Thread.currentThread()).calculateTop( n ) })
        }

        List<List<Map<Long, Collection<String>>>> topMaps           = futures*.get()
        Map<String, Long>                         topArticlesToHits = StatUtils.sumAndTop( n, topMaps*.get( 0 ));
        Map<String, Long>                         topUrisToBytes    = StatUtils.sumAndTop( n, topMaps*.get( 1 ));
        Map<String, Long>                         topUrisTo404      = StatUtils.sumAndTop( n, topMaps*.get( 2 ));

        futures = [];
        pool.getPoolSize().times
        {
            /**
             * Each thread calculates it's own "top n clients/referrers" maps
             * (according to "top articles" calculated previously)
             */
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
    }


    static void report( String title, Map<String, Long> map )
    {
        println ">>> $title <<<: \n* ${ map.entrySet().collect{ Map.Entry entry -> "${ entry.key } : ${ entry.value }" }.join( "\n* " ) }"
    }


   /**
    * Reads number of lines in the channel specified
    */
    private static void processChannel ( FileChannel channel, ByteBuffer buffer, ExecutorService pool, int poolSize )
    {
        buffer.rewind();

        /**
         * Reading from file channel into buffer (until it ends)
         */
        while ( channel.position() < channel.size())
        {
            int     bytesRead = channel.read( buffer );
            byte[]  array     = buffer.array();
            boolean isEof     = ( channel.position() == channel.size());

            /**
             * Iterating through buffer, giving each thread it's own byte[] chunk to analyze:
             *
             * "startIndex" - byte[] index where chunk starts (inclusive)
             * "endIndex"   - byte[] index where chunk ends (exclusive)
             * "chunkSize"  - approximate size of byte[] chunk to be given to each thread             *
             * "chunk"      - array[ startIndex ] - array[ endIndex - 1 ]
             */
            int startIndex = 0;
            int chunkSize  = ( buffer.position() / poolSize );

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
                     * (that usually collect bytes left from the previous read) - expanding
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
                 */
                futures << pool.submit({ processLines( array, startIndex, endIndex, (( Stat ) Thread.currentThread())) })
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
                              break;
                    case 8  : statusCode    = string( array, tokenStart, index );
                              break;
                    case 9  : byteCount     = string( array, tokenStart, index );
                              break;
                    case 10 : referrer      = string( array, tokenStart + 1, index - 1 ); // Getting rid of wrapping '"'
                              break;
                }

                if ( referrer == null )
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
                     * We've found all tokens ("referrer" was the last one) - updating statistics,
                     * adding the data read from the current line
                     */
                    stat.update( clientAddress, httpMethod, uri, statusCode, byteCount, referrer );

                    /**
                     * Finding and skipping "end of line" sequence
                     */
                     while (( index < endIndex ) && ( ! endOfLine( array[ index ] ))){ index++ }
                     while (( index < endIndex ) && (   endOfLine( array[ index ] ))){ index++ }

                     tokenStart    = index;
                     tokenCounter  = 0;

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
        new String( array, start, ( end - start ), "UTF-8" );
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