package widefinder


/**
 * Statistics aggregator utils
 */
@Typed
final class StatUtils
{

    private StatUtils ()
    {
    }


    /**
     * Retrieves [values => counter] Map corresponding to the "top N" counters
     * in "maps" specified.
     */
    static Map<String, Long> top ( int n, Map<String, L> ... maps )
    {
        assert (( n > 0 ) && ( maps != null ));

        Map<String, Long>             resultMap      = new LinkedHashMap<String, Long>( n );
        Map<Long, Collection<String>> topCountersMap = topCountersMap( n, maps );

       /**
        * Iterating over all counters sorted in decreasing order (from top to bottom)
        * and filling the result map (no more than n entries)
        */
        topCountersMap.keySet().sort{ long a, long b -> ( b - a ) }.each
        {
            long topCounter ->

            /**
             * Iterating over each String corresponding to "top counter"
             */
            topCountersMap[ topCounter ].each
            {
                if ( resultMap.size() < n ) { resultMap.put( it, topCounter ) }
            }
        }

        assert ( resultMap.size() <= n );
        return   resultMap;
    }


    /**
     * Retrieves [values => counter] Map corresponding to the "top N" counters in the
     * "countersMap" specified.
     */
    static Map<String, Long> top ( int n, Map<String, Long> topArticles, Map<String, Map<String, L>> countersMap )
    {
        assert ( topArticles.size() <= n );

        /**
         * Collection of maps (key => counter) corresponding to top articles
         */
        List<Map<String, L>> maps = new ArrayList<Map<String,L>>( n );

        topArticles.keySet().each
        {
            String topArticle ->

            if ( countersMap[ topArticle ] != null ) { maps << countersMap[ topArticle ] }
        }

        return top( n, maps.toArray( new Map<String, L>[ maps.size() ] ));
    }



    /**
    * Creates a small "top counters" Map (of size n) from a BIG "key => counter" maps:
    *
    * - Key (Long)                 - top n counters found in the map specified
    * - Value (Collection<String>) - original map's keys that were mapped to that key (counter).
    *                                (no more than n)
    *
    * "Top n counter" means that a counter is in "top n" elements if all original counters
    * (values of the map specified) were sorted but we use no sorting here since it's not needed.
    */
    private static Map<Long, Collection<String>> topCountersMap ( int n, Map<String, L> ... maps )
    {
        assert (( n > 0 ) && ( maps != null ));

        Map<Long, Collection<String>> topCountersMap = new HashMap<Long, Collection<String>>( n );
        long[]                        minCounter     = [ Long.MAX_VALUE ]; // Currently known minimal counter

        maps.each
        {
            Map<String, L> map ->

            map.each
            {
                String key, L l ->

                long     counter = l.counter();
                assert ( counter > 0 );

                if (( topCountersMap.size() == n ) && ( counter > minCounter[ 0 ] ) && ( ! topCountersMap[ counter ] ))
                {
                    topCountersMap.remove( minCounter[ 0 ] );
                }

                if (( topCountersMap.size() < n ) && ( ! topCountersMap[ counter ] ))
                {
                    topCountersMap[ counter ] = new ArrayList<String>( n );
                    minCounter[ 0 ]           = counter;
                    topCountersMap.keySet().each{ minCounter[ 0 ] = (( it < minCounter[ 0 ] ) ? it : minCounter[ 0 ] ) }
                }

                if ( topCountersMap.containsKey( counter ) && ( topCountersMap[ counter ].size() < n ))
                {
                    topCountersMap[ counter ] << key;
                }
            }
        }

        assert ( topCountersMap.size() <= n );
        return   topCountersMap;
    }
}
