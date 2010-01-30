@Typed
package widefinder

import org.junit.Test


/**
 * {@link NoRehashMap} unit tests
 */
class NoRehashMapTest extends GroovyTestCase
{

    @Test
    void testMapsNumber()
    {
        NoRehashMap m = new NoRehashMap<Integer, Integer>( 1000, 0.95f )
        1.upto( 100000 ){ j -> m.put( j, j ) }
        assertEquals( 106, m.mapsNumber()) // ( 100000 / 948 ) + 1


        m = new NoRehashMap<Integer, Integer>( 10000, 0.65f )
        1.upto( 10000 ){ j -> m.put( j, j ) }
        assertEquals( 2, m.mapsNumber()) // ( 10000 / 6499 ) + 1
    }




}
