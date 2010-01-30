@Typed
package widefinder

/**
 * {@link Map} implementation avoiding any rehashes
 *
 * @see widefinder.NoRehashMapTest
 */
public class NoRehashMap<K, V> implements Map<K, V>
{

    final int             capacity
    final int             threshold
    final float           loadFactor
    final List<Map<K, V>> maps


   /**
    *
    * <a href="http://java.sun.com/javase/6/docs/api/java/util/HashMap.html">
    *   If the initial capacity is greater than the maximum number of entries divided by the load factor, no rehash operations will ever occur
    * </a>
    */
    public NoRehashMap ( int capacity, float loadFactor = 0.85f )
    {
        this.capacity   = capacity                         // Initial Map capacity
        this.threshold  = (( capacity * loadFactor ) - 1 ) // Maximal Map size it's allowed to reach
        this.loadFactor = loadFactor                       // Load factor
        this.maps       = new ArrayList<Map<K, V>>([ newMap() ]);
    }


    private Map<K, V> newMap()
    {
        new HashMap<K,V>( getCapacity(), getLoadFactor())
    }


    private Map<K, V> findMap ( Object key )
    {
        return getMaps().find { it.containsKey( key ) }
    }


   /**
    * Returns the number of internal Maps created
    */
    int mapsNumber()
    {
        getMaps().size()
    }


    @Override
    public int size ()
    {
        ( int ) getMaps()*.size().inject( 0 ){ int prev, int current -> ( prev + current ) }
    }


    @Override
    public boolean isEmpty ()
    {
        (( getMaps().isEmpty()) || ( ! getMaps().any()))
    }


    @Override
    public boolean containsKey ( Object key )
    {
        getMaps().any { Map m -> m.containsKey( key ) }
    }


    @Override
    public boolean containsValue ( Object value )
    {
        getMaps().any { Map m -> m.containsValue( value ) }
    }


    @Override
    public void clear ()
    {
        getMaps().clear()
    }


    @Override
    public V get ( Object key )
    {
        findMap( key ).get( key )
    }


    @Override
    public V put ( K key, V value )
    {
        Map map = findMap( key )

        if ( map )
        {
            return map.put( key, value )
        }

        map = getMaps().last()
        map.put( key, value )

        if ( map.size() == getThreshold())
        {
            getMaps() << newMap()
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
    public Set<K> keySet ()
    {
        getMaps()*.keySet().inject( new HashSet<K>()){ Set result, Set keySet -> result << keySet }
    }


    @Override
    public Collection<V> values ()
    {
        getMaps()*.values().inject( new ArrayList<V>()){ Collection result, Collection values -> result << values }
    }


    @Override
    public Set<Map.Entry<K, V>> entrySet ()
    {
        getMaps()*.entrySet().inject( new HashSet()){ Set result, Set entrySet -> result << entrySet }
    }
}
