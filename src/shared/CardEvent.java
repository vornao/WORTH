package shared;

import java.util.Date;

public class CardEvent implements Comparable<CardEvent>{
    private final Date date;
    private final String from;
    private final String to;

    public CardEvent(Date date, String from, String to){
        this.date = date;
        this.from = from;
        this.to = to;
    }

    public Date getDate() {
        return date;
    }

    public String getFrom(){
        return from;
    }

    public String getTo(){
        return to;
    }

    @Override
    public int compareTo(CardEvent o) {
        return this.date.compareTo(o.getDate());
    }
}
