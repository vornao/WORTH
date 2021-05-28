package shared;

import java.util.Date;

public class CardEvent implements Comparable<CardEvent>{
    private final long date;
    private final String from;
    private final String to;

    public CardEvent(long timestamp, String from, String to){
        this.date = timestamp;
        this.from = from;
        this.to = to;
    }

    public long getDate() {
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
        return (int)(this.date - o.getDate());
    }
}
