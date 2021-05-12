package server;

import com.google.gson.*;
import exceptions.CardAlreadyExistsException;
import exceptions.CardMoveForbidden;
import exceptions.CardNotFoundException;
import interfaces.RMIServerInterface;
import shared.Card;
import shared.CardEvent;
import shared.Project;
import utils.PasswordHandler;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final int CHANNEL_BUFFER_SIZE = 8192;
    private final int SOCKETPORT;
    private final int RMIPORT;
    private RMIServer rmiServer = null;
    private final String ADDRESS;
    private static final String REGISTRY_NAME =  "SIGN-UP-SERVER";

    private final ConcurrentHashMap<String, User> registeredUsers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Project> projects = new ConcurrentHashMap<>();

    private static Selector selector;


    public Server(String address, int channelport, int rmiport){
        this.SOCKETPORT = channelport;
        this.RMIPORT = rmiport;
        this.ADDRESS = address;
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(SOCKETPORT));
            serverSocketChannel.configureBlocking(false);
            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        }catch(IOException e){
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void start(){
        System.out.println("---- Welcome to WORTH server (Progetto Reti Laboratorio a.a. 2020/20201) ----");
        startRMI();

        while(true){
            try{
                if(selector.select() == 0) continue;
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keysIterator = selectedKeys.iterator();

                while(keysIterator.hasNext()){
                    SelectionKey sk = keysIterator.next();
                    keysIterator.remove();
                    if (sk.isAcceptable()) registerRead(selector, sk);
                    else if (sk.isReadable()) readSocketChannel(selector, sk);
                }

            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private void startRMI(){
        try {
            rmiServer = new RMIServer(registeredUsers);
            RMIServerInterface stub = (RMIServerInterface) UnicastRemoteObject.exportObject(rmiServer, RMIPORT);
            LocateRegistry.createRegistry(RMIPORT);
            //todo try to bind to non-localhost address!
            Registry registry = LocateRegistry.getRegistry(ADDRESS, RMIPORT);
            registry.rebind(REGISTRY_NAME, stub);

        }catch (RemoteException e){
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private void registerRead(Selector s, SelectionKey sk) throws IOException {

        //setting things ready for SocketChannel to receive data
        System.out.println("Client connected!");
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) sk.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);

        //now link a Buffer to store channel's data, (8KB for connection)
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[CHANNEL_BUFFER_SIZE]);

        //socket is now ready for reading up to 8kb of incoming data from client.
        socketChannel.register(s, SelectionKey.OP_READ, byteBuffer);

    }

    private void readSocketChannel(Selector selector, SelectionKey selectionKey) throws IOException {
        String message;
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        socketChannel.configureBlocking(false);
        ByteBuffer buffer = (ByteBuffer) selectionKey.attachment();

        //client disconnected
        if( socketChannel.read(buffer) < 0) {

            for(User u: registeredUsers.values()){
                //if user was logged in, log out.
                if(u.getSessionPort() == socketChannel.getRemoteAddress().hashCode()){
                    u.setStatus(false);
                    u.setSessionPort(-1);
                }
            }
            System.out.println("CLIENT DISCONNECTED.");
            socketChannel.close();
            return;
        }

        buffer.flip();
        message = StandardCharsets.UTF_8.decode(buffer).toString();

        System.out.println("> DEBUG: RECEIVED:" + message);
        buffer.clear();

        //parse message as JSON object to handle it easily
        Gson gson = new Gson();

        JsonObject request = gson.fromJson(message, JsonObject.class);
        JsonObject response =  null;

        //fetching requested method and computing response
        assert request != null;
        try {
            switch (request.get("method").getAsString()) {
                case "login":
                    response = login(
                            request.get("username").getAsString(),
                            request.get("password").getAsString(),
                            socketChannel.getRemoteAddress().hashCode());
                    break;
                case "logout":
                    response = logout(request.get("username").getAsString());
                    break;
                case "create-project":
                    response = addProject(
                            request.get("projectname").getAsString(),
                            request.get("username").getAsString());
                    break;
                case "list-projects":
                    response = listProjects(request.get("username").getAsString());
                    break;
                case "add-member":
                    response = addMember(request.get("username").getAsString(),
                            request.get("new-member").getAsString(),
                            request.get("projectname").getAsString());
                    break;
                case "show-members":
                    response = showMembers(
                            request.get("username").getAsString(),
                            request.get("projectname").getAsString());
                    break;
                case "add-card":
                    response = addCard(
                            request.get("username").getAsString(),
                            request.get("cardname").getAsString(),
                            request.get("cardesc").getAsString(),
                            request.get("projectname").getAsString());
                    break;
                case "show-card":
                    response = showCard(
                            request.get("username").getAsString(),
                            request.get("projectname").getAsString(),
                            request.get("cardname").getAsString());
                    break;
                case "move-card":
                    response = moveCard(
                            request.get("username").getAsString(),
                            request.get("cardname").getAsString(),
                            request.get("from").getAsString(),
                            request.get("to").getAsString(),
                            request.get("projectname").getAsString());
                    break;
                case "list-cards":
                    response = listCards(
                            request.get("username").getAsString(),
                            request.get("projectname").getAsString());
                    break;
                case "get-card-history":
                    response = getCardHistory(
                            request.get("username").getAsString(),
                            request.get("projectname").getAsString(),
                            request.get("cardname").getAsString());
                    break;
                case "delete-project":
                    response = deleteProject(
                            request.get("username").getAsString(),
                            request.get("projectname").getAsString());
                    break;
                default:
                    break;
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        assert response != null;
        buffer.put(response.toString().getBytes(StandardCharsets.UTF_8));
        buffer.flip(); //ready to be read again from zero to limit

        while(buffer.hasRemaining()) socketChannel.write(buffer);
        buffer.flip();
        System.out.println("> DEBUG: buffer content" + StandardCharsets.UTF_8.decode(buffer));
        buffer.clear(); //limit = pos = 0
        System.out.println("> DEBUG: " + response);
        socketChannel.register(selector, SelectionKey.OP_READ, buffer); //channel ready for new client requests.
    }

    //todo write nice user permissions check

    private JsonObject login(String username, String password, int socketHash){
        JsonObject response = new JsonObject();
        User u =  registeredUsers.get(username);

        //user not found or auth failed
        if (u == null || u.getStatus() || !PasswordHandler.authenticate(password, u.getPassword(), u.getSalt())) {
            //401 - unauthorized - http like
            response.addProperty("return-code", 401);
            return response;
        }

        //auth ok
        registeredUsers.get(username).setStatus(true);    //set user state to online
        registeredUsers.get(username).setSessionPort(socketHash);
        rmiServer.updateUsers(username, true);

        response.addProperty("return-code", 200); //send 200 OK code

        //fetch registered user and send status.
        JsonArray jsonArray = new JsonArray();
        for(User t : registeredUsers.values()){
            JsonObject userJson = new JsonObject();
            userJson.addProperty("username", t.getUsername());
            userJson.addProperty("status", t.getStatus());
            jsonArray.add(userJson);
        }
        response.add("registered-users", jsonArray);
        return response;
    }

    private JsonObject logout(String username){
        JsonObject response = new JsonObject();

        if(!registeredUsers.get(username).getStatus()){
            response.addProperty("return-code", 300);
            return response;
        }
        registeredUsers.get(username).setStatus(false);
        registeredUsers.get(username).setSessionPort(-1);
        rmiServer.updateUsers(username, false);
        response.addProperty("return-code", 200);
        return response;
    }

    private JsonObject addProject(String projectname, String username){
        JsonObject response = new JsonObject();
        User u = registeredUsers.get(username);

        if(!isLoggedIn(username)) {
            response.addProperty("return-code", 401);
            return response;
        }
        if(projects.containsKey(projectname)){
            response.addProperty("return-code", 409);
            return response;
        }
        projects.put(projectname, new Project(projectname, u, null));

        response.addProperty("return-code", 201);
        return response;
    }

    private JsonObject addCard(String username, String cardname, String desc, String projectname){
        JsonObject response = new JsonObject();
        User u = registeredUsers.get(username);
        Project p = projects.get(projectname);

        if(u == null || !isLoggedIn(username) || p == null || !p.isMember(username)) {
            response.addProperty("return-code", 401);
            return response;
        }

        try {
            p.addCard(new Card(cardname, desc));
        }catch (CardAlreadyExistsException e){
            response.addProperty("return-code", 409);
            return response;
        }

        response.addProperty("return-code", 201);
        return response;
    }

    private JsonObject moveCard(String username, String cardname, String from, String to, String projectname){
        //user is logged in, member and card exists in project.
        JsonObject response = new JsonObject();
        Project p = projects.get(projectname);

        if(!isLoggedIn(username) || p == null || !p.isMember(username) ){
            response.addProperty("return-code", 401);
            return response;
        }

        try{
            p.moveCard(cardname, from, to);
        }catch (CardNotFoundException | CardMoveForbidden e){
            response.addProperty("return-code", 405);
            return response;
        }

        response.addProperty("return-code", 200);
        return response;
    }

    private JsonObject listCards(String username, String projectname){
        JsonObject response = new JsonObject();
        Project p = projects.get(projectname);

        //permissions checks
        if(!isLoggedIn(username) || p == null || !p.isMember(username) ) {
            response.addProperty("return-code", 401);
            return response;
        }

        ArrayList<Card> cardList = p.getCards();
        JsonArray cardsJson = new JsonArray();
        for(Card c : cardList){
            JsonObject json = new JsonObject();
            json.addProperty("card-name", c.getName());
            json.addProperty("card-state", c.getStatus());
            cardsJson.add(json);
        }

        response.addProperty("return-code", 200);
        response.add("card-list", cardsJson);
        return response;
    }

    private JsonObject showCard(String username, String projectname, String cardname){
        JsonObject response = new JsonObject();
        Project p = projects.get(projectname);

        //permissions checks
        if(!isLoggedIn(username) || p == null || !p.isMember(username) ) {
            response.addProperty("return-code", 401);
            return response;
        }
        try {
            response.addProperty("return-code", 200);
            response.add("card-info", p.getCardJson(cardname));
        }catch (CardNotFoundException e){
            response.addProperty("return-code", 401);
        }
        return response;

    }

    private JsonObject getCardHistory(String username, String projectname, String cardname){
        JsonObject response = new JsonObject();
        Project p = projects.get(projectname);

        if(!isLoggedIn(username) || p == null || !p.isMember(username) ){
            response.addProperty("return-code", 401);
            return response;
        }

        try{
            JsonArray history =  new JsonArray();
            ArrayList<CardEvent> cardHistory = p.getCard(cardname).getCardHistory();
            for(CardEvent e : cardHistory){
                JsonObject event = new JsonObject();
                event.addProperty("date", e.getDate());
                event.addProperty("from", e.getFrom());
                event.addProperty("to",   e.getTo());
                history.add(event);
            }
            response.add("card-history", history);
            response.addProperty("return-code", 200);
            return response;

        }catch (CardNotFoundException e){
            response.addProperty("return-code", 404);
            return response;
        }
    }

    private JsonObject listProjects(String username) {
        JsonObject response = new JsonObject();

        if (!isLoggedIn(username)) {
            response.addProperty("return-code", 401);
            return response;
        }

        JsonArray jsonArray = new JsonArray();
        for (Project p : projects.values()) {
            if (p.isMember(username)) jsonArray.add(p.getName());
        }

        response.add("projects", jsonArray);
        return response;
    }

    private JsonObject deleteProject(String username, String projectname){
        JsonObject response = new JsonObject();
        Project p = projects.get(projectname);

        if(!isLoggedIn(username) || p == null || !p.isMember(username) ){
            response.addProperty("return-code", 401);
            return response;
        }

        if(!p.isAllDone()){
            response.addProperty("return-code", 403);
            return response;
        }

        projects.remove(p.getName(), p);
        response.addProperty("return-code", 200);
        return response;
    }

    private JsonObject addMember(String username, String newMember, String projectname){
        JsonObject response = new JsonObject();

        //check if current user is logged in
        if (!isLoggedIn(username)) {
            response.addProperty("return-code", 401);
            return response;
        }

        //check if new user is registered
        User u = registeredUsers.get(newMember);
        Project p = projects.get(projectname);
        if(u == null || p == null){
            response.addProperty("return-code", 404);
            return response;
        }

        //check if current user is an actual member of the project
        if(!p.isMember(username)){
            response.addProperty("return-code", 401);
            return response;
        }

        p.addMember(u);
        response.addProperty("return-code", 201);
        return response;
    }

    private JsonObject showMembers(String username, String projectname){
        Project p = projects.get(projectname);
        JsonObject response = new JsonObject();

        //check if user is logged in and exists
        if (!isLoggedIn(username)) {
            response.addProperty("return-code", 401);
            return response;
        }

        //check if project exists
        if(p == null){
            response.addProperty("return-code", 404);
            return response;
        }

        //check if current user is an actual member of the project
        if(!p.isMember(username)){
            response.addProperty("return-code", 401);
            return response;
        }

        JsonArray members = new JsonArray();
        for(String s : p.getMembers().keySet()){
            members.add(s);
        }

        response.addProperty("return-code", 200);
        response.add("members", members);
        return response;
    }

    /**
     * Chechs whether user is online.
     * @param username username associated to a particular user
     * @return false if username not found or not logged in;
     *         true if username found and logged in == true
     */
    private boolean isLoggedIn(String username){
        User u = registeredUsers.get(username);
        return u != null && u.getStatus();
    }
}
