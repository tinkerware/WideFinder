@Typed
package widefinder.maps

import org.junit.Test
import widefinder.maps.NoRehashMap

/**
 * {@link NoRehashMap} unit tests
 */
class NoRehashMapTest extends GroovyTestCase
{

    @Test
    void testMapsNumber()
    {
        assertEquals( 1, new NoRehashMap( 1 ).mapsNumber())

        NoRehashMap m = new NoRehashMap<Integer, Integer>( 1000, 0.95f )
        1.upto( 100000 ){ int j -> m.put( j, j ) }
        assertEquals( 106, m.mapsNumber()) // ( 100000 / 948 ) + 1


        m = new NoRehashMap<Integer, Integer>( 10000, 0.65f )
        1.upto( 10000 ){ int j -> m.put( j, j ) }
        assertEquals( 2, m.mapsNumber()) // ( 10000 / 6499 ) + 1


        m = new NoRehashMap<Integer, Integer>( 1000, 0.10f )
        1.upto( 98 ){ int j -> m.put( j, j ) }
        assertEquals( 1, m.mapsNumber())
    }


    @Test
    void testSize()
    {
        NoRehashMap m = new NoRehashMap<Integer, Integer>( 10000, 0.95f )
        1.upto( 100000 ){ int j -> m.put( j, j ) }
        assertEquals( 100000, m.size())

        1.upto( 100000 ){ int j -> m.put( j, j ) }
        assertEquals( 100000, m.size())

        m = new NoRehashMap<String, Integer>( 10000 )
        1.upto( 100000 ){ int j -> m.put( "[$j]", j ) }

        assertEquals( 100000, m.size())

        1.upto( 100000 ){ int j -> m.put( "[$j][new]", j ) }
        assertEquals( 200000, m.size())
    }


    @Test
    void testKeySet()
    {
        Map m    = new NoRehashMap<String, Integer>( 45000 )
        1.upto( 100000 ){ int j -> m.put( "$j", j ) }
        def keys = m.keySet()
        assertEquals( 100000, keys.size())
        100000.downto( 1 ){ int j -> assertTrue ( keys.contains( "$j" )) }
    }


    @Test
    void testValues()
    {
        Map m = new NoRehashMap<String, String>( 450 )
        1.upto( 1000 ){ int j -> m.put( "$j", "$j" ) }
        def values = m.values()
        assertEquals( 1000, values.size())
        1000.downto( 1 ){ int j -> assertTrue ( values.contains( "$j" )) }
    }


    @Test
    void testEmptyAndSize()
    {
        assertTrue( new NoRehashMap( 100, 0.5f ).isEmpty())

        Map m = new NoRehashMap<String, String>( 450 )
        assertTrue( m.isEmpty())
        assertEquals( 0, m.size())

        m.put( "a", "b" );
        m.put( "c", "d" );

        assertEquals( 2, m.size())
        assertFalse( m.isEmpty())

        m.remove( "a" )

        assertEquals( 1, m.size())
        assertFalse( m.isEmpty())

        m.remove( "c" )
        assertEquals( 0, m.size())
        assertTrue( m.isEmpty())
    }


    @Test
    void testContainsKeyAndRemove()
    {
        assertFalse( new NoRehashMap( 100, 0.5f ).containsKey( "anything" ))

        Map m = new NoRehashMap<String, String>( 450 )
        m.put( "a", "b" )
        m.put( "c", "d" )
        m.put( "e", "f" )

        assertTrue( m.containsKey( "a" ))
        assertTrue( m.containsKey( "c" ))
        assertTrue( m.containsKey( "e" ))

        assertEquals( 3, m.size())

        assertFalse( m.containsKey( "b" ))
        assertFalse( m.containsKey( "d" ))
        assertFalse( m.containsKey( "f" ))

        m.remove( "a" )
        m.remove( "c" )
        m.remove( "e" )

        assertFalse( m.containsKey( "a" ))
        assertFalse( m.containsKey( "c" ))
        assertFalse( m.containsKey( "e" ))

        assertEquals( 0, m.size())
        assertTrue( m.isEmpty())

        1.upto( 10000 ){ int j -> m.put( "[$j]", "<$j>" ) }
        assertEquals( 10000, m.size())

        10000.downto( 1 ){ int j -> assertTrue( m.containsKey( "[$j]" ));
                                    assertFalse( m.containsKey( "<$j>" ))}

    }


    @Test
    void testContainsValueAndRemove()
    {
        assertFalse( new NoRehashMap( 100, 0.5f ).containsValue( "anything" ))

        Map m = new NoRehashMap<String, String>( 123, 0.33f )
        m.put( "a", "b" )
        m.put( "c", "d" )
        m.put( "e", "f" )

        assertTrue( m.containsValue( "b" ))
        assertTrue( m.containsValue( "d" ))
        assertTrue( m.containsValue( "f" ))

        assertEquals( 3, m.size())

        assertFalse( m.containsValue( "a" ))
        assertFalse( m.containsValue( "c" ))
        assertFalse( m.containsValue( "e" ))

        m.remove( "a" )
        m.remove( "c" )
        m.remove( "e" )

        assertFalse( m.containsValue( "b" ))
        assertFalse( m.containsValue( "d" ))
        assertFalse( m.containsValue( "f" ))

        assertEquals( 0, m.size())
        assertTrue( m.isEmpty())

        1.upto( 1000 ){ int j -> m.put( "[$j]", "<$j>" ) }
        assertEquals( 1000, m.size())

        1000.downto( 1 ){ int j -> assertTrue( m.containsValue( "<$j>" ));
                                   assertFalse( m.containsValue( "[$j]" ))}
    }


    @Test
    void testGetPutClear()
    {
        NoRehashMap m = new NoRehashMap<String, String>( 1234 )

        m.put( "a", "b" )
        assertEquals( "b", m[ "a" ] )
        assertEquals( "b", m.get( "a" ))

        m.put( "c", "d" )
        assertEquals( "d", m[ "c" ] )
        assertEquals( "d", m.get( "c" ))

        m.put( "e", "f" )
        assertEquals( "f", m[ "e" ] )
        assertEquals( "f", m.get( "e" ))

        m.clear()
        assertEquals( 0, m.size())
        assertEquals( 1, m.mapsNumber()) // There's always one internal Map, may be empty
        assertTrue( m.isEmpty())

        1.upto( 10000 ){ int j -> m.put( "[$j]", "<$j>" ) }
        assertEquals( 10000, m.size())

        10000.downto( 1 ){ int j -> assertEquals( "<$j>", m[ "[$j]" ] );
                                    assertEquals( "<$j>", m.get( "[$j]" )) }
        m.clear()
        assertEquals( 0, m.size())
        assertEquals( 1, m.mapsNumber()) // There's always one internal Map, may be empty
        assertTrue( m.isEmpty())
    }


    @Test
    void testPutAll()
    {
        Map m1 = new NoRehashMap<String, String>( 123 )
        1.upto( 1000 ){ int j -> m1.put( "[$j]", "<$j>" ) }
        assertEquals( 1000, m1.size())

        Map m2 = new NoRehashMap<String, String>( 234 )
        m2.putAll( m1 )

        assertEquals( m1.size(), m2.size())
        assertEquals( 1000,      m2.size())

        1000.downto( 1 ){ int j -> assertTrue( m2.containsKey( "[$j]" ));
                                   assertTrue( m2.containsValue( "<$j>" ));
                                   assertEquals( "<$j>", m2[ "[$j]" ] );
                                   assertEquals( "<$j>", m2.get( "[$j]" ))}

        m2.clear()
        1.upto( 1000 ){ int j -> m2.put( "[$j]", "<$j>" ) }
        m2.putAll( m1 )

        assertEquals( m1.size(), m2.size())
        assertEquals( 1000,      m2.size())

        1000.downto( 1 ){ int j -> assertTrue( m2.containsKey( "[$j]" ));
                                   assertTrue( m2.containsValue( "<$j>" ));
                                   assertEquals( "<$j>", m2[ "[$j]" ] );
                                   assertEquals( "<$j>", m2.get( "[$j]" ))}
    }
}
