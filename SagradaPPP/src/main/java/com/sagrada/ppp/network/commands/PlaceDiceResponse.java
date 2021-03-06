package com.sagrada.ppp.network.commands;

import com.sagrada.ppp.model.PlaceDiceResult;

import java.io.Serializable;

/**
 * Class container used in socket connection. Response to :
 * @see PlaceDiceRequest
 */
public class PlaceDiceResponse implements Serializable, Response {

    public PlaceDiceResult placeDiceResult;

    public PlaceDiceResponse(PlaceDiceResult placeDiceResult){
        this.placeDiceResult = new PlaceDiceResult(placeDiceResult.message, placeDiceResult.status, placeDiceResult.panel, placeDiceResult.draftPool);
    }

    @Override
    public void handle(ResponseHandler handler) {
        handler.handle(this);
    }
}
