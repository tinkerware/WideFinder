package widefinder;

/**
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * {@link Map} implementation avoiding any rehashes
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */
public class NoRehashMap<K, V> implements Map<K, V>
{

    private final int capacity;
    private final def maps = new ArrayList<Map<K, V>>()

    private int            capacity(){ this.@capacity }
    private List<Map<K,V>> maps()    { this.@maps     }


    public NoRehashMap ( int capacity )
    {
        this.capacity = capacity
    }


    /**
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * Trivial implementations
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */


    @Override
    public int size ()
    {
        ( int ) maps()*.size().inject( 0 ){ int prev, int current -> ( prev + current ) }
    }


    @Override
    public boolean isEmpty ()
    {
        (( maps().isEmpty()) || ( ! maps().any()))
    }


    @Override
    public boolean containsKey ( Object key )
    {
        maps().any { Map m -> m.containsKey( key ) }
    }


    @Override
    public boolean containsValue ( Object value )
    {
        maps().any { Map m -> m.containsValue( value ) }
    }


    @Override
    public void clear ()
    {
        maps().clear()
    }


    /**
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * Now comes the tricky part ...
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    @Override
    public V get ( Object key )
    {
        return null;
    }


    @Override
    public V put ( K key, V value )
    {
        return null;
    }


    @Override
    public V remove ( Object key )
    {
        return null;
    }


    @Override
    public void putAll ( Map<? extends K, ? extends V> m )
    {
    }




    @Override
    public Set<K> keySet ()
    {
        return null;
    }


    @Override
    public Collection<V> values ()
    {
        return null;
    }


    @Override
    public Set<Map.Entry<K, V>> entrySet ()
    {
        return null;
    }
}
