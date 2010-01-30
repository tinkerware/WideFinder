@Typed
package widefinder


/**
 * Statistics aggregator utils
 */
final class StatUtils
{

    private StatUtils ()
    {
    }



   /**
    * Summarizes counter values for the maps specified and finds "top N" resulting counters
    *
    * Returns a Map<String, Long> (of size <= n) iterating on which one can see all String values
    * (from the maps passed) and their corresponding counters, from biggest counter to the smaller ones
    */
    static Map<String, Long> sumAndTop( int n, Collection<Map<Long, Collection<String>>> maps )
    {
        Map<String, L> sumMap = new HashMap<String, L>();

        maps.each
        {
            Map<Long, Collection<String>> map ->
            map.each
            {
                Long counter, Collection<String> values ->
                values.each
                {
                    String value ->

                    if ( ! sumMap[ value ] ) { sumMap[ value ] = new L() }
                    sumMap[ value ].add( counter );
                }
            }
        }

        return topN( n, sumMap );
    }


    /**
     * Summarizes counter values for the maps specified and finds "top N" resulting counters
     *
     * Returns a Map<String, Long> (of size <= n) iterating on which one can see all String values
     * (from the maps passed) and their corresponding counters, from biggest counter to the smaller ones
     */
    static Map<String, Long> sumAndTop2( int n, Collection<Map<String, L>> maps )
    {
        Map<String, L> sumMap = new HashMap<String, L>();

        maps.each
        {
            Map<String, L> map ->
            map.each
            {
                String value, L counter ->
                if ( ! sumMap[ value ] ){ sumMap[ value ] = new L() }
                sumMap[ value ].add( counter.counter());
            }
        }

        return topN( n, sumMap );
    }


    /**
     * Retrieves [String value => counter] Map corresponding to the "top N" counters
     * in "maps" specified.
     */
    private static Map<String, Long> topN ( int n, Map<String, L> ... maps )
    {
        Map<String, Long>             resultMap      = new LinkedHashMap<String, Long>( n );
        Map<Long, Collection<String>> topCountersMap = topCountersMap( n, maps );

       /**
        * Iterating over all counters sorted in decreasing order (from top to bottom)
        * and filling the result map (no more than n entries)
        */
        topCountersMap.keySet().sort{ long a, long b -> ( b <=> a ) }.each
        {
            long topCounter ->

            /**
             * Iterating over each String corresponding to "top counter"
             */
            topCountersMap[ topCounter ].each
            {
                String s ->
                if ( resultMap.size() < n ) { resultMap[ s ] = topCounter }
            }
        }

        return resultMap;
    }


    /**
    * Creates a small "top counters" Map (of size <= n) from a bigger "key => counter" maps:
    *
    * - Key (Long)                 - top n counters found in maps specified
    * - Value (Collection<String>) - original map's keys that were mapped to that key (counter),
    *                                no more than n
    *
    * "Top n counter" means that a counter is in "top n" elements if all original counters
    * (values of the map specified) were sorted but we use no sorting here since it's not needed.
    */
    static Map<Long, Collection<String>> topCountersMap ( int n, Map<String, L> ... maps )
    {
        Map<Long, Collection<String>> topCountersMap = new HashMap<Long, Collection<String>>( n );
        long[]                        minCounter     = [ Long.MAX_VALUE ]; // Currently known minimal counter

        maps.each
        {
            Map<String, L> map ->

            map.each
            {
                String key, L l ->

                long counter = l.counter();

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

        return topCountersMap;
    }
}
