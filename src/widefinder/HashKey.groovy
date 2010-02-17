
@Typed
package widefinder

/**
 * Map key wrapping <code>int</code> and <code>long</code>
 */
class HashKey
{
    private final int  i
    private final long l

    HashKey ( int i, long l )
    {
        this.i = i;
        this.l = l;
    }

    @Override
    public int hashCode () { ( this.i * 31 ) + this.l }

    @Override
    public boolean equals ( Object o )
    {
        if ( this.is( o )) { return true }

        HashKey otherKey = (( HashKey ) o )
        ( otherKey.@i == this.@i ) && ( otherKey.@l == this.@l )
    }

    @Override
    String toString () { "[${ this.i }][${ this.l }]" }
}
