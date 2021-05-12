package shared;

import com.google.gson.JsonObject;
import exceptions.CardAlreadyExistsException;
import exceptions.CardMoveForbidden;
import exceptions.CardNotFoundException;
import server.User;
import shared.Card;
import utils.Const;

import java.util.ArrayList;
import java.util.HashMap;

public class Project {
    private final String name;
    private final String chatAddress;
    private final HashMap<String, HashMap<String, Card>> cardLists;
    private final HashMap<String, Card> todo;
    private final HashMap<String, Card> inProgress;
    private final HashMap<String, Card> toBeRevised;
    private final HashMap<String, Card> done;
    private final HashMap<String, User> members;

    public Project(String projectname, User creator, String chatAddress){
        //initialize private project lists
        this.chatAddress = chatAddress;
        this.name   = projectname;
        todo        = new HashMap<>();
        inProgress  = new HashMap<>();
        toBeRevised = new HashMap<>();
        done        = new HashMap<>();
        members     = new HashMap<>();
        cardLists = new HashMap<String, HashMap<String, Card>>(){
            {
                put(Const.TODO, todo);
                put(Const.INPROGRESS, inProgress);
                put(Const.TOBEREVISED, toBeRevised);
                put(Const.DONE, done);
            }
        };
        members.put(creator.getUsername(), creator);
    }

    //todo create CardNotMovingException
    public boolean moveCard(String name, String from, String to) throws CardMoveForbidden, CardNotFoundException {
        //card moves constraints check
        if(!cardExists(name))       throw new CardNotFoundException();
        if(from.equals(to))         throw new CardMoveForbidden("Card move not allowed.");
        if(from.equals(Const.DONE)) throw new CardMoveForbidden("Card move not allowed.");
        if(from.equals(Const.INPROGRESS) && to.equals(Const.TODO))  throw new CardMoveForbidden("Card move not allowed.");
        if(from.equals(Const.TOBEREVISED) && to.equals(Const.TODO)) throw new CardMoveForbidden("Card move not allowed.");
        if(from.equals(Const.TODO) && !to.equals(Const.INPROGRESS)) throw new CardMoveForbidden("Card move not allowed.");

        try {
            Card temp = cardLists.get(from).remove(name);
            cardLists.get(to).put(name, temp);
            temp.setStatus(to);
        }
        catch(Exception e){
                e.printStackTrace();
                return false;
        }
        return true;
    }

    private boolean cardExists(String cardname){
        return (todo.containsKey(cardname) || inProgress.containsKey(cardname)) ||
                (toBeRevised.containsKey(cardname) || done.containsKey(cardname));
    }

    public JsonObject getCardJson(String name) throws CardNotFoundException {
        Card card = getCard(name);
        JsonObject cardInfo = new JsonObject();
        cardInfo.addProperty("name", card.getName());
        cardInfo.addProperty("description", card.getDescription());
        cardInfo.addProperty("currentlist", card.getStatus());
        return cardInfo;
    }

    public Card getCard(String name) throws CardNotFoundException {
        if(todo.containsKey(name))          return todo.get(name);
        if(inProgress.containsKey(name))    return inProgress.get(name);
        if(toBeRevised.containsKey(name))   return toBeRevised.get(name);
        if(done.containsKey(name))          return done.get(name);

        throw new CardNotFoundException();
    }

    public void addMember(User newMember){
        members.putIfAbsent(newMember.getUsername(), newMember);
    }

    public boolean addCard(Card card) throws CardAlreadyExistsException {
        if(cardExists(card.getName())) throw new CardAlreadyExistsException();
        todo.put(card.getName(), card);
        return true;
    }

    public boolean addCard(String name, String desc) throws CardAlreadyExistsException {
        if(cardExists(name)) throw new CardAlreadyExistsException();
        todo.put(name, new Card(name, desc));
        return true;
    }

    public ArrayList<Card> getCards(){
        ArrayList<Card> allCards = new ArrayList<>();
        allCards.addAll(todo.values());
        allCards.addAll(inProgress.values());
        allCards.addAll(toBeRevised.values());
        allCards.addAll(done.values());
        return allCards;
    }

    public boolean isMember(String username){
        return members.containsKey(username);
    }

    public HashMap<String, User> getMembers(){
        return members;
    }

    public HashMap<String, Card> getTodoList(){
        return todo;
    }

    public HashMap<String, Card> getInProgressList(){
        return todo;
    }

    public HashMap<String, Card> getToBeRevisedList(){
        return todo;
    }

    public HashMap<String, Card> getDoneList(){
        return todo;
    }

    public String getName(){
        return name;
    }
}
