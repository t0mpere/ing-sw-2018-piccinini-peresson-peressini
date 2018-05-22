package com.sagrada.ppp;

import com.sagrada.ppp.cards.*;
import com.sagrada.ppp.cards.ToolCards.*;
import com.sagrada.ppp.network.commands.PanelChoiceNotification;
import com.sagrada.ppp.utils.StaticValues;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class Game implements Serializable{
    private ArrayList<Player> players;
    private Player activePlayer;
    private DiceBag diceBag;
    private ArrayList<WindowPanel> panels;
    private ArrayList<Dice> draftPool;
    private RoundTrack roundTrack;
    private GameStatus gameStatus;
    private ArrayList<LobbyObserver> lobbyObservers;
    private LobbyTimer lobbyTimer;
    private long lobbyTimerStartTime;
    public volatile boolean waitingForPanelChoice;
    public volatile boolean panelChoiceTimerExpired;
    public Integer chosenPanelIndex;
    public ArrayList<GameObserver> gameObservers;
    private ArrayList<ToolCard> toolCards;
    private ArrayList<PublicObjectiveCard> publicObjectiveCards;
    private int turn;

    /*
    TODO: Add a method that given the username string returns the desired players
    TODO: Add overloading methods that take a Player as a parameter instead of a String
    */

    public Game(String username){
        diceBag = new DiceBag();
        players = new ArrayList<>();
        draftPool = new ArrayList<>();
        roundTrack = new RoundTrack(StaticValues.NUMBER_OF_TURNS);
        gameStatus = GameStatus.INIT;
        if(username != null) players.add(new Player(username));
        lobbyObservers = new ArrayList<>();
        waitingForPanelChoice = false;
        panelChoiceTimerExpired = false;
        chosenPanelIndex = null;
        gameObservers = new ArrayList<>();
        toolCards = new ArrayList<>();
        publicObjectiveCards = new ArrayList<>();
    }

    public void init(){
        gameStatus = GameStatus.ACTIVE;
        assignPrivateObjectiveColors();
        Collections.shuffle(players);
        turn = 1;
        HashMap<Integer, ArrayList<WindowPanel>> panels = extractPanels();
        for(int playerHashCode : panels.keySet()){
            waitingForPanelChoice = true;
            panelChoiceTimerExpired = false;
            System.out.println("Sending panels to " + playerHashCode);
            HashMap<String, WindowPanel> usernameToPanelHashMap = new HashMap<>();
            for(Player player : players){
                usernameToPanelHashMap.put(player.getUsername(),player.getPanel());
            }
            notifyPanelChoice(playerHashCode, panels.get(playerHashCode),usernameToPanelHashMap, getPlayerPrivateColor(playerHashCode));
            PanelChoiceTimer panelChoiceTimer = new PanelChoiceTimer(System.currentTimeMillis(), this);
            panelChoiceTimer.start();
            while(waitingForPanelChoice && !panelChoiceTimerExpired){
            }
            System.out.println("Proceeding due to flag change.");
            System.out.println("---> waitingForPanelChoice = " + waitingForPanelChoice);
            System.out.println("---> panelChoiceTimerExpired = " + panelChoiceTimerExpired);
            panelChoiceTimer.interrupt();
            if(chosenPanelIndex == null) chosenPanelIndex = 0;
            getPlayerByHashcode(playerHashCode).setPanel(panels.get(playerHashCode).get(chosenPanelIndex));

        }
        extractPublicObjCards();
        extractToolCards();
        roundTrack.setCurrentRound(1);
        draftPool.addAll(diceBag.extractDices(players.size() *2+1));
        System.out.println("Game is starting.. notify users of that");
        notifyGameStart();
        gameHandler();
    }

    public void gameHandler(){
        //TODO implement game logic
    }

    public ArrayList<Dice> getDraftPool(){
        ArrayList<Dice> h = new ArrayList<>();
        for(Dice dice : draftPool){
            h.add(new Dice(dice));
        }
        return h;
    }

    //TO DO check if enum should be passed as a copy and not as a reference --> private invariant
    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public Player getActivePlayer() {
        return activePlayer;
    }

    public Player getPlayer(String username){
        return players.stream().filter(x -> x.getUsername().equals(username)).findFirst().orElse(null);
    }

    public void toNextRound(){
        if (roundTrack.getCurrentRound() == 10){
            gameStatus = GameStatus.SCORE;
            System.out.println("WARNING --> 10 turns played");
            return;
        }
        else{
            roundTrack.setDicesOnTurn(roundTrack.getCurrentRound(), getDraftPool());
            roundTrack.setCurrentRound(roundTrack.getCurrentRound() + 1);
            draftPool.clear();
            draftPool.addAll(diceBag.extractDices(players.size() *2 + 1));
        }
    }

    public long getLobbyTimerStartTime(){
        return lobbyTimerStartTime;
    }

    public int joinGame(String username, LobbyObserver lobbyObserver, GameObserver gameObserver) {
        int i = 1;
        String user = username;
        while(isInMatch(user)){
            user = username + "(" + i + ")";
            i++;
        }
        Player h = new Player(user);
        players.add(h);
        notifyPlayerJoin(user,getUsernames(),players.size());
        attachLobbyObserver(lobbyObserver);
        attachGameObserver(gameObserver);
        if(players.size() == 2){
            //start timer
            lobbyTimerStartTime = System.currentTimeMillis();
            lobbyTimer = new LobbyTimer(lobbyTimerStartTime, this);
            lobbyTimer.start();
            notifyTimerChanges(TimerStatus.START);
        }
        else{
            if(players.size() == 4){
                //starting game
                lobbyTimer.interrupt();
                notifyTimerChanges(TimerStatus.FINISH);
                Runnable myrunnable = () -> {
                    init();
                };
                new Thread(myrunnable).start();

            }
        }
        return h.hashCode();
    }

    public boolean isJoinable(){
        return players.size() < StaticValues.MAX_USER_PER_GAME && gameStatus.equals(GameStatus.INIT);
    }

    public String getPlayerUsername(int hashCode){
        for(Player player : players){
            if (player.hashCode() == hashCode) return player.getUsername();
        }
        return null;
    }


    public boolean isInMatch(String username){
        for(Player player : players){
            if (player.getUsername().equals(username)) return true;
        }
        return false;
    }

    public ArrayList<Player> getPlayers(){
        ArrayList<Player> playersCopy = new ArrayList<>();
        for(Player player : players){
            playersCopy.add(new Player(player));
        }
        return playersCopy;
    }
    public int getActivePlayersNumber(){
        return (int) players.stream().filter(x -> x.getPlayerStatus()==PlayerStatus.ACTIVE).count();
    }

    public ArrayList<String> getUsernames(){
        ArrayList<String> playersUsername = new ArrayList<>();
        for(Player player : players){
            playersUsername.add(player.getUsername());
        }
        return playersUsername;
        //Commented cause im not sure this keep the array ordered
        //naive way doesn't fail
        //return this.getPlayers().stream().map(Player::getUsername).collect(Collectors.toCollection(ArrayList::new));
    }
    public int getPrivateScore(Player activePlayer) {
        int score = 0;
        for (int i = 0; i < StaticValues.NUMBER_OF_CELLS; i++) {
            Dice tempDice = activePlayer.getPanel().getCell(i).getDiceOn();
            if (tempDice != null) {
                if (tempDice.getColor() == activePlayer.getPrivateColor()) {
                    score += tempDice.getValue();
                }
            }
        }
        return score;
    }

    public void leaveLobby(String username, LobbyObserver observer){
        for(Player player : players){
            if (player.getUsername().equals(username)){
                players.remove(player);
                detachLobbyObserver(observer);
                notifyPlayerLeave(username,getUsernames(),players.size());
                if(players.size() < 2){
                    if(lobbyTimer != null) {
                        lobbyTimer.interrupt();
                    }
                    lobbyTimerStartTime = 0;
                    notifyTimerChanges(TimerStatus.INTERRUPT);
                }
                return;
            }
        }
    }

    public int getPlayerHashCode(String username){
        for(Player player : players){
            if (player.getUsername().equals(username)){
                return player.hashCode();
            }
        }
        return -1;
    }


    public void attachGameObserver(GameObserver observer){
        if(observer == null) return;
        gameObservers.add(observer);
    }

    public void detachGameObserver(GameObserver observer){
        lobbyObservers.remove(observer);
    }

    public void attachLobbyObserver(LobbyObserver observer){
        if(observer == null) return;
        lobbyObservers.add(observer);
    }

    public void detachLobbyObserver(LobbyObserver observer){
        lobbyObservers.remove(observer);
    }

    public void notifyPanelChoice(int playerHashCode, ArrayList<WindowPanel> panels, HashMap<String, WindowPanel> panelsAlreadyChosen, Color color){
        for(GameObserver observer : gameObservers){
            try {
                observer.onPanelChoice(playerHashCode, panels, panelsAlreadyChosen, color);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }



    public void notifyTimerChanges(TimerStatus timerStatus) {
        for (LobbyObserver observer : lobbyObservers) {
            try {
                System.out.println("I'm notifying a new LobbyTimerStatus = " + timerStatus);
                observer.onTimerChanges(lobbyTimerStartTime, timerStatus);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }


    public void notifyPlayerJoin (String username,ArrayList<String> players, int numOfPlayers){
            for (LobbyObserver observer: lobbyObservers) {
                try {

                    observer.onPlayerJoined(username , players,numOfPlayers);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
    }

    public void notifyGameStart(){
        HashMap<String, WindowPanel> usernameToPanel = new HashMap<>();
        for(Player player : players){
            usernameToPanel.put(player.getUsername(), player.getPanel());
        }
        for(GameObserver gameObserver : gameObservers){
            try {
                gameObserver.onGameStart(new GameStartMessage(usernameToPanel, draftPool, toolCards, publicObjectiveCards, getUsernames()));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void notifyPlayerLeave(String username,ArrayList<String> players,int numOfPlayers) {
        for (LobbyObserver observer: lobbyObservers) {
            try {
                observer.onPlayerLeave(username ,players, numOfPlayers);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public HashMap<Integer, ArrayList<WindowPanel>> extractPanels() {
        HashMap<Integer, ArrayList<WindowPanel>> temp = new HashMap<>();
        ArrayList<Integer> notUsedPanel = new ArrayList<>();
        for(int i = 1; i <= StaticValues.NUMBER_OF_CARDS; i++ ){
            notUsedPanel.add(i);
        }
        for(Player player : players){
            int randomNum = ThreadLocalRandom.current().nextInt(0, notUsedPanel.size());
            ArrayList<WindowPanel> panels = new ArrayList<>();
            panels.add(new WindowPanel(notUsedPanel.get(randomNum), 1));
            panels.add(new WindowPanel(notUsedPanel.get(randomNum), 0));
            notUsedPanel.remove(randomNum);
            randomNum = ThreadLocalRandom.current().nextInt(0, notUsedPanel.size());
            panels.add(new WindowPanel(notUsedPanel.get(randomNum), 1));
            panels.add(new WindowPanel(notUsedPanel.get(randomNum), 0));
            notUsedPanel.remove(randomNum);
            temp.put(player.hashCode() , panels);
        }
        return temp;
    }

    public void assignPrivateObjectiveColors(){
        ArrayList<Integer> notUsedColor = new ArrayList<>();
        for(int i = 0; i < Color.values().length; i++){
            notUsedColor.add(i);
        }
        for (Player player : players){
            int index = ThreadLocalRandom.current().nextInt(0, notUsedColor.size());
            notUsedColor.remove(index);
            player.setPrivateColor(Color.values()[index]);
        }
    }


    public Color getPlayerPrivateColor(int playerHashCode){
        for(Player player : players){
            if(player.hashCode() == playerHashCode) return player.getPrivateColor();
        }
        return null;
    }

    public HashMap<Integer, Color> getPrivateObjectiveColors(){
        HashMap<Integer, Color> result = new HashMap<>();
        for(Player player : players){
            result.put(player.hashCode(), player.getPrivateColor());
        }
        return result;
    }

    public void extractToolCards(){
        Random r = new Random();
        ArrayList<ToolCard> allToolCards = new ArrayList<>();

        allToolCards.add(new ToolCard1());
        allToolCards.add(new ToolCard2());
        allToolCards.add(new ToolCard3());
        allToolCards.add(new ToolCard4());
        allToolCards.add(new ToolCard5());
        allToolCards.add(new ToolCard6());
        allToolCards.add(new ToolCard7());
        allToolCards.add(new ToolCard8());
        allToolCards.add(new ToolCard9());
        allToolCards.add(new ToolCard10());

        for(int i = 0; i < 3 ; i++){
            toolCards.add(allToolCards.remove( r.nextInt(allToolCards.size()) ));
        }
    }

    public void extractPublicObjCards(){
        Random r = new Random();
        ArrayList<PublicObjectiveCard> allPubObjCards = new ArrayList<>();

        allPubObjCards.add(new PublicObjectiveCard1());
        allPubObjCards.add(new PublicObjectiveCard2());
        allPubObjCards.add(new PublicObjectiveCard3());
        allPubObjCards.add(new PublicObjectiveCard4());
        allPubObjCards.add(new PublicObjectiveCard5());
        allPubObjCards.add(new PublicObjectiveCard6());
        allPubObjCards.add(new PublicObjectiveCard7());
        allPubObjCards.add(new PublicObjectiveCard8());
        allPubObjCards.add(new PublicObjectiveCard9());
        allPubObjCards.add(new PublicObjectiveCard10());

        for(int i = 0; i < 3 ; i++){
            publicObjectiveCards.add(allPubObjCards.remove( r.nextInt(allPubObjCards.size()) ));
        }
    }

    public void pairPanelToPlayer(int playerHashCode, int panelIndex) {
        chosenPanelIndex = panelIndex;
    }

    public boolean disconnect(int playerHashCode, LobbyObserver lobbyObserver, GameObserver gameObserver){
        for(Player player : players){
            if(player.hashCode() == playerHashCode) {
                detachLobbyObserver(lobbyObserver);
                detachGameObserver(gameObserver);
                player.setPlayerStatus(PlayerStatus.INACTIVE);
                return true;
            }
        }
        return false;
    }

    private void reorderPlayers(){

        ArrayList<Player> h = new ArrayList<>();
        players.add(players.get(0));
        for(int i = 1; i < players.size() - 1; i++){
            players.set(i-1, players.get(i));
            h.add(players.get(i));
        }
        players.remove(players.size()-2);
    }

    public void setTurn(int turn){
        this.turn = turn;
    }


    public int getCurrentPlayerIndex(){
        if(turn > players.size()){
            return (players.size()-1) - (turn - players.size() - 1);
        }
        else{
            return turn - 1;
        }
    }

    public PlaceDiceResult placeDice(int playerHashCode, int diceIndex, int row, int col){
        Player currentPlayer = getPlayerByHashcode(playerHashCode);
        if(players.get(getCurrentPlayerIndex()).hashCode() != playerHashCode) return new PlaceDiceResult("Can't do game actions during others players turn", false,currentPlayer.getPanel());

        boolean result = currentPlayer.getPanel().addDiceOnCellWithPosition(row, col, draftPool.get(diceIndex));
        System.out.println("place dice result = " + result);
        if(result) {
            draftPool.remove(diceIndex);
            return new PlaceDiceResult("risiko è meglio",true,new WindowPanel(currentPlayer.getPanel()));
        }
        else {
            return new PlaceDiceResult("Invalid position, pay attention to game rules!" , false, new WindowPanel(currentPlayer.getPanel()));
        }
    }

    private Player getPlayerByHashcode(int hashCode){
        for(Player player : players){
            if(player.hashCode() == hashCode) return player;
        }
        return null;
    }

}

