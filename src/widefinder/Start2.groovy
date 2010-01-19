package widefinder

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.*


@Typed
class Start2
{
   /**
    * Top N counter
    */
    static final int N = 10;


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
        final ThreadPoolExecutor pool       =
            ( ThreadPoolExecutor ) Executors.newFixedThreadPool(
                    200,
                    { Runnable r -> new Stat( r ) });

        processFile( file, pool );

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
        pool.getCorePoolSize().times
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

    static final ByteBuffer stop = ByteBuffer.allocate(1)

    static void processChannel (FileChannel channel, long start, long end) {
    }

   /**
    * Reads number of lines in the channel specified
    */
    private static void processFile ( File file, ThreadPoolExecutor pool )
    {
        final def fis     = new FileInputStream( file );
        final def channel = fis.getChannel();

        final ArrayBlockingQueue<ByteBuffer> queue       = new ArrayBlockingQueue<ByteBuffer>(4)
        final ArrayBlockingQueue<ByteBuffer> freeBuffers = new ArrayBlockingQueue<ByteBuffer>(4)

        pool.execute {
            while (true) {
                def buffer = queue.take()
                if (buffer === stop)
                    break;

                processLines(buffer.array(), 0, buffer.position(), (Stat) Thread.currentThread())

                freeBuffers << buffer
            }

            List<Map<Long, Collection<String>>> topMap = ((Stat)Thread.currentThread()).calculateTop( n );

            Map<String, Long>                         topArticlesToHits = StatUtils.sumAndTop( n, [topMap.get( 0 )])
            Map<String, Long>                         topUrisToBytes    = StatUtils.sumAndTop( n, [topMap.get( 1 )])
            Map<String, Long>                         topUrisTo404      = StatUtils.sumAndTop( n, [topMap.get( 2 )])

            List<Map<String, L>> topArticlesMaps = (( Stat ) Thread.currentThread()).filterWithArticles( topArticlesToHits.keySet())

            Map<String, Long>          topClientsToArticles   = StatUtils.sumAndTop2( n, [topArticlesMaps.get( 0 )])
            Map<String, Long>          topReferrersToArticles = StatUtils.sumAndTop2( n, [topArticlesMaps.get( 1 )])

            report( "Top $n articles (by hits)",          topArticlesToHits      );
            report( "Top $n URIs (by bytes count)",       topUrisToBytes         );
            report( "Top $n URIs (by 404 responses)",     topUrisTo404           );
            report( "Top $n clients (by top articles)",   topClientsToArticles   );
            report( "Top $n referrers (by top articles)", topReferrersToArticles );
        }

        for(i in 0..<2)
            freeBuffers << ByteBuffer.allocate( 1024*1024 );

        def size = channel.size()
        while (channel.position() < size)
        {
            final ByteBuffer buffer = freeBuffers.take ()

            buffer.rewind ()
            channel.read( buffer );

            int endIndex = findLastEol(buffer)

            if (++endIndex == 0)
                endIndex = buffer.position();
            channel.position(channel.position()-(buffer.position()-endIndex));
            buffer.position (endIndex)

            queue << buffer
        }

        queue << stop

        channel.close();
        fis.close();
    }

    private static int findLastEol(ByteBuffer buffer) {
        int endIndex = buffer.position() - 1
        byte [] array = buffer.array()
        for (; endIndex >= 0; endIndex--) {
            if (array[endIndex] == LF || array[endIndex] == CR) {
                break;
            }
        }
        return endIndex
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
        new String( array, 0, start, ( end - start ) );
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