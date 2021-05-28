package shared;

import com.google.gson.JsonObject;
import exceptions.CardAlreadyExistsException;
import exceptions.CardMoveForbidden;
import exceptions.CardNotFoundException;
import server.User;
import server.utils.Const;
import server.utils.FileHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class Project {

    private final String name;
    private final FileHandler fileHandler;

    //used for querying all list by name
    private final HashMap<String, HashMap<String, Card>> cardLists;
    private final HashMap<String, Card> todo;
    private final HashMap<String, Card> inProgress;
    private final HashMap<String, Card> toBeRevised;
    private final HashMap<String, Card> done;
    private final ArrayList<String> members;
    private String chatAddress;


    public Project(String projectname, User creator, String chatAddress, FileHandler fileHandler){
        //initialize private project lists
        this.chatAddress = chatAddress;
        this.name   = projectname;
        this.fileHandler = fileHandler;
        todo        = new HashMap<>();
        inProgress  = new HashMap<>();
        toBeRevised = new HashMap<>();
        done        = new HashMap<>();
        members     = new ArrayList<>();
        cardLists = new HashMap<String, HashMap<String, Card>>(){
            {
                put(Const.TODO, todo);
                put(Const.INPROGRESS, inProgress);
                put(Const.TOBEREVISED, toBeRevised);
                put(Const.DONE, done);
            }
        };
        members.add(creator.getUsername());
    }

    public Project(String projectname, ArrayList<String> members, String chatAddress, FileHandler fileHandler){
        //initialize private project lists
        this.chatAddress = chatAddress;
        this.name   = projectname;
        this.fileHandler = fileHandler;
        todo        = new HashMap<>();
        inProgress  = new HashMap<>();
        toBeRevised = new HashMap<>();
        done        = new HashMap<>();
        cardLists = new HashMap<String, HashMap<String, Card>>(){
            {
                put(Const.TODO, todo);
                put(Const.INPROGRESS, inProgress);
                put(Const.TOBEREVISED, toBeRevised);
                put(Const.DONE, done);
            }
        };
        this.members = members;
    }

    public void moveCard(String name, String from, String to) throws CardMoveForbidden, CardNotFoundException {
        //card moves constraints check
        if(!cardExists(name))       throw new CardNotFoundException();
        if(from.equals(to))         throw new CardMoveForbidden("Card move not allowed.");
        if(from.equals(Const.DONE)) throw new CardMoveForbidden("Card move not allowed.");
        if(from.equals(Const.INPROGRESS) && to.equals(Const.TODO))  throw new CardMoveForbidden("Card move not allowed.");
        if(from.equals(Const.TOBEREVISED) && to.equals(Const.TODO)) throw new CardMoveForbidden("Card move not allowed.");
        if(from.equals(Const.TODO) && !to.equals(Const.INPROGRESS)) throw new CardMoveForbidden("Card move not allowed.");

        try {
            Card temp = cardLists.get(from).remove(name);
            cardLists.get(to).putIfAbsent(name, temp);
            temp.setStatus(to);
            fileHandler.saveCard(this.name, temp);
        }
        catch(Exception e){
                e.printStackTrace();
        }
    }

    private boolean cardExists(String cardname){
        return (todo.containsKey(cardname) || inProgress.containsKey(cardname)) ||
                (toBeRevised.containsKey(cardname) || done.containsKey(cardname));
    }

    public void setChatAddress(String addr){
        this.chatAddress = addr;
    }

    public String getChatAddress(){
        return chatAddress;
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
        if (!members.contains(newMember.getUsername()))
        members.add(newMember.getUsername());
    }

    public void addCard(Card card) throws CardAlreadyExistsException {
        if(cardExists(card.getName())) throw new CardAlreadyExistsException();
        todo.putIfAbsent(card.getName(), card);
        fileHandler.saveCard(this.name, card);
    }

    public void addCard(String name, String desc) throws CardAlreadyExistsException {
        if(cardExists(name)) throw new CardAlreadyExistsException();
        Card c = new Card(name, desc);
        todo.putIfAbsent(name, c);
        fileHandler.saveCard(this.name, c);
    }

    public void restoreCards(ArrayList<Card> cards){
        for(Card c : cards){
            cardLists.get(c.getStatus()).putIfAbsent(c.getName(), c);
        }
    }

    public boolean isMember(String username){
        return members.contains(username);
    }

    public ArrayList<String> getMembers(){
        return members;
    }

    public ArrayList<Card> getCards(){
        ArrayList<Card> allCards = new ArrayList<>();
        allCards.addAll(todo.values());
        allCards.addAll(inProgress.values());
        allCards.addAll(toBeRevised.values());
        allCards.addAll(done.values());
        return allCards;
    }

    public boolean isAllDone(){
        return (todo.isEmpty() && inProgress.isEmpty()) && toBeRevised.isEmpty();
    }

    public String getName(){
        return name;
    }
}
