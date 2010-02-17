
@Typed
package widefinder

/**
 * Map key wrapping <code>int</code> and <code>long</code>
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
    int hashCode () { this.hashcode }

    @Override
    boolean equals ( Object o ) { ((( HashKey ) o ).hashcode == this.hashcode ) && ((( HashKey ) o ).checksum == this.checksum ) }

    @Override
    String toString () { "[${ this.hashcode }][${ this.checksum }]" }
}
