package com.sagrada.ppp.network.commands;

import org.junit.Test;

import static org.junit.Assert.*;

public class IsToolCardUsableRequestTest {

    private IsToolCardUsableRequest isToolCardUsableRequest;

    @Test
    public void handle() {

        RequestHandler handler = new RequestHandler() {
            @Override
            public Response handle(Request request) {
                return null;
            }

            @Override
            public Response handle(JoinGameRequest request) {
                return null;
            }

            @Override
            public Response handle(LeaveGameRequest request) {
                return null;
            }

            @Override
            public Response handle(PanelChoiceRequest request) {
                return null;
            }

            @Override
            public Response handle(DisconnectionRequest request) {
                return null;
            }

            @Override
            public Response handle(PlaceDiceRequest request) {
                return null;
            }

            @Override
            public Response handle(EndTurnRequest request) {
                return null;
            }

            @Override
            public Response handle(DetachGameObserverRequest request) {
                return null;
            }

            @Override
            public Response handle(IsToolCardUsableRequest request) {
                assertEquals(isToolCardUsableRequest, request);
                return null;
            }

            @Override
            public Response handle(UseToolCardRequest request) {
                return null;
            }

            @Override
            public Response handle(GetLegalPositionRequest request) {
                return null;
            }

            @Override
            public Response handle(SpecialDicePlacementRequest request) {
                return null;
            }

            @Override
            public Response handle(PutDiceInDraftPoolRequest request) {
                return null;
            }

            @Override
            public Response handle(ReconnectionRequest request) {
                return null;
            }

            @Override
            public Response handle(DisableAFKRequest request) {
                return null;
            }
        };

        isToolCardUsableRequest = new IsToolCardUsableRequest(123, 12, 10);
        isToolCardUsableRequest.handle(handler);
    }
}