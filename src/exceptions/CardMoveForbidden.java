package exceptions;

public class CardMoveForbidden extends Exception{
    public CardMoveForbidden(){
        super();
    }
    public CardMoveForbidden(String e){
        super(e);
    }
}
