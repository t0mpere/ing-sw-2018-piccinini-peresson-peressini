package PPP;

public class IllegalDiceValueException extends Exception{

    public IllegalDiceValueException(Dice dice){
        super("Dice value out of bound, generated by dice => " + dice.toString());
    }

    public IllegalDiceValueException(){
        super("Error while generating dice");
    }


}
