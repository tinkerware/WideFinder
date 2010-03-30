@Typed
package widefinder


/**
 * Container to hold URI-related data needed by statistics gathering
 */
class UriData
{
    protected final L byteCount  = new L()
    protected final L is404Count = new L()


    void update( int     byteCount,
                 boolean is404 )
    {
        this.@byteCount.add( byteCount )
        if ( is404 ) { this.@is404Count.increment() }
    }
}
