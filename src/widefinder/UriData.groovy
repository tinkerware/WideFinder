@Typed
package widefinder


/**
 * Container to hold URI-related data needed by statistics gathering
 */
class UriData
{
    private final L byteCount  = new L()
    private final L is404Count = new L()

    
    void update( boolean article,
                 int     byteCount,
                 boolean is404,
                 String  clientAddress,
                 String  referrer )
    {
        this.@byteCount.add( byteCount )
        if ( is404 ) { this.@is404Count.increment() }
    }
}
