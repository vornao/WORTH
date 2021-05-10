package client;

import com.google.gson.*;
import interfaces.RMIClientInterface;
import interfaces.RMIServerInterface;
import utils.TermColors;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


//IMPORTANT: TO RUN WITH GSON LIBRARY RUN EXPORT
//export CLASSPATH=$CLASSPATH:/Users/vornao/Developer/university/WORTH/lib/gson-2.8.6.jar

public class Client {
    //todo parse command line arguments
    private final int RMI_SERVER_PORT;
    private final int PORT;
    private final String REGISTRY_NAME = "SIGN-UP-SERVER";
    private final String ADDRESS;

    private final SocketChannel socketChannel;
    private final ByteBuffer buffer;
    private final ConcurrentHashMap<String, Boolean> worthUsers = new ConcurrentHashMap<>();
    private String loginName = null;

    private final RMIClient callbackAgent;
    Registry registry;
    RMIServerInterface remote;

    Gson gson = new Gson();

    //todo server side return different codes
    private static final HashMap<Integer, String> SignupErrorMessages = new HashMap<Integer, String>() {
        {
            put(1, "> User created successfully");
            put(2, "> User creation failed, try again");
            put(0, "> Username already exists, try again");
        }
    };

    public Client(String address, int port, int rmiport) throws IOException, NotBoundException {
        this.ADDRESS = address;
        this.RMI_SERVER_PORT = rmiport;
        this.PORT = port;

        registry = LocateRegistry.getRegistry("localhost", RMI_SERVER_PORT);
        remote = (RMIServerInterface) registry.lookup(REGISTRY_NAME);
        callbackAgent = new RMIClient(worthUsers);

        socketChannel = SocketChannel.open();

        try {
            socketChannel.connect(new InetSocketAddress(ADDRESS, PORT));
        } catch (IOException e) {
            System.out.println(TermColors.ANSI_RED + "ERROR: Failed to connect to server." + TermColors.ANSI_RESET);
            System.exit(-1);
        }

        buffer = ByteBuffer.wrap(new byte[8192]);
        buffer.clear();
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
                print("< ERROR:Password not matching! Retry", "red");
            }

            int status = remote.signUp(username, password);
            print(SignupErrorMessages.get(status), "yellow");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** display help message */
    public void printHelp() {
        System.out.println("+--------------- Help Dialog --------------+");
        System.out.println("|    login       - log in to service       |");
        System.out.println("|    logout      - log out from service    |");
        System.out.println("|    signup      - register user utility   |");
        System.out.println("|    listUsers   - list registered users   |");
        System.out.println("|    help        - show this message       |");
        System.out.println("|    quit        - logout and exit         |");
        System.out.println("+------------------------------------------+");
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
            print("< ERROR: You are already logged in as: " + loginName, "red");
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
            buffer.put(loginJson.toString().getBytes());
            buffer.flip();
            while (buffer.hasRemaining()) socketChannel.write(buffer);
            buffer.clear();

            //wait for response, check if read is successful
            if(socketChannel.read(buffer) < 0){
                print("< ERROR: Server closed connection unexpectedly.", "red");
                System.exit(-1);
            }

            buffer.flip();
            String responseString;
            responseString = StandardCharsets.UTF_8.decode(buffer).toString();
            buffer.clear();

            //System.out.println(buffer);
            //System.out.println("> DEBUG: RECEIVED: " + responseString);

            //parsing response as generic JsonObject
            JsonObject response = gson.fromJson(responseString, JsonObject.class);

            //unnecessary, because we're checking socket read above, just a reminder.
            assert response != null;

            //check if login was successful and notify user.
            if (response.get("return-code").getAsString().equals("200")) {
                print("< Login Successful!", "green");
                this.loginName = username; //associating current login name to session for future requests
            } else {
                print("< Login failed: " + response.get("return-code").getAsString(), "red");
                return;
            }

            //parse users to local map
            JsonArray userlist = response.get("registered-users").getAsJsonArray();

            for (JsonElement j : userlist) {
                JsonObject user = (JsonObject) j;
                worthUsers.put(user.get("username").getAsString(), (user.get("status").getAsBoolean()));
            }
        } catch (IOException | JsonIOException e) {
            e.printStackTrace();
            print("< ERROR: Error sending message to server", "red");
        }
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
            print("< ERROR: Log you in before logging out!", "red");
            return;
        }

        JsonObject request = new JsonObject();
        request.addProperty("method", "logout");
        request.addProperty("username", loginName);

        try {
            buffer.clear();
            buffer.put(request.toString().getBytes());
            buffer.flip();
            while (buffer.hasRemaining()) socketChannel.write(buffer);
            buffer.clear();

            //read response from server
            socketChannel.read(buffer);
            buffer.flip();
            JsonObject response = gson.fromJson(StandardCharsets.UTF_8.decode(buffer).toString(), JsonObject.class);
            buffer.clear();

            if (!response.get("return-code").getAsString().equals("200")) {
                print("< ERROR: Error logging out!", "red");
            }

            loginName = null;
            remote.unregisterForCallback(callbackAgent);

        } catch (IOException | JsonIOException e) {
            print("< Error sending message to server", "red");
            e.printStackTrace();
        }

    }

    /**
     *  Display registered users with nice looking ASCII table
     *  if user is not logged in prints red error message and returns.
     */

    public void listUsers(){

        if(loginName == null) {
            print("> ERROR: You must log in to see registered users.", "red");
            return;
        }

        //green online string, offline label will be printed with default terminal color.
        String online = TermColors.ANSI_GREEN + "online     " + TermColors.ANSI_RESET;
        String status;
        String rowFormat = "| %-15s | %-11s |%n";
        System.out.format("+-----------------+-------------+%n");
        System.out.format("| Username        | Status      |%n");
        System.out.format("+-----------------+-------------+%n");

        for (Map.Entry<String, Boolean> entry : worthUsers.entrySet()) {
            if (entry.getValue()) status = online;
            else status = "offline";
            System.out.format(rowFormat, entry.getKey(), status);
        }
        System.out.format("+-----------------+-------------+%n");
    }

    /** logout and quit client */
    public void quit(){
        logout();
        System.exit(0);
    }

    /** utility to print fancy ANSI terminal colors. */
    private void print(String message, String color){
        System.out.println(TermColors.Colors.get(color) + message + TermColors.Colors.get("reset"));
    }

    private void registerForCallback(){
        try {
            RMIClientInterface stub = (RMIClientInterface) UnicastRemoteObject.exportObject(callbackAgent, 0);
            remote.registerForCallback(stub);

        }catch (RemoteException e){
            e.printStackTrace();
            print("> ERROR: (RMI) cannot subscribe to server callback", "red");
        }
    }

}