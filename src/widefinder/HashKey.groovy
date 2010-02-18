
@Typed
package widefinder


/**
 * Map key wrapping <code>int hashcode</code> and <code>long checksum</code>
 */
class HashKey
{
    private final int  hashcode
    private final long checksum

    HashKey ( int hashcode, long checksum )
    {
        this.hashcode = hashcode;
        this.checksum = checksum;
    }

    @Override
    public int hashCode () { ( this.hashcode * 31 ) + this.checksum }

    @Override
    public boolean equals ( Object o )
    {
        if ( this.is( o )) { return true }

        HashKey otherKey = (( HashKey ) o )
        ( otherKey.@hashcode == this.@hashcode ) && ( otherKey.@checksum == this.@checksum )
    }

    @Override
    String toString () { "[${ this.hashcode }][${ this.checksum }]" }
}
