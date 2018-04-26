package com.sagrada.ppp.Cards;

import com.sagrada.ppp.*;
import java.util.LinkedHashMap;

public class CommandToolCard4 implements CommandToolCard {

    private LinkedHashMap<Integer, Integer> positions;
    private WindowPanel windowPanel;

    public CommandToolCard4(LinkedHashMap<Integer, Integer> positions, WindowPanel windowPanel) {
        this.positions = positions;
        this.windowPanel = windowPanel;
    }

    @Override
    public void useCard() {
        for (Integer pos : positions.keySet()) {
            windowPanel.addDiceOnCellWithIndex(positions.get(pos), windowPanel.removeDice(pos));
        }
    }

}