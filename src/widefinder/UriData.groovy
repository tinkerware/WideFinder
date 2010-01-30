@Typed
package widefinder


/**
 * Container to hold all URI-related data needed by statistics gathering
 */
class UriData
{
           boolean        article   = false
     final L              accessCount = new L()
     final L              byteCount   = new L()
     final L              is404Count  = new L()
           Map<String, L> clients     = null
           Map<String, L> referrers   = null


    void update( boolean article,
                 int     byteCount,
                 boolean is404,
                 String  clientAddress,
                 String  referrer )
    {
        getByteCount().add( byteCount )
        if ( is404 ) { getIs404Count().increment() }

        if ( article )
        {
            setArticle( true )
            getAccessCount().increment()

            /**
             * Update clients Map
             */

            if ( ! getClients()) { setClients( new NoRehashMap<String, L>( 128 )) }
            L counter = getClients()[ clientAddress ];
            if ( counter == null ) { counter = ( getClients()[ clientAddress ] = new L()) }
            counter.increment()

            /**
             * Update referrers map (if "referrer" isn't null)
             */

            if ( referrer )
            {
                if ( ! getReferrers()) { setReferrers( new NoRehashMap<String, L>( 128 )) }
                counter = getReferrers()[ referrer ];
                if ( counter == null ) { counter = ( getReferrers()[ referrer ] = new L()) }
                counter.increment()
            }
        }
    }
}
