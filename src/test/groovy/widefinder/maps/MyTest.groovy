@Typed
package widefinder.maps

import org.junit.Test


class MyTest extends GroovyTestCase
{
    @Test
    void testSize()
    {
        MyMap m = new MyMap<Integer, Integer>( 10000 )
        1.upto( 100000 ){ int j -> m.put( j, j ) }
        assertEquals( 100000, m.size())

        1.upto( 100000 ){ int j -> m.put( j, j ) }
        assertEquals( 100000, m.size())

        m = new MyMap<String, Integer>( 10000 )
        1.upto( 100000 ){ int j -> m.put( "[$j]", j ) }

        assertEquals( 100000, m.size())

        1.upto( 100000 ){ int j -> m.put( "[$j][new]", j ) }
        assertEquals( 200000, m.size())
    }
}



public class MyMap<K, V> implements Map<K, V>
{

    public MyMap ( int capacity )
    {
    }


    @Override
    public boolean isEmpty ()
    {
        true
    }


    @Override
    public boolean containsKey ( Object key )
    {
        false
    }


    @Override
    public boolean containsValue ( Object value )
    {
        false
    }


    @Override
    public void clear ()
    {
    }


    @Override
    public V get ( Object key )
    {
        null
    }


    @Override
    public V put ( K key, V value )
    {
        null
    }


    @Override
    public V remove ( Object key )
    {
        null
    }


    @Override
    public void putAll ( Map<? extends K, ? extends V> m )
    {
    }


    @Override
    public int size ()
    {
        null
    }


    @Override
    public Set<K> keySet ()
    {
        null
    }


    @Override
    public Collection<V> values ()
    {
        null
    }


    @Override
    public Set<Map.Entry<K, V>> entrySet ()
    {
        null
    }
}
