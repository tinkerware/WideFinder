@Typed
package widefinder.maps


import java.util.concurrent.*


/**
 */
class ConcurrentNoRehashMap<K, V> extends NoRehashMap<K, V>
{

    public ConcurrentNoRehashMap ( int capacity, float loadFactor = 0.85f )
    {
        super( capacity, loadFactor )
    }


    protected Map<K, V> newMap ()
    {
        new ConcurrentHashMap<K, V>( getCapacity(), getLoadFactor(), Runtime.getRuntime().availableProcessors())
    }

}
