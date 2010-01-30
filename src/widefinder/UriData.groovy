@Typed
package widefinder


/**
 * Container to hold all URI-related data needed by statistics gathering
 */
class UriData
{
           boolean        isArticle   = false
     final L              accessCount = new L()
     final L              byteCount   = new L()
     final L              is404Count  = new L()
           Map<String, L> clients     = null
           Map<String, L> referrers   = null


    void update( boolean isArticle,
                 int     byteCount,
                 boolean is404,
                 String  clientAddress,
                 String  referrer )
    {
        setIsArticle( isArticle )

        getAccessCount().increment()

        getByteCount().add( byteCount )

        if ( is404 ) { getIs404Count().increment() }

        if ( clientAddress )
        {
            if ( ! getClients()) { setClients( new NoRehashMap<String, L>( 1024 )) }

            L counter = getClients().get( clientAddress );
            if ( counter == null ) { counter = ( getClients()[ clientAddress ] = new L()) }
            counter.increment()
        }

        if ( referrer )
        {
            if ( ! getReferrers()) { setReferrers( new NoRehashMap<String, L>( 1024 )) }

            L counter = getReferrers().get( referrer );
            if ( counter == null ) { counter = ( getReferrers()[ referrer ] = new L()) }
            counter.increment()
        }
    }
}
