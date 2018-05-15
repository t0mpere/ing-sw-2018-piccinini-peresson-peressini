package com.sagrada.ppp;

import com.sagrada.ppp.utils.StaticValues;

public class PanelChoiceTimer extends Thread {
    long startTime;
    Game game;

    public PanelChoiceTimer(long startTime, Game game){
        this.startTime = startTime;
        this.game = game;
    }

    public void run(){
        while ((System.currentTimeMillis() < startTime + StaticValues.TURN_DURATION) && !isInterrupted()){

        }
        if(!isInterrupted()) {
            game.panelChoiceTimerExpired = true;
        }
    }
}