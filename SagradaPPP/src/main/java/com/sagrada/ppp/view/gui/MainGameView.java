package com.sagrada.ppp.view.gui;

import com.sagrada.ppp.model.Dice;
import com.sagrada.ppp.cards.publicobjectivecards.PublicObjectiveCard;
import com.sagrada.ppp.cards.toolcards.ToolCard;
import com.sagrada.ppp.controller.RemoteController;
import com.sagrada.ppp.model.*;
import com.sagrada.ppp.network.client.Client;
import com.sagrada.ppp.network.client.ConnectionHandler;
import com.sagrada.ppp.network.client.ConnectionModeEnum;
import com.sagrada.ppp.utils.PlayerTokenSerializer;
import com.sagrada.ppp.utils.StaticValues;
import com.sagrada.ppp.view.ToolCardHandler;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import static java.lang.System.*;

/**
 * Class managing the main game view.
 */
public class MainGameView extends UnicastRemoteObject implements GameObserver, GuiEventBus, ToolCardHandler {

    private RoundTrack roundTrack;
    private GridPane mainGamePane;
    private VBox opponentsWindowPanelsPane;
    private VBox leftContainer;
    private transient RoundTrackPane roundTrackPane;
    private VBox centerContainer;
    private HBox draftPoolPane;
    private transient WindowPanelPane playerWindowPanel;
    private GridPane toolCardsContainer;
    private GridPane publicCardsContainer;
    private HBox topContainer;
    private Insets defInset;
    private TabPane tabContainer;
    private Button skipButton;
    private Tab gameTab;
    private Tab settingsTab;
    private ScrollPane rightContainer;
    private HashMap<Integer,Tooltip> toolCardsToolTips;
    private boolean gameEnded = false;
    private boolean afk = false;

    private String currentPlayerUser;
    private Stage stage;
    private transient RemoteController controller;
    private com.sagrada.ppp.model.Color privateColor;
    private JoinGameResult joinGameResult;
    private ArrayList<Dice> draftPool;
    private ArrayList<Player> players;
    private ArrayList<DiceButton> draftPoolDiceButtons;
    private ArrayList<Button> toolCardButtons;
    private ArrayList<ToolCard> toolCards;
    private ArrayList<PublicObjectiveCard> publicObjectiveCards;
    private EventHandler<MouseEvent> draftPoolDiceEventHandler;
    private EventHandler<MouseEvent> skipButtonEventHandler;
    private EventHandler<MouseEvent> toolCardClickEvent;
    private Label gameStatus;
    private transient volatile  ToolCardFlags toolCardFlags;
    private boolean isToolCardUsed;
    private static final String ACTION_REQUIRED = "Action required";
    private transient ConnectionModeEnum connectionModeEnum;

    MainGameView() throws RemoteException  {

        defInset = new Insets(10);
        tabContainer = new TabPane();
        gameTab = new Tab();
        settingsTab = new Tab();
        mainGamePane = new GridPane();
        toolCardButtons = new ArrayList<>();
        opponentsWindowPanelsPane = new VBox();
        leftContainer = new VBox();
        roundTrackPane = new RoundTrackPane();
        rightContainer = new ScrollPane();
        roundTrackPane.setObserver(this);
        centerContainer = new VBox();
        draftPoolPane = new HBox();
        playerWindowPanel = new WindowPanelPane(null,440,400);
        toolCardsContainer = new GridPane();
        publicCardsContainer = new GridPane();
        topContainer = new HBox();
        skipButton = new Button();
        draftPoolDiceButtons = new ArrayList<>();
        gameStatus = new Label();
        toolCardFlags = new ToolCardFlags();
        toolCardsToolTips = new HashMap<>();
        connectionModeEnum = Client.getConnectionModeEnum();
        //creating all Listeners
        createListeners();

    }


    @Override
    public void onPlayerAFK(Player playerAFK, boolean isLastPlayer, Player lastPlayer) {
        if(isLastPlayer) gameEnded = true;

        Platform.runLater(()->{
        if (playerAFK.getHashCode() == joinGameResult.getPlayerHashCode()) {
            afk = true;
            Alert alert = new Alert(Alert.AlertType.NONE);
            alert.getDialogPane().getButtonTypes().add(ButtonType.OK);
            if(isLastPlayer){
                alert.setHeaderText(null);
                alert.setContentText("You went afk, leaving only one player!\nPress OK to close the game!");
                alert.setTitle("AFK status");
                alert.showAndWait();
                try {
                    controller.disconnect(joinGameResult.getGameHashCode(),joinGameResult.getPlayerHashCode());
                } catch (RemoteException e) {
                    out.println("ERROR --> SERVER CRASH DETECTED, CLOSING APPLICATION...");
                    exit(0);
                }
                exit(0);

            }else {
                alert.setHeaderText(null);
                alert.setContentText("Press OK to resume the game!");
                alert.setTitle("AFK status");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK) {

                    if (!gameEnded) {
                        try {
                            controller.disableAFK(joinGameResult.getGameHashCode(), joinGameResult.getPlayerHashCode());
                            afk = false;
                        } catch (RemoteException e) {
                            out.println("ERROR --> SERVER CRASH DETECTED, CLOSING APPLICATION...");
                            exit(0);
                        }
                    }else {
                        alert.setContentText("While you were afk, leaving only one player.\n YOU LOSE, press ok to close the game");
                        alert.showAndWait();
                        exit(0);
                    }
                }
            }



        }else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION,playerAFK.getUsername() + " is afk atm!");
            alert.setHeaderText(null);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initOwner(stage);
            alert.show();
        }

            if(isLastPlayer && lastPlayer.getHashCode() == joinGameResult.getPlayerHashCode()){
                Alert alert = new Alert(Alert.AlertType.NONE);
                gameEnded = true;
                alert.setContentText("You are the last player connected.\nYou WIN");
                stage.hide();
                alert.setTitle("Winning situescion");
                alert.setHeaderText(null);
                alert.initModality(Modality.WINDOW_MODAL);
                alert.getDialogPane().getButtonTypes().add(ButtonType.OK);
                alert.initOwner(stage);
                alert.showAndWait();
                exit(0);
            }

    });
    }

    private void draw(){

        Scene scene = new Scene(tabContainer, 1440, 900);

        URL url = null;
        try {
            url = new URL(StaticValues.STYLE_SHEET_URL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        if (url == null) {
            out.println("Resource not found. Aborting.");
            exit(-1);
        }
        String css = url.toExternalForm();
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

        skipButton.setText("Skip Turn");
        skipButton.getStyleClass().add("sagradabutton");
        skipButton.setPadding(new Insets(20));
        skipButton.setDisable(true);
        skipButton.addEventHandler(MouseEvent.MOUSE_CLICKED,skipButtonEventHandler);
        VBox.setMargin(skipButton,defInset);
        skipButton.setAlignment(Pos.CENTER);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(20);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(60);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(20);
        mainGamePane.getColumnConstraints().addAll(col1,col2,col3);
        RowConstraints row1 = new RowConstraints();
        row1.setVgrow(Priority.NEVER);
        RowConstraints row2 = new RowConstraints();
        row2.setVgrow(Priority.ALWAYS);
        mainGamePane.getRowConstraints().addAll(row1,row2);

        drawToolCards();
        drawPublicObjectiveCards();

        topContainer.getChildren().addAll(toolCardsContainer,publicCardsContainer);
        topContainer.setAlignment(Pos.CENTER);
        GridPane.setValignment(topContainer,VPos.CENTER);
        topContainer.setPadding(defInset);
        mainGamePane.add(topContainer,0,0,3,1);
        mainGamePane.getStyleClass().add("mainpane");
        Button privateObjectiveButton = new Button();
        privateObjectiveButton.setPadding(defInset);
        privateObjectiveButton.setMinSize(75,75);
        privateObjectiveButton.getStyleClass().add("privateColor");
        HBox.setMargin(privateObjectiveButton,defInset);
        HBox.setHgrow(privateObjectiveButton,Priority.ALWAYS);
        privateObjectiveButton.setBackground(new Background(new BackgroundFill(WindowPanelPane.getColor(privateColor),new CornerRadii(10),Insets.EMPTY)));

        gameStatus.setAlignment(Pos.CENTER);
        leftContainer.setAlignment(Pos.CENTER);
        leftContainer.setSpacing(20);
        leftContainer.getStyleClass().addAll("leftContainer");
        GridPane.setHalignment(leftContainer,HPos.CENTER);
        gameStatus.getStyleClass().add("title");
        HBox.setHgrow(roundTrackPane,Priority.NEVER);
        leftContainer.setFillWidth(false);
        HBox.setMargin(privateObjectiveButton,defInset);
        leftContainer.getChildren().addAll(roundTrackPane,privateObjectiveButton);
        leftContainer.setPadding(defInset);
        mainGamePane.add(leftContainer,0,1,1,1);


        draftPoolPane.setAlignment(Pos.CENTER);
        draftPoolPane.setPadding(defInset);
        draftPoolPane.setSpacing(5);

        drawDraftPool();
        playerWindowPanel.setBorder(new Border(new BorderStroke(Color.web("C7F4FC"),BorderStrokeStyle.SOLID,
                new CornerRadii(5),BorderStroke.MEDIUM)));

        centerContainer.setAlignment(Pos.CENTER);
        centerContainer.setPadding(defInset);
        GridPane.setFillWidth(centerContainer,false);
        GridPane.setFillHeight(centerContainer,false);
        mainGamePane.add(centerContainer,1,1,1,1);

        HBox tmp = new HBox();
        tmp.getChildren().add(opponentsWindowPanelsPane);
        tmp.setAlignment(Pos.CENTER);
        opponentsWindowPanelsPane.setAlignment(Pos.CENTER);
        opponentsWindowPanelsPane.setSpacing(5);
        rightContainer.setContent(tmp);
        rightContainer.setFitToWidth(true);
        rightContainer.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        mainGamePane.add(rightContainer,2,1,1,1);

        //drawing All Window Panels
        GridPane.setFillWidth(centerContainer,true);
        centerContainer.setSpacing(15);

        HBox tmpHbox = new HBox();
        VBox.setVgrow(tmpHbox,Priority.NEVER);
        tmpHbox.setAlignment(Pos.CENTER);
        HBox.setHgrow(playerWindowPanel,Priority.NEVER);
        tmpHbox.getChildren().add(playerWindowPanel);
        centerContainer.setAlignment(Pos.CENTER);
        centerContainer.getChildren().addAll(gameStatus,tmpHbox,draftPoolPane);
        playerWindowPanel.setObserver(this);
        drawWindowPanels();
        opponentsWindowPanelsPane.setPadding(defInset);

        //setting up all tabs
        gameTab.setContent(mainGamePane);
        gameTab.setText("Game");
        gameTab.setClosable(false);

        settingsTab.setClosable(false);
        settingsTab.setText("Settings");
        VBox settingsVBox = new VBox();
        settingsVBox.setSpacing(10);
        settingsVBox.setAlignment(Pos.TOP_CENTER);
        Label label = new Label("Change connection mode:");
        Button button = new Button("Change!");
        button.setOnAction(action -> {
            ArrayList<String> choices = new ArrayList<>();
            choices.add(ConnectionModeEnum.RMI.toString());
            choices.add(ConnectionModeEnum.SOCKET.toString());
            ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
            dialog.setTitle(ACTION_REQUIRED);
            dialog.setHeaderText(null);
            dialog.setContentText("Chose your action: ");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stage);
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(s -> Platform.runLater(() -> {
                if (!(connectionModeEnum.toString()).equals(s)) {
                    if (s.equals(choices.get(0))) {
                        connectionModeEnum = ConnectionModeEnum.RMI;
                        //if you are here, it means that you want to change socket -> rmi
                        //close socket connection before changing connection mode
                        try {
                            controller.detachAllGameObserver(joinGameResult.getGameHashCode(), joinGameResult.getPlayerHashCode());
                        } catch (RemoteException e) {
                            out.println("ERROR --> SERVER CRASH DETECTED, CLOSING APPLICATION...");
                            exit(0);
                        }
                    } else {
                        connectionModeEnum = ConnectionModeEnum.SOCKET;
                    }
                    ConnectionHandler connectionHandler = new ConnectionHandler(connectionModeEnum);
                    controller = connectionHandler.getController();
                    try {
                        controller.attachGameObserver(joinGameResult.getGameHashCode(), this, joinGameResult.getPlayerHashCode());
                    } catch (RemoteException e) {
                        out.println("ERROR --> SERVER CRASH DETECTED, CLOSING APPLICATION...");
                        exit(0);
                    }
                }
            }));
        });
        HBox connectionHBox = new HBox();
        connectionHBox.setPadding(new Insets(10, 0, 0, 10));
        connectionHBox.setSpacing(10);
        connectionHBox.getChildren().addAll(label, button);
        settingsVBox.getChildren().add(connectionHBox);
        settingsTab.setContent(settingsVBox);

        tabContainer.getTabs().addAll(gameTab, settingsTab);

        scene.getStylesheets().add(css);
        stage.setScene(scene);
        stage.setTitle("Main game");
        stage.setResizable(true);
        stage.show();
        stage.centerOnScreen();
        if (currentPlayerUser.equals(joinGameResult.getUsername())){
            skipButton.setDisable(false);
            Alert alert = new Alert(Alert.AlertType.INFORMATION,"It's your turn!");
            playerWindowPanel.setBorder(new Border(new BorderStroke(Color.web("1EA896"),BorderStrokeStyle.SOLID,
                    new CornerRadii(5),BorderStroke.MEDIUM)));
            alert.setHeaderText(null);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initOwner(stage);
            alert.show();
        }else {
            playerWindowPanel.setBorder(new Border(new BorderStroke(Color.web("FF715B"),BorderStrokeStyle.SOLID,
                    new CornerRadii(5),BorderStroke.MEDIUM)));
        }

        stage.setOnCloseRequest(t -> {
            try {
                controller.disconnect(joinGameResult.getGameHashCode(),joinGameResult.getPlayerHashCode());
            } catch (RemoteException e) {
                out.println("ERROR --> SERVER CRASH DETECTED, CLOSING APPLICATION...");
                exit(0);
            }
            Platform.exit();
            exit(0);
        });

    }

    void init(com.sagrada.ppp.model.Color privateColor, JoinGameResult joinGameResult, GameStartMessage gameStartMessage
            , RemoteController controller, Stage stage,boolean reconnection) {
        this.controller = controller;
        this.stage = stage;
        this.privateColor = privateColor;
        this.joinGameResult = joinGameResult;
        this.draftPool = gameStartMessage.draftpool;
        this.toolCards = gameStartMessage.toolCards;
        this.players = gameStartMessage.players;
        this.publicObjectiveCards = gameStartMessage.publicObjectiveCards;
        this.currentPlayerUser = gameStartMessage.currentPlayer.getUsername();
        if(!reconnection) {
            try {
                this.controller.attachGameObserver(this.joinGameResult.getGameHashCode(), this, joinGameResult.getPlayerHashCode());
            } catch (RemoteException e) {
                out.println("ERROR --> SERVER CRASH DETECTED, CLOSING APPLICATION...");
                exit(0);
            }
        }


        draw();
    }

    void init(com.sagrada.ppp.model.Color privateColor, JoinGameResult joinGameResult, GameStartMessage gameStartMessage
            , RemoteController controller, Stage stage){
        init(privateColor,joinGameResult,gameStartMessage,controller,stage,false);
    }

    private void drawWindowPanels(){
        opponentsWindowPanelsPane.getChildren().clear();
        Label topLabel = new Label("Opponents' panels:");
        topLabel.getStyleClass().remove("label");
        topLabel.getStyleClass().add("opponentsLabel");
        opponentsWindowPanelsPane.getChildren().add(topLabel);
        for (Player player : players) {
            if (player.getUsername().equals(joinGameResult.getUsername())) {
                gameStatus.setText(joinGameResult.getUsername() + "\nFavor Tokens Remaining:\t"
                        + player.getFavorTokens());
                if(playerWindowPanel == null) {
                    playerWindowPanel = new WindowPanelPane(player.getPanel(), 330, 300);

                }else {
                    playerWindowPanel.setPanel(player.getPanel());
                }
            }else {
                Label username = new Label("#" + players.indexOf(player) + " " + player.getUsername()
                        +"\t Remaining Tokens : " + player.getFavorTokens() );
                username.getStyleClass().remove("label");
                username.getStyleClass().add("opponentsLabel");
                opponentsWindowPanelsPane.getChildren().add(username);
                WindowPanelPane pane = new WindowPanelPane(player.getPanel(),200,170);

                Border border = new Border(new BorderStroke(
                        currentPlayerUser.equals(player.getUsername())?Color.web("1EA896"):Color.web("FF715B"),
                        BorderStrokeStyle.SOLID,
                        new CornerRadii(4),
                        BorderStroke.MEDIUM));
                pane.setBorder(border);
                opponentsWindowPanelsPane.getChildren().add(pane);
            }
        }
        opponentsWindowPanelsPane.getChildren().add(skipButton);

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
        toolCardButtons.clear();
        toolCardsToolTips.clear();
        for(ToolCard toolCard : toolCards){
            Button toolCardButton = new Button();
            toolCardButtons.add(toolCardButton);
            toolCardButton.setEffect(new DropShadow(10,Color.BLACK));
            Border border = new Border(new BorderStroke(Color.web("FFFFFF"),BorderStrokeStyle.SOLID,
                    new CornerRadii(3),BorderStroke.MEDIUM));
            toolCardButton.setBorder(border);
            toolCardButton.setId(Integer.toString(toolCard.getId()));
            toolCardButton.setMinSize(150,204);
            toolCardButton.setBackground(
                    new Background(
                            new BackgroundImage(
                                    new Image(StaticValues.FILE_URI_PREFIX + "resources/graphics/ToolCards/tool_"+toolCard.getId()+".png",150,204,true,true),
                                    BackgroundRepeat.NO_REPEAT,
                                    BackgroundRepeat.NO_REPEAT,
                                    BackgroundPosition.CENTER,
                                    BackgroundSize.DEFAULT
                            )
                    )
            );
            Tooltip tooltip = new Tooltip();
            tooltip.setText(StaticValues.getToolCardDescription(toolCard.getId()) + "\nCost: 1");
            tooltip.setWrapText(true);
            toolCardsToolTips.put(toolCard.getId(),tooltip);
            toolCardButton.setTooltip(tooltip);
            toolCardButton.addEventHandler(MouseEvent.MOUSE_CLICKED,toolCardClickEvent);
            toolCardsContainer.add(toolCardButton,count,1);
            count++;
        }
    }
    private void drawPublicObjectiveCards(){
        int count = 0;
        for(PublicObjectiveCard publicObjectiveCard : publicObjectiveCards){
            Button publicObjectiveButton = new Button();
            publicObjectiveButton.setEffect(new DropShadow(10,Color.BLACK));
            Border border = new Border(new BorderStroke(Color.web("FFFFFF"),BorderStrokeStyle.SOLID,new CornerRadii(3),BorderStroke.MEDIUM));
            publicObjectiveButton.setBorder(border);
            publicObjectiveButton.setId(Integer.toString(publicObjectiveCard.getId()));
            publicObjectiveButton.setMinSize(150,204);
            publicObjectiveButton.setBackground(
                    new Background(
                            new BackgroundImage(
                                    new Image(StaticValues.FILE_URI_PREFIX + "resources/graphics/PublicCards/public_"+publicObjectiveCard.getId()+".png",150,204,true,true),
                                    BackgroundRepeat.NO_REPEAT,
                                    BackgroundRepeat.NO_REPEAT,
                                    BackgroundPosition.CENTER,
                                    BackgroundSize.DEFAULT
                            )
                    )
            );
            Tooltip tooltip = new Tooltip();
            tooltip.setText(StaticValues.getPublicObjectiveCardDescription(publicObjectiveCard.getId()));
            tooltip.setWrapText(true);
            publicObjectiveButton.setTooltip(tooltip);
            publicCardsContainer.add(publicObjectiveButton,count,1);
            count++;
        }
    }

    /**
     * Creates listeners for the draft pool buttons, skip turn button and tool cards buttons
     */
    private void createListeners(){
        draftPoolDiceEventHandler = event -> {
            //managing the scaling effect on draft pool.
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
            if (toolCardFlags.isDraftPoolDiceRequired){
                try {
                    out.println("Index draft pool: " + draftPoolDiceButtons.indexOf(clickedButton));
                    controller.setDraftPoolDiceIndex(joinGameResult.getPlayerHashCode(),draftPoolDiceButtons
                            .indexOf(clickedButton));

                } catch (RemoteException e) {
                    out.println("ERROR --> SERVER CRASH DETECTED, CLOSING APPLICATION...");
                    exit(0);
                }
            }

        };
        skipButtonEventHandler = event -> {
            try {
                skipButton.setDisable(true);
                controller.endTurn(joinGameResult.getGameHashCode(),joinGameResult.getPlayerHashCode());
            } catch (RemoteException e) {
                out.println("ERROR --> SERVER CRASH DETECTED, CLOSING APPLICATION...");
                exit(0);
            }
        };
        toolCardClickEvent = event -> {
            Button toolCardButton =(Button) event.getSource();
            try {
                if (!isToolCardUsed) {
                    controller.isToolCardUsable(joinGameResult.getGameHashCode(), joinGameResult.getPlayerHashCode(),
                            toolCardButtons.indexOf(toolCardButton), this);
                }else{
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText(null);
                    alert.setContentText("You can't use another tool card in this turn!");
                    alert.initModality(Modality.APPLICATION_MODAL);
                    alert.initOwner(stage);
                    alert.showAndWait();
                }
            } catch (RemoteException e) {
                out.println("ERROR --> SERVER CRASH DETECTED, CLOSING APPLICATION...");
                exit(0);
            }
        };

    }

    @Override
    public void onPanelChoice(int playerHashCode, ArrayList<WindowPanel> panels,
                              HashMap<String,WindowPanel> panelsAlreadyChosen,
                              com.sagrada.ppp.model.Color playerPrivateColor) {
            //Do nothing here
    }

    @Override
    public void onPlayerReconnection(Player reconnectingPlayer) {
        Platform.runLater(()-> {
            Alert alert = new Alert(Alert.AlertType.NONE);
            alert.setContentText(reconnectingPlayer.getUsername() + " is back online!");
            alert.setTitle("Player Reconnected");
            alert.setHeaderText(null);
            alert.initModality(Modality.WINDOW_MODAL);
            alert.initOwner(stage);
            alert.getDialogPane().getButtonTypes().add(ButtonType.OK);
            alert.show();
        });
    }

    @Override
    public void onPlayerDisconnection(Player disconnectingPlayer, boolean isLastPlayer)  {
        Platform.runLater(()-> {
            Alert alert = new Alert(Alert.AlertType.NONE);
            alert.setTitle("Player Disconnected");
            alert.setHeaderText(null);
            alert.initModality(Modality.WINDOW_MODAL);
            alert.initOwner(stage);
            alert.getDialogPane().getButtonTypes().add(ButtonType.OK);

            if (!gameEnded) {
                if (isLastPlayer) {
                    alert.setContentText(disconnectingPlayer.getUsername() + " has disconnected from the game!\nYou are the last player connected.\nYou WIN");
                    gameEnded = true;
                    stage.hide();
                    alert.showAndWait();
                    exit(0);
                } else {
                    alert.setContentText(disconnectingPlayer.getUsername() + " has disconnected from the game!");
                    alert.show();
                }
            }
        });
    }

    @Override
    public void onToolCardUsed(ToolCardNotificationMessage toolCardUsedMessage)  {
        Platform.runLater(()->{

            if(!toolCardUsedMessage.player.getUsername().equals(joinGameResult.getUsername())) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, toolCardUsedMessage.player.getUsername()
                        + " has used toolcard #" + toolCardUsedMessage.toolCardID);
                alert.setTitle("ToolCard notification");
                alert.setHeaderText(null);
                alert.initModality(Modality.APPLICATION_MODAL);
                alert.initOwner(stage);
                alert.show();
                draftPool = toolCardUsedMessage.draftPool;
                players.stream().filter(x -> x.getUsername().equals(toolCardUsedMessage.player.getUsername()))
                        .findFirst().ifPresent(x-> x.setPanel(toolCardUsedMessage.player.getPanel()));
                drawDraftPool();
                drawWindowPanels();
                roundTrackPane.setRoundTrack(toolCardUsedMessage.roundTrack);
                toolCards.stream()
                        .filter(x-> x.getId() == toolCardUsedMessage.toolCardID)
                        .findFirst().ifPresent(x -> toolCardsToolTips.get(toolCardUsedMessage.toolCardID).setText( StaticValues.getToolCardDescription(x.getId()) + "\nCost: " + toolCardUsedMessage.toolCardCost));
            }
        });
    }

    /**
     * Handles the click of an empty cell, this function is called only by the player's instance of WindowPanel
     * @param row row of the clicked cell
     * @param col column of the clicked cell
     */
    @Override
    public void onCellClicked(int row, int col) {
        if (!(toolCardFlags.isPanelCellRequired || toolCardFlags.isSecondPanelCellRequired)) {

            //normal dice placement handling

            DiceButton diceButtonSelected = draftPoolDiceButtons.stream()
                    .filter(DiceButton::isSelected).findFirst().orElse(null);
            if (diceButtonSelected != null) {
                Platform.runLater(() -> {
                    PlaceDiceResult result;
                    try {
                        result = controller.placeDice(joinGameResult.getGameHashCode(),
                                joinGameResult.getPlayerHashCode(), draftPoolDiceButtons.indexOf(diceButtonSelected),
                                row, col);
                    } catch (RemoteException e) {
                        result = new PlaceDiceResult("Network Error",false,null,null);
                        out.println("ERROR --> SERVER CRASH DETECTED, CLOSING APPLICATION...");
                        exit(0);
                    }
                    if (result.status) {
                        playerWindowPanel.setPanel(result.panel);
                        draftPool = result.draftPool;
                        drawDraftPool();
                    }
                    else {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText(null);
                        alert.setContentText(result.message);
                        alert.initModality(Modality.APPLICATION_MODAL);
                        alert.initOwner(stage);
                        alert.showAndWait();
                    }

                });
            }
            else {
                //tool card handling
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Please select a dice and THEN click on a panel cell!");
                alert.initModality(Modality.APPLICATION_MODAL);
                alert.initOwner(stage);
                alert.showAndWait();
            }
        }
        else if (toolCardFlags.isPanelCellRequired) {
            panelCellRequired(row, col);
        }
        else if (toolCardFlags.isSecondPanelCellRequired) {
            secondPanelCellRequired(row, col);
        }
    }

    private void panelCellRequired(int row, int col) {
        toolCardFlags.isPanelCellRequired = false;
        try {
            controller.setPanelCellIndex(joinGameResult.getPlayerHashCode(),
                    row * StaticValues.PATTERN_COL + col);
        } catch (RemoteException e) {
            out.println("ERROR --> SERVER CRASH DETECTED, CLOSING APPLICATION...");
            exit(0);
        }
    }

    private void secondPanelCellRequired(int row, int col) {
        toolCardFlags.isSecondPanelCellRequired = false;
        try {
            controller.setSecondPanelCellIndex(joinGameResult.getPlayerHashCode(),
                    row * StaticValues.PATTERN_COL + col);
        } catch (RemoteException e) {
            out.println("ERROR --> SERVER CRASH DETECTED, CLOSING APPLICATION...");
            exit(0);
        }
    }

    /**
     * Called by WindowPanelPane when a cell with a dice is clicked
     * @param row row of the clicked cell
     * @param col column of the clicked cell
     */
    @Override
    public void onDiceClicked(int row, int col) {
        if (toolCardFlags.isPanelDiceRequired) {
            panelDiceRequired(row, col);
        }
        else if (toolCardFlags.isSecondPanelDiceRequired) {
            secondPanelDiceRequired(row, col);
        }
        else if (toolCardFlags.isSecondPanelCellRequired) {
            secondPanelCellRequired(row,col);
        }
    }

    private void panelDiceRequired(int row, int col) {
        toolCardFlags.isPanelDiceRequired = false;
        try {
            controller.setPanelDiceIndex(joinGameResult.getPlayerHashCode(), row *
                    StaticValues.PATTERN_COL + col);
        } catch (RemoteException e) {
            out.println("ERROR --> SERVER CRASH DETECTED, CLOSING APPLICATION...");
            exit(0);
        }
    }

    private void secondPanelDiceRequired(int row, int col) {
        toolCardFlags.isSecondPanelDiceRequired = false;
        try {
            controller.setSecondPanelDiceIndex(joinGameResult.getPlayerHashCode(), row *
                    StaticValues.PATTERN_COL + col);
        } catch (RemoteException e) {
            out.println("ERROR --> SERVER CRASH DETECTED, CLOSING APPLICATION...");
            exit(0);
        }
    }

    @Override
    public void onGameStart(GameStartMessage gameStartMessage)  {
        //to nothing
    }

    @Override
    public void onDicePlaced(DicePlacedMessage dicePlacedMessage) {
        Platform.runLater(()->{
            if(!dicePlacedMessage.username.equals(joinGameResult.getUsername())) {
                draftPool = dicePlacedMessage.draftPool;
                players.stream().filter(x -> x.getUsername().equals(dicePlacedMessage.username))
                        .findFirst().ifPresent(x -> x.setPanel(dicePlacedMessage.panel));
                drawDraftPool();
                drawWindowPanels();
            }
        });

    }

    @Override
    public void onEndTurn(EndTurnMessage endTurnMessage)  {
       Platform.runLater(() -> {
            isToolCardUsed = false;
            roundTrack = endTurnMessage.roundTrack;
            roundTrackPane.setRoundTrack(roundTrack);
            draftPool = endTurnMessage.draftpool;
            currentPlayerUser = endTurnMessage.currentPlayer.getUsername();
            players = endTurnMessage.players;
            drawDraftPool();
            drawWindowPanels();



            if (endTurnMessage.currentPlayer.getUsername().equals(joinGameResult.getUsername())) {
                playerWindowPanel.setBorder(new Border(new BorderStroke(Color.web("1EA896"), BorderStrokeStyle.SOLID,
                        new CornerRadii(5), BorderStroke.MEDIUM)));
                if(!gameEnded && !afk) {
                    skipButton.setDisable(false);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "It's your turn!");

                    alert.setHeaderText(null);
                    alert.initModality(Modality.APPLICATION_MODAL);
                    alert.initOwner(stage);
                    alert.show();
                }
            } else if (endTurnMessage.previousPlayer.getUsername().equals(joinGameResult.getUsername())) {
                playerWindowPanel.setBorder(new Border(new BorderStroke(Color.web("FF715B"), BorderStrokeStyle.SOLID,
                        new CornerRadii(5), BorderStroke.MEDIUM)));
                if(!gameEnded && !afk) {
                    skipButton.setDisable(true);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Your turn is ended!");
                    alert.setHeaderText(null);
                    alert.initModality(Modality.APPLICATION_MODAL);
                    alert.initOwner(stage);
                    alert.show();
                }
            }

        });
    }

    @Override
    public void onEndGame(ArrayList<PlayerScore> playersScore) {
        gameEnded = true;
        Platform.runLater(() -> {
            try {
                PlayerTokenSerializer.deleteToken();
                new EndGameView(playersScore, publicObjectiveCards, stage);
            } catch (RemoteException e) {
                out.println("ERROR --> SERVER CRASH DETECTED, CLOSING APPLICATION...");
                exit(0);
            }
        });
    }

    @Override
    public void isToolCardUsable(boolean result) {
        Platform.runLater(() -> {
          if(!result){
              Alert alert = new Alert(Alert.AlertType.ERROR);
              alert.setTitle("Error");
              alert.setHeaderText(null);
              alert.setContentText("ToolCard currently not usable");
              alert.initModality(Modality.APPLICATION_MODAL);
              alert.initOwner(stage);
              alert.showAndWait();
          }
        });

    }

    @Override
    public void draftPoolDiceIndexRequired()  {
        Platform.runLater(()-> {
            for (DiceButton diceButton : draftPoolDiceButtons) {
                if (diceButton.isSelected()) {
                    diceButton.setSelected(false);
                    diceButton.setScaleX(1);
                    diceButton.setScaleY(1);
                }
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Select a dice from draft pool!");
            alert.setTitle(ACTION_REQUIRED);
            alert.setHeaderText(null);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initOwner(stage);
            alert.showAndWait();
            toolCardFlags.reset();
            toolCardFlags.isDraftPoolDiceRequired = true;

        });
    }

    @Override
    public void panelDiceIndexRequired() {
        Platform.runLater(() -> {
            for (DiceButton diceButton : draftPoolDiceButtons) diceButton.setDisable(true);
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Select a dice from your Panel");
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initOwner(stage);
            alert.showAndWait();
            toolCardFlags.reset();
            toolCardFlags.isPanelDiceRequired = true;
        });
    }

    @Override
    public void panelCellIndexRequired()  {
        Platform.runLater(()-> {
            for (DiceButton diceButton : draftPoolDiceButtons) diceButton.setDisable(true);
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Select a cell from your panel!");
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initOwner(stage);
            alert.showAndWait();
            toolCardFlags.reset();
            toolCardFlags.isPanelCellRequired = true;
        });
    }

    @Override
    public void actionSignRequired()  {
        Platform.runLater(() -> {
            toolCardFlags.reset();
            ArrayList<String> choices = new ArrayList<>();
            choices.add("Increase the value by 1!");
            choices.add("Decrease the value by 1!");
            ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
            dialog.setTitle(ACTION_REQUIRED);
            dialog.setHeaderText(null);
            dialog.setContentText("Chose your action: ");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stage);
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                int sign;
                if (result.get().equals(choices.get(0))) {
                    sign = 1;
                }
                else {
                    sign = -1;
                }
                try {
                    controller.setActionSign(joinGameResult.getPlayerHashCode(), sign);
                } catch (RemoteException e) {
                    out.println("ERROR --> SERVER CRASH DETECTED, CLOSING APPLICATION...");
                    exit(0);
                }
            }
            else {
                isToolCardUsed = false;
            }
        });
    }

    @Override
    public void notifyUsageCompleted(UseToolCardResult useToolCardResult) {
        Platform.runLater(() -> {
            players = useToolCardResult.players;
            draftPool = useToolCardResult.draftpool;
            toolCardFlags.reset();
            roundTrackPane.setRoundTrack(useToolCardResult.roundTrack);
            for (DiceButton diceButton : draftPoolDiceButtons) diceButton.setDisable(false);
            drawDraftPool();
            drawWindowPanels();
           toolCards.stream()
                            .filter(x-> x.getId() == useToolCardResult.toolCardId)
                            .findFirst().ifPresent(x -> toolCardsToolTips.get(useToolCardResult.toolCardId).setText( StaticValues.getToolCardDescription(x.getId()) + "\nCost: " + useToolCardResult.toolCardCost));


            Alert alert = new Alert(Alert.AlertType.NONE);
            if(!gameEnded && !afk) {
                if (useToolCardResult.result) {
                    isToolCardUsed = true;
                    alert.setAlertType(Alert.AlertType.INFORMATION);
                    alert.setTitle("All good");
                    alert.setHeaderText(null);
                    alert.setContentText("Tool card used successfully!");
                    alert.initModality(Modality.APPLICATION_MODAL);
                    alert.initOwner(stage);
                } else {
                    isToolCardUsed = false;
                    alert.setAlertType(Alert.AlertType.INFORMATION);
                    alert.setTitle("Ehhhgggrrr, something went wrong...");
                    alert.setHeaderText("Negative result!");
                    alert.initModality(Modality.APPLICATION_MODAL);
                    alert.initOwner(stage);
                }
                alert.showAndWait();
            }
        });

    }

    @Override
    public void secondPanelDiceIndexRequired() {
        Platform.runLater(() -> {
            for (DiceButton diceButton : draftPoolDiceButtons) diceButton.setDisable(true);
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Select your second dice from your panel!");
            alert.setTitle(ACTION_REQUIRED);
            alert.setHeaderText(null);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initOwner(stage);
            alert.showAndWait();
            toolCardFlags.reset();
            toolCardFlags.isSecondPanelDiceRequired = true;
        });
    }

    @Override
    public void secondPanelCellIndexRequired() {
        Platform.runLater(() -> {
            for (DiceButton diceButton : draftPoolDiceButtons) diceButton.setDisable(true);
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Select the cell for your second dice!");
            alert.setTitle(ACTION_REQUIRED);
            alert.setHeaderText(null);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initOwner(stage);
            alert.showAndWait();
            toolCardFlags.reset();
            toolCardFlags.isSecondPanelCellRequired = true;
        });
    }

    @Override
    public void diceValueRequired(com.sagrada.ppp.model.Color color) {
        Platform.runLater(() -> {
            toolCardFlags.reset();
            ArrayList<Integer> choices = new ArrayList<>();
            for (int i = 1; i <= 6; i++) {
                choices.add(i);
            }
            ChoiceDialog<Integer> dialog = new ChoiceDialog<>(choices.get(0), choices);
            dialog.setTitle(ACTION_REQUIRED);
            dialog.setHeaderText(null);
            dialog.setContentText("You have drafted a " + color.toString().toUpperCase() + " dice!\n" +
                    "Now chose the value!");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stage);
            Optional<Integer> result = dialog.showAndWait();
            if (result.isPresent()) {
                try {
                    controller.setDiceValue(joinGameResult.getPlayerHashCode(), result.get());
                } catch (RemoteException e) {
                    out.println("ERROR --> SERVER CRASH DETECTED, CLOSING APPLICATION...");
                    exit(0);
                }
            }
            else {
                isToolCardUsed = false;
            }
        });
    }

    @Override
    public void twoDiceActionRequired() {
        Platform.runLater(() -> {
            toolCardFlags.reset();
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(ACTION_REQUIRED);
            alert.setHeaderText(null);
            alert.setContentText("Do you want to place another dice?");
            Button cancelButton = (Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL);
            cancelButton.setText("No");
            Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
            okButton.setText("Yes");
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initOwner(stage);
            Optional<ButtonType> result = alert.showAndWait();
            try {
                if (result.get() == ButtonType.OK) {
                    controller.setTwoDiceAction(joinGameResult.getPlayerHashCode(), true);
                }
                else {
                    controller.setTwoDiceAction(joinGameResult.getPlayerHashCode(), false);
                }
            } catch (RemoteException e) {
                out.println("ERROR --> SERVER CRASH DETECTED, CLOSING APPLICATION...");
                exit(0);
            }
        });
    }

    @Override
    public void roundTrackDiceIndexRequired() {
        Platform.runLater(() -> {
            toolCardFlags.reset();
            for (DiceButton diceButton : draftPoolDiceButtons) diceButton.setDisable(true);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(ACTION_REQUIRED);
            alert.setHeaderText("Round dice selection");
            alert.setContentText("Select a dice from Round Track");
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initOwner(stage);
            alert.showAndWait();
            toolCardFlags.isRoundTrackDiceRequired = true;
        });
    }

    @Override
    public void reRolledDiceActionRequired(Dice dice) {
        Platform.runLater(() -> {
            toolCardFlags.reset();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(ACTION_REQUIRED);
            alert.setGraphic(new ImageView(new Image(StaticValues.getAssetUri(dice.getColor(), dice.getValue()), 50, 50, true, true)));
            alert.setHeaderText("Your new drafted dice is: ");
            alert.setContentText(null);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initOwner(stage);
            alert.showAndWait();
        });
    }

    @Override
    public void onRoundTrackDiceClicked(int diceIndex, int roundIndex) {
        if (toolCardFlags.isRoundTrackDiceRequired) {
            try {
                toolCardFlags.isRoundTrackDiceRequired = false;
                controller.setRoundTrackDiceIndex(joinGameResult.getPlayerHashCode(), diceIndex, roundIndex);
            } catch (RemoteException e) {
                out.println("ERROR --> SERVER CRASH DETECTED, CLOSING APPLICATION...");
                exit(0);
            }
        }
    }

    @Override
    public void rmiPing() {
        //do nothing here
    }
}