
package widefinder.maps

import org.junit.Test


class MapsTest extends GroovyTestCase
{

    @Test
    void test1()
    {
        def m = [:]

        1.upto( 10 ){ int j -> m[ "[$j]" ] = "<$j>" }
        1.upto( 10 ){ int j -> assertEquals( "<$j>", m[ "[$j]" ]    ) }
        1.upto( 10 ){ int j -> assertEquals( "<$j>", m.get( "[$j]" )) } // Fails, returns null
    }


    @Test
    void test1Fixed()
    {
        def m = [:]

        1.upto( 10 ){ int j -> m[ "[$j]" ] = "<$j>" }
        1.upto( 10 ){ int j -> assertEquals( "<$j>", m[ "[$j]" ]    ) }
        1.upto( 10 ){ int j -> assertEquals( "<$j>", m.get( "[$j]".toString())) } // Fixed
    }


    @Test
    void test2()
    {
        def m = [:]

        1.upto( 10 ){ int j -> m.put( "[$j]", "<$j>" ) }
        1.upto( 10 ){ int j -> assertEquals( "<$j>", m.get( "[$j]" )) }
        1.upto( 10 ){ int j -> assertEquals( "<$j>", m[ "[$j]" ]    ) }  // Fails, returns null, can't be fixed
    }

}
