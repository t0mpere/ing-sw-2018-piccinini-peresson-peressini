package PPP;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.FileNotFoundException;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */

    public void testApp() throws IllegalDiceValueException {
        //CellTest.cellValueTest();
        //DiceTest.diceValueTest();
        //DiceBagTest.diceBagGenTest();
        //WindowPanelTest.testPanelComposition();
        //RoundTrackTest.testRoundTrack();
        //WindowPanelTest.testDiceOn();
        //GameTest.joiningTest();



        assertTrue( true);
    }
    public void testCards() throws FileNotFoundException {
        PublicObjectiveCardTest.card1();
        PublicObjectiveCardTest.card9();
        PublicObjectiveCardTest.card10();
        PublicObjectiveCardTest.card8();
    }
}