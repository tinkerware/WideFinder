package demo

import java.util.concurrent.Callable
import widefinder.Stat

class C
{

    public static void main ( String ... args )
    {
        List<Callable> callables = [];

        ( 1 .. 100 ).each
        {
            callables << [ call : { doNothing() }]
        }
    }


    static void doNothing()
    {

    }

}
