package demo

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Typed
class ExecutorDemo
{

    static void main( String ... args )
    {
        int             n    = Runtime.getRuntime().availableProcessors();
        ExecutorService pool = Executors.newFixedThreadPool( n );

        ( 1 .. n ).iterator().each( pool )
        {
            println "[${ new Date()}]: [$it]: [${ Thread.currentThread() }] started";
            long t = System.currentTimeMillis();
            long j = calc();
            println "[${ new Date()}]: [$it]: [${ Thread.currentThread() }] finished - [$j] (${ System.currentTimeMillis() - t } ms)";
        }.get();

        println "[${ new Date()}]: all threads finished"
        pool.shutdown();
    }


    static long calc ()
    {
        long   j    = 1;
        Random rand = new Random( new Random( System.currentTimeMillis()).nextLong());

        for ( i in ( 1 .. rand.nextInt( 100000000 ))){ j *= i }
        for ( i in ( 1 .. rand.nextInt( 100000000 ))){ j -= i }
        for ( i in ( 1 .. rand.nextInt( 100000000 ))){ j += i }
        for ( i in ( 1 .. rand.nextInt( 100000000 ))){ j /= i }

        return j;
    }
}
