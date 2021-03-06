package com.sagrada.ppp.network.commands;

import com.sagrada.ppp.model.DicePlacedMessage;

import java.io.Serializable;

/**
 * Response used to notify all client about the placement of a dice.
 */
public class DicePlacedNotification implements Response,Serializable {

    public DicePlacedMessage dicePlacedMessage;

    public DicePlacedNotification(DicePlacedMessage dicePlacedMessage) {
        this.dicePlacedMessage = dicePlacedMessage;
    }

    @Override
    public void handle(ResponseHandler handler) {
        handler.handle(this);
    }
}
