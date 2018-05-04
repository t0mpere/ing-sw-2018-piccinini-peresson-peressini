package com.sagrada.ppp.cards;

import com.sagrada.ppp.Dice;
import com.sagrada.ppp.utils.StaticValues;
import com.sagrada.ppp.WindowPanel;

//Deep Shades: sets of 5 & 6 values anywhere

public class PublicObjectiveCard7 extends PublicObjectiveCard {

    public PublicObjectiveCard7() {
        super(StaticValues.PUBLICOBJECTIVECARD7_NAME, 7);
    }

    @Override
    public int getScore(WindowPanel playerWindowPanel) {
        int numberOfFive = 0;
        int numberOfSix = 0;
        for (int i = 0; i < StaticValues.NUMBER_OF_CELLS; i++) {
            Dice tempDice = playerWindowPanel.getCell(i).getDiceOn();
            if (tempDice != null) {
                int tempValue = tempDice.getValue();
                if (tempValue == 5) {
                    numberOfFive++;
                }
                else if (tempValue == 6) {
                    numberOfSix++;
                }
            }
        }
        if(numberOfFive < numberOfSix) {
            return numberOfFive * 2;
        }
        else {
            return numberOfSix * 2;
        }
    }

}