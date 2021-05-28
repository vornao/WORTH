package exceptions;

public class CardNotFoundException extends Exception{
    public CardNotFoundException(){
        super();
    }
    public CardNotFoundException(String e){
        super(e);
    }
}
