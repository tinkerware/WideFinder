@Typed
package widefinder


/**
 * Mutable "long" wrapper
 */
class L
{
    private long counter = 0;

    L ()
    {
    }


    void add ( long l )
    {
        this.counter += l;
    }


    void increment ()
    {
        this.counter++;
    }


    long counter ()
    {
        return this.counter;
    }


    @Override
    String toString ()
    {
        return String.valueOf( counter());
    }


   /**
    * If L is ever used as map key .. Not today.
    */


    @Override
    int hashCode ()
    {
        return counter().hashCode();
    }


    @Override
    boolean equals ( Object obj )
    {
        return (( this.is( obj )) ||
                (( obj instanceof L ) && ( this.counter == (( L ) obj ).counter )));
    }
}
