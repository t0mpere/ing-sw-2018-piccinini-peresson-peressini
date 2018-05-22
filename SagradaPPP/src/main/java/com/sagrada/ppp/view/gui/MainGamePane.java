package com.sagrada.ppp.view.gui;

import com.sagrada.ppp.*;
import com.sagrada.ppp.Cell;
import com.sagrada.ppp.cards.PublicObjectiveCard;
import com.sagrada.ppp.cards.ToolCards.ToolCard;
import com.sagrada.ppp.controller.RemoteController;
import com.sagrada.ppp.utils.StaticValues;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;


public class MainGamePane extends UnicastRemoteObject implements GameObserver, WindowPanelEventBus {

    private RoundTrack roundTrack;
    private double height,widht = 100d;

    private GridPane mainGamePane;
    private VBox opponentsWindowPanelsPane;
    private FlowPane bottomContainer;
    private RoundTrackPane roundTrackPane;
    private FlowPane centerContainer;
    private VBox draftPoolContainer;
    private FlowPane draftPoolPane;
    private WindowPanelPane playerWindowPanel;
    private GridPane toolCardsContainer,publicCardsContainer;
    private HBox topContainer;
    private Insets defInset;
    private TabPane tabContainer;
    private Tab gameTab,settingsTab,logTab;

    private Stage stage;
    private RemoteController controller;
    private com.sagrada.ppp.Color privateColor;
    private JoinGameResult joinGameResult;
    private HashMap<String, WindowPanel> panels;
    private ArrayList<Dice> draftPool;
    private ArrayList<DiceButton> draftPoolDiceButtons;
    private ArrayList<Button> toolCardButtons,publicCardButtons;
    private ArrayList<ToolCard> toolCards;
    private ArrayList<PublicObjectiveCard> publicObjectiveCards;
    private EventHandler<MouseEvent> draftPoolDiceEventHandler;

    public MainGamePane() throws RemoteException {


        defInset = new Insets(5);
        tabContainer = new TabPane();
        gameTab = new Tab();
        settingsTab = new Tab();
        logTab = new Tab();
        mainGamePane = new GridPane();
        toolCardButtons = new ArrayList<>();
        publicCardButtons = new ArrayList<>();
        opponentsWindowPanelsPane = new VBox();
        bottomContainer = new FlowPane();
        roundTrackPane = new RoundTrackPane();
        centerContainer = new FlowPane();
        draftPoolContainer = new VBox();
        draftPoolPane = new FlowPane();
        playerWindowPanel = new WindowPanelPane(new WindowPanel(0,0),440,400);
        toolCardsContainer = new GridPane();
        publicCardsContainer = new GridPane();
        topContainer = new HBox();
        draftPoolDiceButtons = new ArrayList<>();

        createListeners();

    }

    public void draw(){

        Label toolCardsTitle = new Label("Tool Cards");
        Label publicObjectiveCardsTitle = new Label("Public Objective Cards");
        toolCardsContainer.setVgap(5);
        toolCardsContainer.setHgap(5);
        toolCardsContainer.setPadding(defInset);
        toolCardsContainer.add(toolCardsTitle,0,0);
        publicCardsContainer.setVgap(5);
        publicCardsContainer.setHgap(5);
        publicCardsContainer.setPadding(defInset);
        publicCardsContainer.add(publicObjectiveCardsTitle,0,0,2,1);
        mainGamePane.add(opponentsWindowPanelsPane,1,0,1,3);
        drawToolCards();
        drawPublicObjectiveCards();

        topContainer.getChildren().addAll(toolCardsContainer,publicCardsContainer);
        topContainer.setAlignment(Pos.CENTER);
        GridPane.setHalignment(topContainer,HPos.CENTER);
        topContainer.setPadding(defInset);
        mainGamePane.add(topContainer,0,0,1,1);

        roundTrackPane.init();
        mainGamePane.setBackground(new Background(new BackgroundFill(Color.LIGHTGRAY,new CornerRadii(5),new Insets(0))));

        ImageView privateCardImageView = new ImageView();
        FlowPane.setMargin(privateCardImageView,defInset);
        privateCardImageView.setImage(new Image(StaticValues.FILE_URI_PREFIX + "graphics/PrivateCards/private_"+privateColor.toString().toLowerCase()+".png",150,204,true,true));

        bottomContainer.setAlignment(Pos.CENTER);
        GridPane.setHalignment(bottomContainer,HPos.CENTER);
        bottomContainer.getChildren().addAll(privateCardImageView,roundTrackPane);
        bottomContainer.setPadding(defInset);
        mainGamePane.add(bottomContainer,0,2,1,1);

        Label draftPoolTitle = new Label("DraftPool");
        draftPoolTitle.setTextFill(Color.BLACK);
        draftPoolTitle.setAlignment(Pos.CENTER);

        draftPoolContainer.setAlignment(Pos.CENTER);
        draftPoolContainer.setPadding(defInset);
        draftPoolContainer.getChildren().addAll(draftPoolTitle, draftPoolPane);

        draftPoolPane.setHgap(2);
        draftPoolPane.setVgap(2);
        draftPoolPane.setPrefWrapLength(190);

        drawDraftPool();
        centerContainer.setBackground(
                new Background(
                    new BackgroundFill(
                        Color.DARKGREEN,
                        new CornerRadii(5),
                        Insets.EMPTY)));
        centerContainer.getChildren().add(draftPoolContainer);
        centerContainer.setAlignment(Pos.CENTER);
        centerContainer.setPadding(defInset);
        mainGamePane.add(centerContainer,0,1,1,1);

        opponentsWindowPanelsPane.setFillWidth(false);
        opponentsWindowPanelsPane.setAlignment(Pos.CENTER);
        opponentsWindowPanelsPane.setSpacing(5);
        opponentsWindowPanelsPane.getChildren().add(new Label("OpponentsPanels:"));

        //drawing All Window Panels
        GridPane.setFillWidth(centerContainer,true);
        centerContainer.getChildren().add(playerWindowPanel);
        playerWindowPanel.setObserver(this);
        HBox.setHgrow(playerWindowPanel,Priority.ALWAYS);
        HBox.setMargin(playerWindowPanel,defInset);
        drawWindowPanels();
        opponentsWindowPanelsPane.setPadding(defInset);


        //setting up all tabs
        gameTab.setContent(mainGamePane);
        gameTab.setText("Game Tab");
        gameTab.setClosable(false);

        settingsTab.setClosable(false);
        settingsTab.setText("Settings Tab");

        logTab.setClosable(false);
        logTab.setText("LogTab");

        tabContainer.getTabs().addAll(gameTab,settingsTab,logTab);
        //creating all Listeners

        stage.setScene(new Scene(tabContainer, 700, 1270));
        stage.setTitle("Main game");
        stage.setResizable(true);
        stage.show();

    }

    public void init(com.sagrada.ppp.Color privateColor, JoinGameResult joinGameResult, HashMap<String, WindowPanel> panelsAlreadyChosen,
                     ArrayList<Dice> draftPool,ArrayList<ToolCard> toolCards,ArrayList<PublicObjectiveCard> publicObjectiveCards, RemoteController controller, Stage stage) {
        this.controller = controller;
        this.stage = stage;
        this.privateColor = privateColor;
        this.joinGameResult = joinGameResult;
        this.panels = panelsAlreadyChosen;
        this.draftPool = draftPool;
        this.toolCards = toolCards;
        this.publicObjectiveCards = publicObjectiveCards;
        try {
            this.controller.attachGameObserver(this.joinGameResult.getGameHashCode(),this);
        } catch (RemoteException e) {
            e.printStackTrace();
        }


        draw();
    }

    private void drawWindowPanels(){
        opponentsWindowPanelsPane.getChildren().clear();
        for (HashMap.Entry<String,WindowPanel> entry: panels.entrySet()) {
            if (entry.getKey().equals(joinGameResult.getUsername())) {
                if(playerWindowPanel == null) {
                    playerWindowPanel = new WindowPanelPane(entry.getValue(), 330, 300);

                }else {
                    playerWindowPanel.setPanel(entry.getValue());
                }
            }else {
                Label username = new Label(entry.getKey() +"\t Remaining Tokens :change" );
                username.setTextFill(Color.BLACK);
                username.setAlignment(Pos.CENTER);
                opponentsWindowPanelsPane.getChildren().add(username);
                opponentsWindowPanelsPane.getChildren().add(new WindowPanelPane(entry.getValue(),200,170));
            }
        }

    }
    private void drawDraftPool(){
        draftPoolDiceButtons.clear();
        draftPoolPane.getChildren().clear();

        int index = 0;
        for (Dice dice:draftPool) {
            DiceButton diceButton = new DiceButton(dice,70,70);
            diceButton.setIndex(index);
            FlowPane.setMargin(diceButton,new Insets(10));
            diceButton.addEventHandler(MouseEvent.MOUSE_CLICKED, draftPoolDiceEventHandler);
            draftPoolDiceButtons.add(diceButton);
            index++;
        }
        draftPoolPane.getChildren().addAll(draftPoolDiceButtons);
    }
    private void drawToolCards(){
        int count = 0;
        for(ToolCard toolCard : toolCards){
            Button toolCardButton = new Button();
            toolCardButtons.add(toolCardButton);
            toolCardButton.setId(Integer.toString(toolCard.getId()));
            toolCardButton.setMinSize(150,204);
            toolCardButton.setBackground(
                    new Background(
                            new BackgroundImage(
                                    new Image(StaticValues.FILE_URI_PREFIX + "graphics/ToolCards/tool_"+toolCard.getId()+".png",150,204,true,true),
                                    BackgroundRepeat.NO_REPEAT,
                                    BackgroundRepeat.NO_REPEAT,
                                    BackgroundPosition.CENTER,
                                    BackgroundSize.DEFAULT
                            )
                    )
            );
            toolCardsContainer.add(toolCardButton,count,1);
            count++;
        }
    }
    private void drawPublicObjectiveCards(){
        int count = 0;
        for(PublicObjectiveCard publicObjectiveCard : publicObjectiveCards){
            Button publicObjectiveButton = new Button();
            publicCardButtons.add(publicObjectiveButton);
            publicObjectiveButton.setId(Integer.toString(publicObjectiveCard.getId()));
            publicObjectiveButton.setMinSize(150,204);
            publicObjectiveButton.setBackground(
                    new Background(
                            new BackgroundImage(
                                    new Image(StaticValues.FILE_URI_PREFIX + "graphics/PublicCards/public_"+publicObjectiveCard.getId()+".png",150,204,true,true),
                                    BackgroundRepeat.NO_REPEAT,
                                    BackgroundRepeat.NO_REPEAT,
                                    BackgroundPosition.CENTER,
                                    BackgroundSize.DEFAULT
                            )
                    )
            );
            publicCardsContainer.add(publicObjectiveButton,count,1);
            count++;
        }
    }
    private void createListeners(){
        draftPoolDiceEventHandler = event -> {
            DiceButton clickedButton = ((DiceButton) event.getSource());
            if (clickedButton.isSelected()) {
                clickedButton.setSelected(false);
                clickedButton.setScaleY(1);
                clickedButton.setScaleX(1);
            }else {
                for (DiceButton diceButton : draftPoolDiceButtons) {
                    if (diceButton.isSelected()) {
                        diceButton.setSelected(false);
                        diceButton.setScaleX(1);
                        diceButton.setScaleY(1);
                    }
                }
                clickedButton.setScaleX(1.2);
                clickedButton.setScaleY(1.2);
                clickedButton.setSelected(true);
            }

        };
    }

    @Override
    public void onPanelChoice(int playerHashCode, ArrayList<WindowPanel> panels, HashMap<String, WindowPanel> panelsAlreadyChosen, com.sagrada.ppp.Color playerPrivateColor) throws RemoteException {
            //Do nothing here
    }





    @Override
    public void onCellClicked(int x,int y) {
        DiceButton diceButtonSelected = draftPoolDiceButtons.stream().filter(DiceButton::isSelected).findFirst().orElse(null);
        if(diceButtonSelected != null){
            Platform.runLater(()-> {
                        try {
                            PlaceDiceResult result = controller.placeDice(joinGameResult.getGameHashCode(),joinGameResult.getPlayerHashCode(),draftPoolDiceButtons.indexOf(diceButtonSelected),y,x);
                            if(result.status){
                                playerWindowPanel.setPanel(result.panel);
                                draftPool.remove(draftPoolDiceButtons.indexOf(diceButtonSelected));
                                drawDraftPool();
                            }else{
                                Alert alert = new Alert(Alert.AlertType.ERROR,result.message);
                                alert.showAndWait();
                            }

                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
            );

        }else {
            Alert alert = new Alert(Alert.AlertType.ERROR,"Please Select a dice and THEN click on a panel cell!");
            alert.showAndWait();
        }

    }

    @Override
    public void onDiceClicked(DiceButton diceButton, Dice dice) {

    }

    @Override
    public void onGameStart(GameStartMessage gameStartMessage) throws RemoteException {

    }

    @Override
    public void onDicePlaced(DicePlacedMessage dicePlacedMessage) throws RemoteException {
        Platform.runLater(()->{
            if(dicePlacedMessage.username != joinGameResult.getUsername()) {
                panels.put(dicePlacedMessage.username, dicePlacedMessage.panel);
                draftPool = dicePlacedMessage.draftPool;
                drawDraftPool();
                drawWindowPanels();
            }
        });

    }

    @Override
    public void onEndTurn(EndTurnMessage endTurnMessage) throws RemoteException {

    }
}
