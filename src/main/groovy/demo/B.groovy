package demo


@Typed
class B
{
    public static void main ( String ... args )
    {
        long[] j = [ 0 ];

        long t = System.currentTimeMillis();
        new File( "e:/Projects/groovy-booster/data/O.Big.log" ).eachLine{ j[ 0 ]++ }
        println "[${ j[ 0 ] }] lines, [${ System.currentTimeMillis() - t }] ms"
    }

}
