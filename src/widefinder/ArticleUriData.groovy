@Typed
package widefinder


/**
 * Article-related URI data
 */
class ArticleUriData extends UriData
{
    private final L              accessCount = new L()
    private final Map<String, L> clients     = new HashMap<String,L>()
    private final Map<String, L> referrers   = new HashMap<String,L>()


    @Override
    void update( boolean article,
                 int     byteCount,
                 boolean is404,
                 String  clientAddress,
                 String  referrer )
    {
        super( byteCount, is404 );
        this.@accessCount.increment()

        /**
         * Update clients Map
         */

        L counter = this.@clients[ clientAddress ];
        if ( counter == null ) { counter = ( this.@clients[ clientAddress ] = new L()) }
        counter.increment()

        /**
         * Update referrers map (if "referrer" isn't null)
         */

        if ( referrer )
        {
            counter = this.@referrers[ referrer ];
            if ( counter == null ) { counter = ( this.@referrers[ referrer ] = new L()) }
            counter.increment()
        }
    }

}
