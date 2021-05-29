package server;

import com.google.gson.*;
import exceptions.CardAlreadyExistsException;
import exceptions.CardMoveForbidden;
import exceptions.CardNotFoundException;
import interfaces.RMIServerInterface;
import shared.Card;
import shared.CardEvent;
import shared.Project;
import server.utils.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private final String PROJECTDIR;
    private static final int CHANNEL_BUFFER_SIZE = 8192;
    private final int SOCKETPORT;
    private final int RMIPORT;
    private RMIServer rmiServer = null;
    private final String ADDRESS;
    private final String REGISTRY_NAME;

    private final ConcurrentHashMap<String, User> registeredUsers;
    private final ConcurrentHashMap<String, Project> projects;

    private static Selector selector;
    private final  FileHandler fileHandler;

    public Server(String address, int channelport, String registryname, int rmiport, String workingdir) throws IOException {
        this.SOCKETPORT = channelport;
        this.RMIPORT = rmiport;
        this.ADDRESS = address;
        this.PROJECTDIR = workingdir;
        REGISTRY_NAME = registryname;

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

        fileHandler = new FileHandler(PROJECTDIR);
        registeredUsers = fileHandler.loadUsers();
        projects = fileHandler.loadProjects();
    }

    public void start(){
        startRMI();
        Printer.println(String.format(
                "> INFO: Server started: \n\tTCP: %s:%d\n\tRMI: %s:%d @ %s\n\tProjectDirectory: %s\n",
                ADDRESS, SOCKETPORT, ADDRESS, RMIPORT, REGISTRY_NAME, PROJECTDIR) ,
                "green");

        while(true){
            try{
                if(selector.select() == 0) continue;
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keysIterator = selectedKeys.iterator();

                while(keysIterator.hasNext()){
                    SelectionKey sk = keysIterator.next();
                    keysIterator.remove();
                    if (sk.isAcceptable()) accept(selector, sk);
                    else if (sk.isReadable()) readSocketChannel(selector, sk);
                }

            }catch (IOException e){
                e.printStackTrace();
            }
        }

    }

    private void startRMI(){
        try {
            rmiServer = new RMIServer(registeredUsers, PROJECTDIR);
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

    private void accept(Selector s, SelectionKey sk) throws IOException {

        //setting things ready for SocketChannel to receive data
        Printer.println("> INFO: Client connected!", "green");
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
        if(socketChannel.read(buffer) < 0) {
            for(User u: registeredUsers.values()){
                //if user was logged in, log out.
                if(u.getSessionPort() == socketChannel.getRemoteAddress().hashCode()){
                    u.setStatus(false);
                    u.setSessionPort(-1);
                }
            }
            Printer.println("> INFO: CLIENT DISCONNECTED.", "green");
            socketChannel.close();
            return;
        }

        buffer.flip();
        message = StandardCharsets.UTF_8.decode(buffer).toString();

        Printer.println("> DEBUG: RECEIVED:" + message, "yellow");
        buffer.clear();

        //parse message as JSON object to handle it easily
        Gson gson = new Gson();
        JsonObject request = null;
        JsonObject response =  null;

        try{
            request = gson.fromJson(message, JsonObject.class);
            //fetching requested method and computing response
            try {
                assert request != null;
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
                response = new JsonObject();
                response.addProperty("return-code", 500);
            }
        }catch (JsonSyntaxException e){
            response = new JsonObject();
            response.addProperty("return-code", 400);
        }

        assert response != null;
        buffer.put(response.toString().getBytes(StandardCharsets.UTF_8));
        buffer.flip(); //ready to be read again from zero to limit

        while(buffer.hasRemaining()) socketChannel.write(buffer);
        buffer.flip();
        Printer.println("> DEBUG: buffer content" + StandardCharsets.UTF_8.decode(buffer), "yellow");
        buffer.clear(); //limit = pos = 0
        Printer.println("> DEBUG: " + response, "yellow");
        socketChannel.register(selector, SelectionKey.OP_READ, buffer); //channel ready for new client requests.
    }

    /** login method, uses RMI for user callback. See RMIServer.java for actual callback implementation. */
    private JsonObject login(String username, String password, int socketHash) throws RemoteException {
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
        JsonObject projects = listProjects(username);
        response.add("registered-users", jsonArray);
        response.add("projects-list", projects.get("projects"));
        return response;
    }

    /** performs user logout. Returning jsonObject  in case of failure or success either, just changes error code.*/
    private JsonObject logout(String username) throws RemoteException {
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


    /**
     * @param projectname -> new project name.
     * @param username -> user requesting for new project.
     * @return new JsonObject with return code.
     *
     * Add project Projectname to project list. Different return codes if projectname already exists
     * or user has not enough privileges to perform action.
     */
    private JsonObject addProject(String projectname, String username) {
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
        String multicastAddress = MulticastBaker.getNewMulticastAddress();


        if(multicastAddress == null){
            response.addProperty("return-code", 500);
            return response;
        }

        Project project = new Project(projectname, u, multicastAddress, fileHandler);
        projects.putIfAbsent(projectname, project);
        try {
            rmiServer.updateChat(username, projectname, project.getChatAddress());
            fileHandler.saveProject(project);
        }catch (IOException e){
            response.addProperty("return-code", 500);
            return response;
        }

        response.addProperty("return-code", 201);
        return response;
    }

    /**
     * @param username -> user requesting for action.
     * @param cardname -> new card name.
     * @param desc -> new card description.
     * @param projectname -> existing project in which card will be created.
     * @return new JsonObject with return code.
     *
     * Simply add new card to an existing project. If project does not exists, 401 error code is added to response
     * if card already exists, code 409 is returned to client.
     */

    private JsonObject addCard(String username, String cardname, String desc, String projectname){
        JsonObject response = new JsonObject();
        User u = registeredUsers.get(username);
        Project p = projects.get(projectname);

        if(u == null || !isLoggedIn(username) || p == null || !p.isMember(username)) {
            response.addProperty("return-code", 401);
            return response;
        }

        try {
            Card c = new Card(cardname, desc);
            p.addCard(c);

        }catch (CardAlreadyExistsException e){
            response.addProperty("return-code", 409);
            return response;
        }

        response.addProperty("return-code", 201);
        return response;
    }

    /**
     *
     * @param username user demanding for action
     * @param cardname card to move
     * @param from starting list
     * @param to destination list
     * @param projectname project in which card has to be moved.
     * @return JsonObject with 200 code in case of success, else 401 if unauthorized or not found, 405 if
     * request does not meet card move constraints.
     */

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

    /**
     *
     * @param username username demanding for action.
     * @param projectname project name for card list.
     * @return JsonObject with 200 success code and JsonArray with card list.
     */

    private JsonObject listCards(String username, String projectname){
        JsonObject response = new JsonObject();
        Project p = projects.get(projectname);

        //permissions checks
        if (!isLoggedIn(username) || p == null || !p.isMember(username)) {
            response.addProperty("return-code", 401);
            return response;
        }

        try {

            ArrayList<Card> cardList = p.getCards();
            JsonArray cardsJson = new JsonArray();
            for (Card c : cardList) {
                JsonObject json = new JsonObject();
                json.addProperty("card-name", c.getName());
                json.addProperty("card-state", c.getStatus());
                json.addProperty("card-desc", c.getDescription());
                cardsJson.add(json);
            }

            response.addProperty("return-code", 200);
            response.add("card-list", cardsJson);
            return response;
        }catch (Exception e){
            response.addProperty("return-code", 500);
            return response;
        }
    }

    /**
     *
     * @param username user demanding for action.
     * @param projectname project containing card.
     * @param cardname card to show.
     * @return JsonObject with card properties and success code 200, or error codes in case of card not found.
     */

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
            response.addProperty("return-code", 404);
        }
        return response;

    }

    /**
     *
     * @param username user demanding for action.
     * @param projectname project containing card.
     * @param cardname card to show.
     * @return JsonObject with JsonArray containing card history.
     */

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

    /**
     * @param username user demanding for action.
     * @return JsonObject with JsonArray containing actual user's projects.
     */

    private JsonObject listProjects(String username) {
        JsonObject response = new JsonObject();

        if (!isLoggedIn(username)) {
            response.addProperty("return-code", 401);
            return response;
        }

        JsonArray jsonArray = new JsonArray();
        for (Project p : projects.values()) {
            JsonObject jsonObject = new JsonObject();
            if (p.isMember(username)){
                jsonObject.addProperty("name", p.getName());
                jsonObject.addProperty("chat-addr", p.getChatAddress());
                jsonArray.add(jsonObject);
            }
        }

        response.add("projects", jsonArray);
        return response;
    }


    /**
     *
     * @param username user demanding for action.
     * @param projectname project to delete
     * @return JsonObject with 200 code in case of success, other error codes in case of failure.
     */

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
        projects.remove(p.getName());
        MulticastBaker.releaseAddress(p.getChatAddress());
        rmiServer.leaveGroup(p);
        fileHandler.deleteProject(projectname);
        response.addProperty("return-code", 200);
        return response;
    }


    /**
     *
     * @param username user demanding for action.
     * @param newMember new member name.
     * @return JsonObject with 201 code if success.
     */
    private JsonObject addMember(String username, String newMember, String projectname) {
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

        try {
            rmiServer.updateChat(newMember, projectname, p.getChatAddress());
            fileHandler.saveProject(p);
        }catch (IOException e){
            response.addProperty("return-code", 500);
            return response;
        }
        response.addProperty("return-code", 201);
        return response;
    }


    /**
     *
     * @param username user demanding for action.
     * @param projectname project to show
     * @return JsonObject containing JsonArray with members list.
     */
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
        for(String s : p.getMembers()){
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
