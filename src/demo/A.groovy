package demo

import groovy.io.FileType

@Typed
class A
{

    public static void main( String ... args )
    {
        new File( "k:/groovy-booster/data" ).withWriter
        {
            writer ->

            new File( "k:/groovy-booster/logs" ).eachFileRecurse( FileType.FILES )
            {
                File file ->

                println "[$file] - start"
                long t   = System.currentTimeMillis();

                file.eachLine{ writer.println( it ) }

                long  ms  = System.currentTimeMillis() - t;
                float mb  = ( file.length() / ( 2 ** 20 ));
                int   mbs = (( 1000 * mb ) / ms );

                println "[$file] - end ([$ms] ms, [$mb] Mb, [$mbs] Mb/sec)"
            }
        }
    }
}
