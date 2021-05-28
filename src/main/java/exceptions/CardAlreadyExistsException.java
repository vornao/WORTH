package exceptions;

public class CardAlreadyExistsException extends Exception{
    public CardAlreadyExistsException(){
        super();
    }
    public CardAlreadyExistsException(String e){
        super(e);
    }
}
