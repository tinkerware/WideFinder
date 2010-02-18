@Typed
package widefinder

import java.nio.ByteBuffer
import java.nio.channels.FileChannel

import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor
import widefinder.maps.*


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
     * Pre-calculated powers of 10: 1, 10, 100, 1000 ...
     */
    private static final int[] TEN_POWERS = ( 0 .. 9 ).collect { int j -> ( 10 ** j ) }


    /**
     * Shared storage of all allocated Strings,
     * keyed by their checksum
     */
    private static final NoRehashMap<HashKey, String> STRINGS = new ConcurrentNoRehashMap<HashKey, String>( 10240, 0.9F );


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

        pool.shutdown();
        println "[${ System.currentTimeMillis() - t }] ms"
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

            if (( currentPosition - prevPosition ) > GB ) // Next GB processed
            {
                final long currentTime = System.currentTimeMillis();
                println "[${ ( int )( currentPosition / GB ) }] Gb - [${ ( currentTime - prevTime ) / 1000 }] sec";
                println "Cache: size - [${ STRINGS.size() }], maps - [${ STRINGS.mapsNumber() }]"
                println "Data : size - [${ Stat.DATA.size() }], maps - [${ Stat.DATA.mapsNumber() }]"

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
                 */
                futures << pool.submit({ processLines( array, startIndex, endIndex, ( Stat ) Thread.currentThread()) } as Runnable )
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
        String  uri           = null;
        String  statusCode    = null;
        int     byteCount     = 0;
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
                    case 6  : uri           = string( array, tokenStart, index );
                              isArticle     = stat.isArticle( uri )
                              break;
                    case 8  : statusCode    = string( array, tokenStart, index );
                              break;
                    case 9  : byteCount     = integer( array, tokenStart, index );
                              found         = ( ! isArticle ) // If not article - we've found everything we need
                                                              // If article     - we need to find referrer (last token)
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
                    stat.update( isArticle, clientAddress, uri, statusCode, byteCount, referrer );

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
                     uri           = null;
                     statusCode    = null;
                     byteCount     = 0;
                     referrer      = null;
                }
            }
        }
    }


   /**
    * Parses byte[] area specified as positive int number
    * @return integer parsed or zero if area specified contains non-digits
    */
    private static int integer( byte[] array, int start, int end )
    {
        int result = 0

        for ( int j = start; j < end; j++ )
        {
            byte b = array[ j ]

            if (( b < 48 ) || ( b > 57 )) { return 0 }

            result += (( b - 48 ) * TEN_POWERS[ end - j - 1 ] )
        }

        return result;
    }


   /**
    * Creates a String using the array specified and "UTF-8" charset.
    * String starts at index "start" and ends at index "end - 1"
    */
    private static String string( byte[] array, int start, int end )
    {
        end            = Math.min( end, start + 256 )
        HashKey key    = hashkey( array, start, end )
        String  sCache = STRINGS.get( key )

        if ( sCache )
        {
            return sCache
        }
        else
        {
            String sNew = new String( array, 0, start, ( end - start ))
            STRINGS.put( key, sNew )
            return sNew
        }
    }




    /**
     * Calculates {@link HashKey} for the <code>byte[]</code> area specified
     */
    private static HashKey hashkey( byte[] array, int start, int end )
    {
        int hashcode = 0
        int n        = ( end - start )
        int A        = 1
        int B        = n

        // String hashcode : http://www.docjar.com/html/api/java/lang/String.java.html
        // Adler32 checksum: http://en.wikipedia.org/wiki/Adler-32

        for ( int j = 0; j < n; j++ )
        {
            byte b    = array[ j + start ]
            hashcode  = (( 31 * hashcode ) + b )
            A        += b                 // A = 1 + D1 + D2 + ... + Dn (mod 65521)
            B        += (( n - j ) * b )  // B = n×D1 + (n-1)×D2 + (n-2)×D3 + ... + Dn + n (mod 65521)
        }

        long checksum = (( B % 65521 ) * 65536 ) + ( A % 65521 )
        new HashKey( hashcode, checksum )
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