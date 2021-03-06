@Typed
package widefinder.maps

/**
 * {@link Map} implementation avoiding any rehashes
 *
 * @see widefinder.maps.NoRehashMapTest
 */
public class NoRehashMap<K, V> implements Map<K, V>
{
    final int             capacity
    final float           loadFactor
    final int             threshold
    final List<Map<K, V>> maps
          Map<K, V>       tailMap


   /**
    * Creates a new Map instance.
    *
    * @param capacity   initial capacity for each internal Map, same as in {@link HashMap} constructor
    * @param loadFactor internal Map load factor, same as in {@link HashMap} constructor
    * @param concurrent whether this instance is supposed to operate in concurrent environment
    *
    * Actual size of each internal Map (there will be N of them - depending on their capacities and total number
    * of elements added to <code>NoRehashMap</code>) will be no more than {@code threshold  = (( capacity * loadFactor ) - 1 ) }
    * to prevent them from rehashing.
    *
    * As <code>HashMap</code> documentation says:
    * <a href="http://java.sun.com/javase/6/docs/api/java/util/HashMap.html">
    *   If the initial capacity is greater than the maximum number of entries divided by the load factor, no rehash operations will ever occur
    * </a>
    */
    public NoRehashMap ( int capacity, float loadFactor = 0.85f )
    {
        assert ( loadFactor > 0.0f ) && ( loadFactor < 1.0f )

        this.@capacity   = capacity                         // Initial Map capacity
        this.@threshold  = (( capacity * loadFactor ) - 1 ) // Maximal Map size it's allowed to reach
        this.@loadFactor = loadFactor                       // Load factor
        this.@tailMap    = newMap()
        this.@maps       = new ArrayList<Map<K, V>>([ getTailMap() ])
    }


    protected Map<K, V> newMap()
    {
        new HashMap<K, V>( getCapacity(), getLoadFactor())
    }



   /**
    * Finds a Map containing the key specified
    * @param key key to use
    * @return Map containing the key specified or <code>null</code> if not found
    */
    private Map<K, V> findMap ( Object key ) { getMaps().find { it.containsKey( key ) }}


   /**
    * Returns the number of internal Maps created
    */
    int mapsNumber()
    {
        getMaps().size()
    }


    @Override
    public boolean isEmpty ()
    {
        ( ! getMaps().any())
    }


    @Override
    public boolean containsKey ( Object key )
    {
        ( findMap( key ) != null )
    }


    @Override
    public boolean containsValue ( Object value )
    {
        getMaps().any { Map m -> m.containsValue( value ) }
    }


    @Override
    public void clear ()
    {
        setTailMap( newMap())
        getMaps().clear()
        getMaps() << getTailMap()
    }


    @Override
    public V get ( Object key )
    {
        findMap( key )?.get( key )
    }


    @Override
    public V put ( K key, V value )
    {
        Map map = findMap( key )

        if ( map )
        {
            return map.put( key, value ) // Replacing existing value
        }

        map = getTailMap()
        map.put( key, value ) // Adding new value

        synchronized ( map )
        {
            if ( map.size() == getThreshold())
            {
                /**
                 * Last map is full - creating a new one
                 */
                setTailMap( newMap())
                getMaps() << getTailMap()
            }
        }

        return null
    }


    @Override
    public V remove ( Object key )
    {
        findMap( key ).remove( key )
    }


    @Override
    public void putAll ( Map<? extends K, ? extends V> m )
    {
        m.each { key, value -> put( key, value ) }
    }


    @Override
    public int size ()
    {
        reduce({ Map m -> m.size() },
               0,
               { int result, int size -> ( result + size ) })
    }


    @Override
    public Set<K> keySet ()
    {
        reduce({ Map m -> m.keySet() },
               new HashSet<K>(),
               { Set result, Set keySet -> result.addAll( keySet ); result })
    }


    @Override
    public Collection<V> values ()
    {
        reduce({ Map m -> m.values() },
               new ArrayList<V>(),
               { Collection result, Collection values -> result.addAll( values ); result })
    }


    @Override
    public Set<Map.Entry<K, V>> entrySet ()
    {
        reduce({ Map m -> m.entrySet() },
               new HashSet(),
               { Set result, Set entrySet -> result.addAll( entrySet ); result })
    }


   /**
    * "Reduces" an operation invoked on all internal Maps to a single result
    *
    * @param methodClosure    method to invoke on all internal maps,
    *                         passed a single Map as a parameter
    * @param initialValue     initial value to pass to {@code inject()}
    * @param operationClosure second parameter to pass to {@code inject()},
    *                         passed two arguments: - aggregated results (started from <code>initialValue</code>)
    *                                               - current iteration value (as returned by <code>methodClosure</code>)
    *
    * See <a href="http://groovy.codehaus.org/groovy-jdk/java/lang/Object.html#inject(java.lang.Object value, groovy.lang.Closure closure)">
    *       inject()
    *     </a>
    */
    private <T> T reduce( Closure methodClosure, T initialValue, Closure operationClosure )
    {
        ( T ) getMaps().collect
                        {
                            Map m -> methodClosure( m )
                        }.
                        inject( initialValue )
                        {
                            def prevResult, def currentValue -> operationClosure( prevResult, currentValue )
                        }
    }
}
