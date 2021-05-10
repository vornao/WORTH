package client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import interfaces.RemoteSignUpInterface;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

public class Client {
    private final int RMI_SERVER_PORT;
    private final int PORT;
    private final String REGISTRY_NAME = "SIGN-UP-SERVER";
    private final String ADDRESS;

    private final SocketChannel socketChannel;
    private final ByteBuffer buffer;

    private String loginName = null;
    private String chatAddress = null;

    private final HashMap<String, Boolean> worthUsers;

    Gson gson = new Gson();

   private static final HashMap<Integer, String> SignupErrorMessages = new HashMap<Integer, String>(){
        {
            put(1, "> User created successfully");
            put(2, "> User creation failed, try again");
            put(0, "> Username already exists, try again");
        }
    };

    public Client(String address, int port, int rmiport) throws IOException {
        this.ADDRESS = address;
        this.RMI_SERVER_PORT = rmiport;
        this.PORT = port;
        worthUsers = new HashMap<>();
        socketChannel = SocketChannel.open();
        try {
            socketChannel.connect(new InetSocketAddress(ADDRESS, PORT));
        }catch(IOException e){
            System.out.println("ERROR: Failed to connect to server.");
            System.exit(-1);
        }
        buffer = ByteBuffer.wrap(new byte[8192]);
        buffer.clear();
    }

    /** connects to RMI server in order to let user sign up. */
    public void signUp(){
        String username;
        String password;
        BufferedReader input =  new BufferedReader(new InputStreamReader(System.in));

        try {
            Registry registry = LocateRegistry.getRegistry("localhost", RMI_SERVER_PORT);
            RemoteSignUpInterface remote = (RemoteSignUpInterface) registry.lookup(REGISTRY_NAME);
            System.out.print("> Insert new username: ");
            username = input.readLine();
            System.out.print("> Insert password: ");
            password = input.readLine();
            System.out.print("> Confirm password: ");

            if(!password.equals(input.readLine())){
                System.out.println("< Password not matching! Retry");
            }

            int status = remote.signUp(username, password);
            System.out.println(SignupErrorMessages.get(status));

        } catch (NotBoundException | IOException e){
            e.printStackTrace();
        }
    }

    public void printHelp(){
        System.out.println("login - login utility");
        System.out.println("signup - register user utility");
        System.out.println("help - show this message");
    }

    public void login(){

        String username;
        String password;

        //read user input from keyboard
        BufferedReader input =  new BufferedReader(new InputStreamReader(System.in));
        try {
        //ask user for login credentials
            System.out.print("> Username: ");
            username = input.readLine();
            System.out.print("> Password: ");
            password = input.readLine();

            JsonObject loginJson =  new JsonObject();
            loginJson.addProperty("method", "login");
            loginJson.addProperty("username", username);
            loginJson.addProperty("password", password);

            System.out.println("> DEBUG: " + loginJson);

            //write login request to server
            buffer.put(loginJson.toString().getBytes());
            buffer.flip();
            while(buffer.hasRemaining()) socketChannel.write(buffer);
            buffer.clear();

            //wait for response
            int bytes = socketChannel.read(buffer);
            buffer.flip();
            String responseString;
            responseString = StandardCharsets.UTF_8.decode(buffer).toString();
            buffer.clear();
            System.out.println(buffer);
            System.out.println("> DEBUG: RECEIVED: " + responseString);
            //parsing response as generic JsonObject
            JsonObject response = gson.fromJson(responseString, JsonObject.class);

            assert response != null;
            if(response.get("return-code").getAsString().equals("200")){
                System.out.println("< Login Successful!");
                this.loginName = username; //associating current login name to session for future requests
            }else{
                System.out.println("< Login failed: " + response.get("return-code").getAsString());
                return;
            }


            //parse users to local map
            JsonArray userlist = response.get("registered-users").getAsJsonArray();

            for(JsonElement j : userlist){
                JsonObject user = (JsonObject)j;
                worthUsers.put(user.get("username").getAsString(), user.get("status").getAsBoolean());
            }
        } catch (IOException e){
            e.printStackTrace();
            System.out.println("< Error sending message to server");
        }
    }

    public void logout(){
        if (loginName == null) {
            System.out.println("< ERROR: Log you in before logging out!");
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
                System.out.println("Error logging out!");
            }

            loginName = null;

        } catch (IOException e) {
            System.out.println("< Error sending message to server");
            e.printStackTrace();
        }
    }

    private void setOnlineList(JsonObject jsonObject){

    }
}