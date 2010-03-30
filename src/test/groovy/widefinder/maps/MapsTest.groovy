
package widefinder.maps

import org.junit.Test


class MapsTest extends GroovyTestCase
{

    @Test
    void test1()
    {
        Map m = new HashMap<String, String>( 1234 )

        1.upto( 10000 ){ int j -> m[ "[$j]" ] = "<$j>" }

        10000.downto( 1 ){ int j -> assertEquals( "<$j>", m[ "[$j]" ]    ) }
        10000.downto( 1 ){ int j -> assertEquals( "<$j>", m.get( "[$j]" )) }
    }


    @Test
    void test2()
    {
        Map m = new HashMap<String, String>( 1234 )

        1.upto( 10000 ){ int j -> m[ "[$j]" ] = "<$j>" }

        10000.downto( 1 ){ int j -> assertEquals( "<$j>", m[ "[$j]" ]              ) }
        10000.downto( 1 ){ int j -> assertEquals( "<$j>", m.get( "[$j]".toString())) }
    }
}
