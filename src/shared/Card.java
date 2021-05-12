package shared;

import utils.Const;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class Card {
    private final ArrayList<CardEvent> cardHistory;
    private final String name;
    private final String description;
    private String status;

    public Card(String name, String desc){
        this.cardHistory = new ArrayList<>();
        this.name = name;
        this.description = desc;
        setStatus(Const.TODO);

    }


    public String getName(){ return name; }

    public String getDescription(){ return description; }

    public String getStatus(){ return status; }

    public void setStatus(String status){
        //card is created
        String prevStatus;
        if(this.status == null) prevStatus = "(created)";
        else prevStatus = this.status;
        addEvent(new CardEvent(new Date().getTime()/1000, prevStatus, status));
        this.status = status;
    }

    private void addEvent(CardEvent e){
        cardHistory.add(e);
        Collections.sort(cardHistory);
    }

    public ArrayList<CardEvent> getCardHistory(){
        return this.cardHistory;
    }

}
