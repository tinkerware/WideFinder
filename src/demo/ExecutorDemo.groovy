package demo

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

@Typed
class ExecutorDemo
{

    static void main( String ... args )
    {
        int             n    = Runtime.getRuntime().availableProcessors();
        ExecutorService pool = Executors.newFixedThreadPool( n );

        Future f = ( 1 .. n ).iterator().each( pool )
        {
            println "[${ new Date()}]: [$it]: [${ Thread.currentThread() }] started";
            long t = System.currentTimeMillis();
            calc();
            println "[${ new Date()}]: [$it]: [${ Thread.currentThread() }] finished - (${ System.currentTimeMillis() - t } ms)";
        };

        while( ! f.isDone()){ sleep( 500 ) }

        println "[${ new Date()}]: all threads finished"
        pool.shutdown();
    }


    static void calc ()
    {
        Random rand = new Random( new Random( System.currentTimeMillis()).nextLong());

        10.times
        {
            double j = 1.0D;
            for ( i in ( 1 .. ( rand.nextInt( 100000000 )))){ j *= i }
            for ( i in ( 1 .. ( rand.nextInt( 100000000 )))){ j -= i }
            for ( i in ( 1 .. ( rand.nextInt( 100000000 )))){ j += i }
            for ( i in ( 1 .. ( rand.nextInt( 100000000 )))){ j /= i }
        }
    }
}
