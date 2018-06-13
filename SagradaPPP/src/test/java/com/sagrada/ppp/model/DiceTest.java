package com.sagrada.ppp.model;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class DiceTest {

    private ArrayList<Color> colors = new ArrayList<>();

    public DiceTest() {
        colors.add(Color.BLUE);
        colors.add(Color.GREEN);
        colors.add(Color.PURPLE);
        colors.add(Color.RED);
        colors.add(Color.YELLOW);
    }

    @Test
    public void throwDice() {
        Dice dice = new Dice(Color.RED, 2);

        dice.throwDice();

        assertEquals(Color.RED, dice.getColor());
        assertTrue(dice.getValue() >= 1 || dice.getValue() <=6);
    }

    @Test
    public void isSimilar() {
        Dice dice = new Dice(Color.PURPLE, 1);
        Dice tempDice = new Dice(Color.GREEN, 1);

        assertTrue(tempDice.isSimilar(dice));

        tempDice.setColor(Color.PURPLE);
        tempDice.setValue(5);

        assertTrue(tempDice.isSimilar(dice));

        tempDice.setValue(1);

        assertTrue(tempDice.isSimilar(dice));

        tempDice.setColor(Color.YELLOW);
        tempDice.setValue(4);

        assertFalse(tempDice.isSimilar(dice));
    }

    @Test
    public void getColor() {
        Dice dice = new Dice();

        assertTrue(colors.contains(dice.getColor()));

        dice = new Dice(Color.GREEN);

        assertEquals(Color.GREEN, dice.getColor());
    }

    @Test
    public void setColor() {
        Dice dice = new Dice();

        dice.setColor(Color.GREEN);
        assertEquals(Color.GREEN, dice.getColor());
        assertNotEquals(Color.RED, dice.getColor());
    }

    @Test
    public void getValue() {
        Dice dice = new Dice();

        assertTrue(dice.getValue() >= 1 || dice.getValue() <=6);

        dice = new Dice(6);

        assertEquals(6, dice.getValue());
    }

    @Test
    public void setValue() {
        Dice dice = new Dice(5);

        dice.setValue(3);
        assertEquals(3, dice.getValue());
        assertNotEquals(5, dice.getValue());
    }

    @Test
    public void equals() {
        Dice dice = new Dice(Color.GREEN, 6);

        assertEquals(Color.GREEN, dice.getColor());
        assertEquals(6, dice.getValue());

        Dice tempDice = new Dice(dice);

        assertNotEquals(dice.hashCode(), tempDice.hashCode());
        assertEquals(dice.getValue(), tempDice.getValue());
        assertEquals(dice.getColor(), tempDice.getColor());
        assertTrue(tempDice.equals(dice));

        tempDice.setValue(3);
        tempDice.setColor(Color.BLUE);

        assertFalse(tempDice.equals(dice));
    }

    @Test
    public void toStringTest() {
        Dice dice = new Dice(Color.GREEN, 6);

        assertEquals("Color: " + dice.getColor() + ", value: " +
                dice.getValue(), dice.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void DiceException() {
        new Dice(Color.getRandomColor(), 777);
    }


    @Test(expected = IllegalArgumentException.class)
    public void setValueException() {
        Dice dice = new Dice();
        dice.setValue(777);
    }

}