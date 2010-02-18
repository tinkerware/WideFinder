@Typed
package widefinder.maps


import java.util.concurrent.locks.*

/**
 *
 */
class StrictConcurrentNoRehashMap<K, V> extends ConcurrentNoRehashMap<K, V>
{
    private final ReadWriteLock lock = new ReentrantReadWriteLock()
    private       Lock getReadLock() { this.@lock.readLock()  }
    private       Lock getWriteLock(){ this.@lock.writeLock() }


    public StrictConcurrentNoRehashMap ( int capacity, float loadFactor = 0.85f )
    {
        super( capacity, loadFactor )
    }



    private <T> T readLock( Closure c )
    {
        getReadLock().lock()

        try
        {
            return c()
        }
        finally
        {
            getReadLock().unlock()
        }
    }


    private <T> T writeLock( Closure c )
    {
        getWriteLock().lock()

        try
        {
            return c()
        }
        finally
        {
            getWriteLock().unlock()
        }
    }


    @Override
    public V get ( Object key )
    {
        return readLock {
            return null;//super.get( key )
        }
    }


    @Override
    public V put ( K key, V value )
    {
        return writeLock {
            return null;//super.put( key, value )
        };
    }

}
