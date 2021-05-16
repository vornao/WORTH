package client;

import com.google.gson.*;
import exceptions.ProjectNotFoundException;
import interfaces.RMIClientInterface;
import interfaces.RMIServerInterface;
import server.utils.Const;
import server.utils.Printer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


//IMPORTANT: TO RUN WITH GSON LIBRARY RUN EXPORT
//export CLASSPATH=$CLASSPATH:/Users/vornao/Developer/university/WORTH/lib/gson-2.8.6.jar

public class Client {
    //todo parse command line arguments
    private final int    RMI_SERVER_PORT;
    private final int    PORT;
    private final String REGISTRY_NAME;
    private final String ADDRESS;

    private final SocketChannel socketChannel;
    private final ByteBuffer buffer;
    private final ConcurrentHashMap<String, Boolean> worthUsers = new ConcurrentHashMap<>();
    private String loginName = null;

    private RMIClient callbackAgent;
    private final ChatHelper chatHelper;
    Registry registry;
    RMIServerInterface remote;

    Gson gson = new Gson();

    //todo server side return different codes
    //todo create enum to convert server codes into fancy string
    private static final HashMap<Integer, String> SignupErrorMessages = new HashMap<Integer, String>() {
        {
            put(1, "> User created successfully");
            put(2, "> User creation failed, try again");
            put(0, "> Username already exists, try again");
        }
    };

    private static final HashMap<String, String> returnCodes = new HashMap<String, String>() {
        {
            put("200", Const.ANSI_GREEN +"> OK - 200 - Done" + Const.ANSI_RESET);
            put("201", Const.ANSI_GREEN + "> OK - 201 - Resource Created" + Const.ANSI_RESET);
            put("404", Const.ANSI_RED+ "> ERROR: 404 - Resource not found" + Const.ANSI_RESET);
            put("401", Const.ANSI_RED+ "> ERROR: 401 - Unauthorized" + Const.ANSI_RESET);
            put("403", Const.ANSI_RED+ "> ERROR: 403 - Forbidden" + Const.ANSI_RESET);
            put("409", Const.ANSI_RED+ "> ERROR: 409 - Resource already exists" + Const.ANSI_RESET);
        }
    };

    public Client(String address, int port, int rmiport, String registry, ChatHelper chatHelper) throws IOException, NotBoundException {
        this.ADDRESS = address;
        this.RMI_SERVER_PORT = rmiport;
        this.REGISTRY_NAME = registry;
        this.PORT = port;
        this.chatHelper = chatHelper;
        this.registry = LocateRegistry.getRegistry(address, RMI_SERVER_PORT);
        remote = (RMIServerInterface) this.registry.lookup(REGISTRY_NAME);

        socketChannel = SocketChannel.open();

        try {
            socketChannel.connect(new InetSocketAddress(ADDRESS, PORT));
        } catch (IOException e) {
            System.out.println(Const.ANSI_RED + "ERROR: Failed to connect to server." + Const.ANSI_RESET);
            System.exit(-1);
        }

        buffer = ByteBuffer.wrap(new byte[8192]);
        buffer.clear();
        Printer.println(String.format("> DEBUG: Connection established to WORTH server @ %s:%d", address, port), "yellow");
    }

    /**
     * connects to RMI server in order to let user sign up, if signup returns errors, an error message
     * is displayed to console.
     * If something goes wrong exception is catched and error message is printed as well.
     */
    public void signUp() {
        String username;
        String password;
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        try {
            System.out.print("> Insert new username: ");
            username = input.readLine();
            System.out.print("> Insert password: ");
            password = input.readLine();
            System.out.print("> Confirm password: ");

            if (!password.equals(input.readLine())) {
                Printer.println("< ERROR:Password not matching! Retry", "red");
                return;
            }

            int status = remote.signUp(username, password);
            Printer.println(SignupErrorMessages.get(status), "yellow");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** display help message */
    public void printHelp() {
        System.out.println("+---------------- Help Dialog --------------+");
        System.out.println("|    login        - log in to service       |");
        System.out.println("|    logout       - log out from service    |");
        System.out.println("|    signup       - register user utility   |");
        System.out.println("|    list-users   - list registered users   |");
        System.out.println("|    list-online  - list registered users   |");
        System.out.println("|    help         - show this message       |");
        System.out.println("|    quit         - logout and exit         |");
        System.out.println("+-------------------------------------------+");
    }

    /**
     * Log in method. First checks if user is logged in
     * if already logged in, prints red error message;
     * else will send a login json-formatted message to server, and waits for response
     * finally parses response to local object and displays response to user.
     * If something else goes wrong IOException or JSON format exception is catched and mesasge is displayed
     */

    public void login() {
        String username;
        String password;

        if(loginName != null){
            Printer.println("< ERROR: You are already logged in as: " + loginName, "red");
            return;
        }
        //read user input from keyboard
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        try {
            //ask user for login credentials
            System.out.print("> Username: ");
            username = input.readLine();
            System.out.print("> Password: ");
            password = input.readLine();

            JsonObject loginJson = new JsonObject();
            loginJson.addProperty("method", "login");
            loginJson.addProperty("username", username);
            loginJson.addProperty("password", password);

            //System.out.println("> DEBUG: " + loginJson);

            //write login request to server
            writeSocket(loginJson.toString().getBytes());

            //wait for response, check if read is successful
            String responseString = readSocket();

            //System.out.println(buffer);
            //System.out.println("> DEBUG: RECEIVED: " + responseString);

            //parsing response as generic JsonObject
            JsonObject response = gson.fromJson(responseString, JsonObject.class);

            //unnecessary, because we're checking socket read above, just a reminder.
            assert response != null;

            //check if login was successful and notify user.
            if (response.get("return-code").getAsString().equals("200")) {
                Printer.println("< Login Successful!", "green");
                this.loginName = username; //associating current login name to session for future requests
            } else {
                Printer.println("< Login failed: " + response.get("return-code").getAsString(), "red");
                return;
            }

            //parse users to local map
            JsonArray userlist = response.get("registered-users").getAsJsonArray();

            for (JsonElement j : userlist) {
                JsonObject user = (JsonObject) j;
                worthUsers.put(user.get("username").getAsString(), (user.get("status").getAsBoolean()));
            }

            //join to project chats
            for(JsonElement e : response.get("projects-list").getAsJsonArray()){
                JsonObject o = e.getAsJsonObject();
                chatHelper.joinGroup(o.get("chat-addr").getAsString(), o.get("name").getAsString());
            }

        } catch (IOException | JsonIOException e) {
            e.printStackTrace();
            Printer.println("< ERROR: Error sending message to server", "red");
        }

        //todo ask for project list in order to retreive chat info
        registerForCallback();
    }

    /**
     *
     * Log out method. First checks if user is logged in
     * If not logged in print red error message;
     * else will send a logout json-formatted message to server, and wait for response
     * finally displays response to user.
     * If something else goes wrong IOException or JSON format exception is catched and mesasge is displayed
     */

    public void logout() {

        if (loginName == null) {
            Printer.println("< ERROR: Log you in before logging out!", "red");
            return;
        }

        JsonObject request = new JsonObject();
        request.addProperty("method", "logout");
        request.addProperty("username", loginName);

        try {
            writeSocket(request.toString().getBytes());

            //read response from server
            String responseString = readSocket();
            JsonObject response = gson.fromJson(responseString, JsonObject.class);

            if (!response.get("return-code").getAsString().equals("200")) {
                Printer.println("< ERROR: Error logging out!", "red");
            }

            loginName = null;
            remote.unregisterForCallback(callbackAgent);

        } catch (IOException | JsonIOException e) {
            Printer.println("< Error sending message to server", "red");
            e.printStackTrace();
        }

    }

    /**
     *  Display registered users with nice looking ASCII table
     *  if user is not logged in prints red error message and returns.
     */

    public void createProject() throws IOException {
        if(loginName == null){
            Printer.println("< ERROR: you must log in before adding new project", "red");
            return;
        }

        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        String name;
        System.out.print("> Insert new project name: ");
        name = input.readLine();
        JsonObject request = new JsonObject();
        request.addProperty("username", loginName);
        request.addProperty("method", "create-project");
        request.addProperty("projectname", name);

        writeSocket(request.toString().getBytes());

        String returnCode = gson.fromJson(readSocket(), JsonObject.class).get("return-code").getAsString();
        System.out.println(returnCodes.get(returnCode));
    }

    public void listProjects(){
        if(loginName == null){
            Printer.println("< ERROR: you must log in before adding new project", "red");
            return;
        }

        JsonObject request = new JsonObject();
        request.addProperty("username", loginName);
        request.addProperty("method", "list-projects");

        writeSocket(request.toString().getBytes());
        String response = readSocket();

        JsonArray projects = gson.fromJson(response, JsonObject.class).get("projects").getAsJsonArray();

        String rowFormat = "| %-15s |%n";
        System.out.format("+-----------------+%n");
        System.out.format("| Projects        |%n");
        System.out.format("+-----------------+%n");

        for(JsonElement e : projects){
            JsonObject obj = e.getAsJsonObject();
            System.out.format(rowFormat, obj.get("name").getAsString());
        }

        System.out.format("+-----------------+%n");
    }

    public void listUsers(Boolean onlyOnlineUsers){

        if(loginName == null) {
            Printer.println("> ERROR: You must log in to see registered users.", "red");
            return;
        }

        //green online string, offline label will be printed with default terminal color.
        String online = Const.ANSI_GREEN + "online     " + Const.ANSI_RESET;
        String status;
        String rowFormat = "| %-15s | %-11s |%n";
        System.out.format("+-----------------+-------------+%n");
        System.out.format("| Username        | Status      |%n");
        System.out.format("+-----------------+-------------+%n");

        for (Map.Entry<String, Boolean> entry : worthUsers.entrySet()) {
            if (entry.getValue()) status = online;
            else status = "offline";
            if(onlyOnlineUsers) {
                if (status.equals(online)) System.out.format(rowFormat, entry.getKey(), status);

            }else System.out.format(rowFormat, entry.getKey(), status);

        }
        System.out.format("+-----------------+-------------+%n");
    }

    public void showMembers() throws IOException {
        if(loginName == null){
            Printer.println("< ERROR: you must log in before adding new users", "red");
            return;
        }
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        String projectname;
        System.out.print("> Insert project name: ");
        projectname = input.readLine();

        JsonObject request = new JsonObject();
        request.addProperty("username", loginName);
        request.addProperty("method", "show-members");
        request.addProperty("projectname", projectname);

        writeSocket(request.toString().getBytes());
        JsonObject response = gson.fromJson(readSocket(), JsonObject.class);

        String returnCode = response.get("return-code").getAsString();
        if(!returnCode.equals("200")){
            System.out.println(returnCodes.get(returnCode));
            return;
        }

        JsonArray projects = response.get("members").getAsJsonArray();
        String rowFormat = "| %-15s |%n";
        System.out.format("+-----------------+%n");
        System.out.format(rowFormat, projectname + " members");
        System.out.format("+-----------------+%n");

        for(JsonElement e : projects){
            System.out.format(rowFormat, e.getAsString());
        }

        System.out.format("+-----------------+%n");

    }

    public void addMember() throws IOException {
        if(loginName == null){
            Printer.println("< ERROR: you must log in before adding new users", "red");
            return;
        }
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        String name;
        String projectname;
        System.out.print("> Insert project name: ");
        projectname = input.readLine();
        System.out.print("> Insert new member username (user id): ");
        name = input.readLine();
        JsonObject request = new JsonObject();
        request.addProperty("method", "add-member");
        request.addProperty("username", loginName);
        request.addProperty("projectname", projectname);
        request.addProperty("new-member", name);
        writeSocket(request.toString().getBytes());

        String returnCode = gson.fromJson(readSocket(), JsonObject.class).get("return-code").getAsString();
        System.out.println(returnCodes.get(returnCode));

    }

    public void addCard() throws IOException {
        if(loginName == null){
            Printer.println("< ERROR: you must log in before adding new users", "red");
            return;
        }
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        String cardname;
        String cardDescr;
        String projectname;
        System.out.print("> Insert project name: ");
        projectname = input.readLine();
        System.out.print("> Insert new card name: ");
        cardname = input.readLine();
        System.out.print("> Provide a description for new card: ");
        cardDescr = input.readLine();

        JsonObject request = new JsonObject();
        request.addProperty("username", loginName);
        request.addProperty("method", "add-card");
        request.addProperty("projectname", projectname);
        request.addProperty("cardname", cardname);
        request.addProperty("cardesc", cardDescr);

        writeSocket(request.toString().getBytes());
        String returnCode = gson.fromJson(readSocket(), JsonObject.class).get("return-code").getAsString();
        System.out.println(returnCodes.get(returnCode));
    }

    public void showCard() throws IOException{
        if(loginName == null){
            Printer.println("< ERROR: you must log in before adding new users", "red");
            return;
        }
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        String name;
        String projectname;
        System.out.print("> Insert project name: ");
        projectname = input.readLine();
        System.out.print("> Insert card name: ");
        name = input.readLine();

        JsonObject request = new JsonObject();
        request.addProperty("method", "show-card");
        request.addProperty("username", loginName);
        request.addProperty("projectname", projectname);
        request.addProperty("cardname", name);

        writeSocket(request.toString().getBytes());
        JsonObject response = gson.fromJson(readSocket(), JsonObject.class);
        JsonObject cardJson = response.getAsJsonObject("card-info");
        String returnCode = response.get("return-code").getAsString();

        if(!returnCode.equals("200")) System.out.println(returnCodes.get(returnCode));
        else{
            Printer.println("> Card Name: " + cardJson.get("name"), "yellow");
            Printer.println("> Card Description: " + cardJson.get("description"), "yellow");
            Printer.println("> Task Status: " + cardJson.get("currentlist"), "yellow");
        }
    }

    public void listCards() throws IOException{
        if(loginName == null) {
            Printer.println("> ERROR: You must log in to see registered users.", "red");
            return;
        }
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        String projectname;
        System.out.print("> Insert project name: ");
        projectname = input.readLine();
        JsonObject request = new JsonObject();
        request.addProperty("username", loginName);
        request.addProperty("method", "list-cards");
        request.addProperty("projectname", projectname);

        writeSocket(request.toString().getBytes());
        JsonObject response = gson.fromJson(readSocket(), JsonObject.class);
        String statusCode = response.get("return-code").getAsString();

        if(!statusCode.equals("200")){
            System.out.println(returnCodes.get(statusCode));
            return;
        }

        JsonArray cards = response.get("card-list").getAsJsonArray();

        //green online string, offline label will be printed with default terminal color.
        String rowFormat = "| %-15s | %-11s |%n";
        System.out.format("+-----------------+-------------+%n");
        System.out.format("| Card Name       | List        |%n");
        System.out.format("+-----------------+-------------+%n");
        for(JsonElement e : cards){
            System.out.format(rowFormat,
                    e.getAsJsonObject().get("card-name").getAsString(),
                    e.getAsJsonObject().get("card-state").getAsString());
        }
        System.out.format("+-----------------+-------------+%n");
    }

    public void moveCard() throws IOException{
        if(loginName == null){
            Printer.println("< ERROR: you must log in before adding new users", "red");
            return;
        }
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        String name;
        String projectname;
        String from;
        String to;

        System.out.print("> Insert project name: ");
        projectname = input.readLine();
        System.out.print("> Insert card name: ");
        name = input.readLine();
        System.out.print("> Insert start list: ");
        from = input.readLine();
        System.out.print("> Insert destination: ");
        to = input.readLine();

        JsonObject request = new JsonObject();
        request.addProperty("method", "move-card");
        request.addProperty("username", loginName);
        request.addProperty("projectname", projectname);
        request.addProperty("cardname", name);
        request.addProperty("from", from);
        request.addProperty("to", to);

        writeSocket(request.toString().getBytes());
        String returnCode = gson.fromJson(readSocket(), JsonObject.class).get("return-code").getAsString();
        System.out.println(returnCodes.get(returnCode));

        if("200".equals(returnCode)){
            String notification = String.format("%s moved card %s from list \"%s\" to list \"%s\"", loginName, name, from, to);
            try {
                chatHelper.sendMessage(projectname, "Project " + projectname, notification);
            }catch (ProjectNotFoundException e){
                Printer.print("> ERROR: Failed to send message: project not found.", "red");
            }
        }
    }

    public void getCardHistory() throws IOException {
        if(loginName == null){
            Printer.println("< ERROR: you must log in before adding new users", "red");
            return;
        }
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        String name;
        String projectname;
        String from;
        String to;

        System.out.print("> Insert project name: ");
        projectname = input.readLine();
        System.out.print("> Insert card name: ");
        name = input.readLine();


        JsonObject request = new JsonObject();
        request.addProperty("method", "get-card-history");
        request.addProperty("username", loginName);
        request.addProperty("projectname", projectname);
        request.addProperty("cardname", name);

        writeSocket(request.toString().getBytes());

        JsonObject response = gson.fromJson(readSocket(), JsonObject.class);

        if(!response.get("return-code").getAsString().equals("200")){
            System.out.println(returnCodes.get(response.get("return-code").getAsString()));
            return;
        }


        JsonArray history = response.getAsJsonArray("card-history");

        String rowFormat = "| %-15s | %-11s | %-23s |%n";
        System.out.format("+-----------------+-------------+-------------------------+%n");
        System.out.format("| From            | To          | Date                    |%n");
        System.out.format("+-----------------+-------------+-------------------------+%n");

        for(JsonElement e : history){

            int timestamp = e.getAsJsonObject().get("date").getAsInt();
            String start = e.getAsJsonObject().get("from").getAsString();
            String end = e.getAsJsonObject().get("to").getAsString();
            String date = new java.text.SimpleDateFormat("MM-dd-yyyy HH:mm:ss")
                    .format(new java.util.Date (timestamp* 1000L));
            System.out.format(rowFormat, start, end, date);
        }
        System.out.format("+-----------------+-------------+-------------------------+%n");

    }

    /** logout and quit client */
    public void quit(){
        if(loginName != null) logout();
        System.exit(0);
    }

    public void clear(){
        System.out.println("\033[H\033[2J");
        System.out.flush();
    }

    public void sendChat() throws IOException {
        if(loginName == null){
            Printer.println("< ERROR: you must log in before reading chat", "red");
            return;
        }

        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        Printer.print("> Insert project chat name: ", "purple");
        String projectname = input.readLine();
        Printer.print("> Message body: ", "purple");
        String text = input.readLine();
        try {
            chatHelper.sendMessage(projectname, loginName, text);
        }catch (ProjectNotFoundException e){
            Printer.print("> ERROR: Failed to send message: project not found.", "red");
        }
    }

    public void readChat() throws IOException  {

        if(loginName == null){
            Printer.println("< ERROR: you must log in before reading chat", "red");
            return;
        }

        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        Printer.print("> Insert project chat name: ", "yellow");
        String projectname = input.readLine();
        List<String> messages = chatHelper.getProjectMessages(projectname);

        if(messages == null || messages.size() == 0){
            Printer.println("> No messages to show for " + projectname, "green");
            return;
        }

        //using an iterator to avoid ConcurrentModificationException for calling remove() while iterating on collection
        Iterator<String> iterator = messages.iterator();
        while(iterator.hasNext()) {
            Printer.println("- " + iterator.next(), "purple");
            iterator.remove();
        }
    }

    //todo server side delete files associated with project
    public void deleteProject() throws IOException {
        if(loginName == null){
            Printer.println("< ERROR: you must log in before adding new users", "red");
            return;
        }
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        String projectname;

        System.out.print("> Insert project name: ");
        projectname = input.readLine();

        JsonObject request = new JsonObject();
        request.addProperty("method", "delete-project");
        request.addProperty("username", loginName);
        request.addProperty("projectname", projectname);

        Printer.print("> Are you sure you want to delete the project? This action cannot be undone [yes/no]: ", "red");
        String ans;

        while(true) {
            ans = input.readLine();
            if ("yes".equals(ans)) {
                writeSocket(request.toString().getBytes());
                break;
            }
            else if ("no".equals(ans)) {
                Printer.println("< Operation cancelled.", "green");
                return;
            } else {
                Printer.print("> Please type \"yes\" or \"no\": ", "red");
            }
        }

        JsonObject response = gson.fromJson(readSocket(), JsonObject.class);
        System.out.println(returnCodes.get(response.get("return-code").getAsString()));

    }

    private void writeSocket(byte[] request){
        try {
            buffer.clear();
            buffer.put(request);
            buffer.flip();
            while (buffer.hasRemaining()) socketChannel.write(buffer);
            buffer.clear();
        }catch(IOException e){
            Printer.println("< Failed sending message to server. Try Again",  "red");
        }
    }

    private String readSocket(){
        try {
            if (socketChannel.read(buffer) < 0) {
                Printer.println("< ERROR: Server closed connection unexpectedly.", "red");
                System.exit(-1);
            }
            buffer.flip();
            String responseString;
            responseString = StandardCharsets.UTF_8.decode(buffer).toString();
            buffer.clear();
            return responseString;

        }catch(IOException e){
            Printer.println("< ERROR: Server closed connection unexpectedly.", "red");
        }
        return null;
    }

    private void registerForCallback(){
        callbackAgent = new RMIClient(worthUsers, chatHelper, this.loginName);

        try {
            RMIClientInterface stub = (RMIClientInterface) UnicastRemoteObject.exportObject(callbackAgent, 0);
            remote.registerForCallback(stub);

        }catch (RemoteException e){
            e.printStackTrace();
            Printer.println("> ERROR: (RMI) cannot subscribe to server callback", "red");
        }
    }

}