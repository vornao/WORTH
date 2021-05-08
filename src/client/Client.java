package client;

import interfaces.RemoteSignUpInterface;
import sun.lwawt.macosx.CSystemTray;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.HashMap;

public class Client {
    private final int RMI_SERVER_PORT;
    private final int PORT;
    private final String REGISTRY_NAME = "SIGN-UP-SERVER";
    private final String ADDRESS;

    private Socket socket;
    private OutputStream outToServer;
    private InputStream inFromServer;


    private static final HashMap<Integer, String> SignupErrorMessages = new HashMap<Integer, String>(){
        {
            put(1, "> User created successfully");
            put(2, "> User creation failed, try again");
            put(0, "> Username already exists, try again");
        }
    };

    public Client(String address, int port, int rmiport){
        this.ADDRESS = address;
        this.RMI_SERVER_PORT = rmiport;
        this.PORT = port;
        connect();
    }

    private void connect() {
        try{
            socket = new Socket(ADDRESS, PORT);
            outToServer = socket.getOutputStream();
            inFromServer = socket.getInputStream();

        }catch (IOException e){
            e.printStackTrace();
            System.exit(-1);
        }
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

    public void login() {
        String messaggio = "helo";
        byte[] inBytes = new byte[8192];
        String username;
        String password;
        BufferedReader input =  new BufferedReader(new InputStreamReader(System.in));
        try {
            outToServer.write(messaggio.getBytes());
            int read = inFromServer.read(inBytes);
            System.out.println("> RECEIVED: " + new String(inBytes, 0, read));
        } catch (IOException e){
            System.out.println("< Error sending message to server");
        }
    }

}
