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
    private static final int N = 10;


    /**
     * Character constants
     */
    private static final byte CR    = 0x0D; // "\r"
    private static final byte LF    = 0x0A; // "\n"
    private static final byte SPACE = 0x20; // " "


    public static void main ( String[] args )
    {
        final long t       = System.currentTimeMillis();
        final File file    = new File( args[ 0 ] );
        final int  coreNum = Runtime.getRuntime().availableProcessors()

        assert file.isFile(), "File [$file] is not available" ;

        ExecutorService pool       = Executors.newFixedThreadPool( coreNum, [ newThread : { Runnable r -> new Stat( r ) } ] );
        int             bufferSize = Math.min( file.size(), ( 10 * 1024 * 1024 ));
        ByteBuffer      buffer     = ByteBuffer.allocate( bufferSize );
        FileInputStream fis        = new FileInputStream( file );
        FileChannel     channel    = fis.getChannel();

        processChannel( channel, buffer, pool, coreNum );

        channel.close();
        fis.close();
        pool.shutdown();

/*
        Map<String, Long> topArticles = StatUtils.top( N, stat.articlesToHits());

        report( "Top $N articles (by hits)",          topArticles );
        report( "Top $N URIs (by bytes count)",       StatUtils.top( N, stat.uriToByteCounts()));
        report( "Top $N URIs (by 404 responses)",     StatUtils.top( N, stat.uriTo404()));
        report( "Top $N clients (by hot articles)",   StatUtils.top( N, topArticles, stat.articlesToClients()));
        report( "Top $N referrers (by hot articles)", StatUtils.top( N, topArticles, stat.articlesToReferrers()));
*/

        println "[${ System.currentTimeMillis() - t }] ms"
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
        for ( int remaining = 0; ( channel.position() < channel.size()); )
        {
            int     bytesRead = channel.read( buffer );
            byte[]  array     = buffer.array();
            boolean isEof     = ( channel.position() == channel.size());

            assert (( bytesRead > 0 ) &&
                        (( bytesRead + remaining ) == buffer.position()) &&
                            ( buffer.position()    <= array.length ));

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

                futures << pool.submit(( Runnable ) [ run : { processLines( array, startIndex, endIndex, (( Stat ) Thread.currentThread()));
                                                              println "[${ Thread.currentThread()}]"
                                                            }])
                startIndex = endIndex;
            }

            futures*.get();

            buffer.position( startIndex );  // Moving buffer's position a little back to last known "endIndex"
            remaining = buffer.remaining(); // How many bytes are left unread in buffer
            buffer.compact();               // Copying remaining (unread) bytes to beginning of buffer
                                            // Next file read will be added to them
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

                if ( referrer != null )
                {
                   /**
                    * We've found all tokens ("referrer" was the last one) - updating statistics
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
                else
                {
                    /**
                     * Not all tokens are found yet - keep looking for the next one
                     * (skipping "space" sequence)
                     */
                    while ( space( array[ index ] )){ index++ }
                    tokenStart = index;
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