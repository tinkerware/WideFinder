package demo

import java.util.concurrent.Executor
import java.util.concurrent.Executors


@Typed
class ExecutorDemo
{
    static class MyThread extends Thread
    {
        MyThread ( target )
        {
            super( target );
            println "MyThread created"
        }

        String toString ()
        {
            return "MyThread";
        }


    }


    static void main( String ... args )
    {
        Executor pool = Executors.newFixedThreadPool( 3, [ newThread : { Runnable r -> new MyThread( r ) } ] );

        [ 1, 2, 3 ].iterator().each( pool )
        {
            println Thread.currentThread();
            println Thread.currentThread() instanceof MyThread;
        }
    }
}
