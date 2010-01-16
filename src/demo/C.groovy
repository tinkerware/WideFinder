package demo

import java.util.concurrent.Callable
import widefinder.Stat


//@Typed
class C
{

    public static void main ( String ... args )
    {
        int[] j = [ 0 ];
        List<String> uris = [   '/ongoing/When/200x/2007/06/17/Web3S',
                                '/ongoing/When/200x/2007/06/17/Tokyo-Playing',
                                '/ongoing/When/200x/2003/09/18/NXML',
                                '/ongoing/When/200x/2007/06/16/X-Me-is-a-Facebook-Virus',
                                '/ongoing/When/200x/2007/06/17/Fathers-Day',
                                '/ongoing/When/200x/2007/06/16/Tokyo-Phone',
                                '/ongoing/When/200x/2007/06/15/Cameras',
                                '/ongoing/When/200x/2004/02/20/GenxStatus',
                                '/ongoing/When/200x/2007/06/14/RFC4287',
                                '/ongoing/When/200x/2006/03/30/Teacup' ];

        new File( "e:/Projects/groovy-booster/data/O.Big.log" ).eachLine
        {
            String line ->

            if ( line.startsWith( 'localhost' ))
            {
                String  uri = line.find( /"GET (.+) HTTP/ ){ it[ 1 ] }
                if ( uris.contains( uri ))
                {
                    j[ 0 ]++;
                }
            }
        }

        println j[ 0 ];
    }


}
