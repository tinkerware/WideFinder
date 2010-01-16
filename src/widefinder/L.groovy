package widefinder


/**
 * Mutable "long" wrapper
 */
@Typed
class L
{
    private long counter = 0;

    L ()
    {
        this.counter = 0;
    }


    L ( long value )
    {
        this.counter = value;
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
