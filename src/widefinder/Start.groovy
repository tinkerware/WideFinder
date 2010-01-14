package widefinder

import java.nio.ByteBuffer
import java.nio.channels.FileChannel

import java.util.regex.Pattern


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
    private static final byte BN    = 0x0A; // "\n"
    private static final byte SPACE = 0x20; // " "


   /**
    * Article URI pattern: "/ongoing/When/200x/2007/06/17/Web3S"
    */
    private static final String  ARTICLE_PREFIX  = '/ongoing/When/';
    private static final Pattern ARTICLE_PATTERN = Pattern.compile( "^$ARTICLE_PREFIX\\d{3}x/\\d{4}/\\d{2}/\\d{2}/[^ .]+\$" );


    public static void main ( String[] args )
    {
        long   t            = System.currentTimeMillis();
        int    bufferSizeMb = 10;
        int    cpuNum       = Runtime.getRuntime().availableProcessors();
        File   file         = new File( args[ 0 ] );
        Stat   stat         = new Stat();

        assert file.isFile();

        int             bufferSize = Math.min( file.size(), ( bufferSizeMb * 1024 * 1024 ));
        ByteBuffer      buffer     = ByteBuffer.allocate( bufferSize );
        FileInputStream fis        = new FileInputStream( file );
        FileChannel     channel    = fis.getChannel();
        long            lines      = processLines( channel, buffer, cpuNum, stat );

        channel.close();
        fis.close();

        Map<String, Long> topArticles = Stat.top( N, stat.articlesToHits());

        report( "Top $N articles (by hits)",          topArticles );
        report( "Top $N URIs (by bytes count)",       Stat.top( N, stat.uriToByteCounts()));
        report( "Top $N URIs (by 404 responses)",     Stat.top( N, stat.uriTo404()));
        report( "Top $N clients (by hot articles)",   Stat.top( N, topArticles, stat.articlesToClients()));
        report( "Top $N referrers (by hot articles)", Stat.top( N, topArticles, stat.articlesToReferrers()));

        println "[$lines] lines, [${ System.currentTimeMillis() - t }] ms"
    }


    static void report( String title, Map<String, Long> map )
    {
        println ">>> $title <<<: \n* ${ map.entrySet().collect{ Map.Entry entry -> "${ entry.key } : ${ entry.value }" }.join( "\n* " ) }"
    }


   /**
    * Reads number of lines in the channel specified
    */
    private static long processLines ( FileChannel channel, ByteBuffer buffer, int cpuNum, Stat stat )
    {
        buffer.rewind();

        long totalLines     = 0;
        long totalBytesRead = 0;

        /**
         * Reading from file channel into buffer (until it ends)
         */
        for ( int remaining = 0; ( channel.position() < channel.size()); )
        {
            int  bytesRead  = channel.read( buffer );
            totalBytesRead += bytesRead;
            byte[] array    = buffer.array();
            boolean isEof   = ( channel.position() == channel.size());

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
            int chunkSize  = ( buffer.position() / cpuNum );

           /**
            * When chunk size is too small - we leave only a single chunk for a single thread
            */
            if ( chunkSize < 1024 ) { chunkSize = buffer.position() }

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

                assert (( startIndex == 0 ) || (( startIndex > 0 ) && endOfLine( array[ startIndex - 1 ] )));
                assert (                        ( endIndex   > 0 ) && endOfLine( array[ endIndex   - 1 ] ));
                assert (                                    ( ! endOfLine( array[ startIndex ] )));
                assert (( endIndex == buffer.position()) || ( ! endOfLine( array[ endIndex ]   )));

                totalLines += processLinesPerThread( array, startIndex, endIndex, stat );
                startIndex  = endIndex;
            }

            buffer.position( startIndex );  // Moving buffer's position a little back to last known "endIndex"
            remaining = buffer.remaining(); // How many bytes are left unread in buffer
            buffer.compact();               // Copying remaining (unread) bytes to beginning of buffer
                                            // Next file read will be added to them
        }

        assert ( totalBytesRead == channel.size());
        return totalLines;
    }



   /**
    * This is where each thread gets it's own byte[] chunk to analyze:
    * - it starts at index "startIndex"
    * - it ends   at index "endIndex" - 1
    * - it contains a number of complete rows (no half rows)
    */
    private static int processLinesPerThread( byte[] array, int startIndex, int endIndex, Stat stat )
    {
        assert (( startIndex >=0 ) &&
                    ( endIndex <= array.length ) &&
                        ( startIndex < endIndex ));

        int linesCounter   = 0;
        int lastStartIndex = 0;

        for ( int index = startIndex; index < endIndex; index++ ) // "index" is incremented manually - Range doesn't fit here
        {
            if ( endOfLine( array[ index ] ))
            {
                analyze( array, lastStartIndex, stat );

                linesCounter++;

                while(( index < endIndex ) && endOfLine( array[ index ] )){ index++ } // Skipping "end of line" sequence
                assert ( endOfLine( array[ index - 1 ] ) && (( index == endIndex ) || ( ! endOfLine( array[ index ] ))));

                lastStartIndex = index;
            }
        }

        return linesCounter;
    }



   /**
    * Analyzes the line specified (starting at index "offset" in the array specified)
    * according to benchmark needs:
    * - http://wikis.sun.com/display/WideFinder/The+Benchmark
    * - http://groovy.codehaus.org/Regular+Expressions
    */
    private static void analyze ( byte[] array, int offset, Stat stat )
    {
        String  clientAddress = null;
        String  httpMethod    = null;
        String  uri           = null;
        String  statusCode    = null;
        String  byteCount     = null;
        String  referrer      = null;

        int     start         = offset;
        int     end           = start;
        boolean stop          = false;

        for( int tokenCounter = 0; ( ! stop  ); end++ )
        {
            if ( array[ end ] == SPACE )
            {
                switch ( tokenCounter++ )
                {
                    case 0  : clientAddress = string( array, start, end );
                              break;
                    case 5  : httpMethod    = string( array, start + 1, end ); // Getting rid of starting '"'
                              break;
                    case 6  : uri           = string( array, start, end );
                              break;
                    case 8  : statusCode    = string( array, start, end );
                              break;
                    case 9  : byteCount     = string( array, start, end );
                              break;
                    case 10 : referrer      = string( array, start + 1, end - 1 ); // Getting rid of wrapping '"'
                              stop          = true; // We're done with this line!
                              break;
                }

                while ( array[ end ] == SPACE ){ end++ }
                start = end;
            }
        }

        assert ( clientAddress && httpMethod && uri && statusCode && byteCount && ( referrer != null )); // "referrer" may be empty

        boolean isArticle = (( httpMethod == 'GET' ) && ( uri.startsWith( ARTICLE_PREFIX )) && ( uri ==~ ARTICLE_PATTERN ));

        if ( isArticle )
        {
            stat.addArticle( uri,
                             clientAddress,
                             ((( ! referrer.isEmpty()) && ( referrer != '-' )) ? referrer : null ));
        }

        stat.addUri( uri,
                     (( byteCount != '-' ) ? Integer.valueOf( byteCount ) : 0 ),
                     ( statusCode == '404' ));
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
    * Determines if byte specified is an end-of-line character
    */
    private static boolean endOfLine( byte b )
    {
        ( b == BN );
    }
}