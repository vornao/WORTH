package client;

import interfaces.RemoteSignUpInterface;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;

public class MainClient {

    private static final int SERVER_PORT = 6789;
    private static final String REGISTRY_NAME = "SIGN-UP-SERVER";

    private static final HashMap<Integer, String> SignupErrorMessages = new HashMap<Integer, String>(){
        {
            put(1, "> User created successfully");
            put(2, "> User creation failed, try again");
            put(0, "> Username already exists, try again");
        }
    };

    public static void main(String[] args) throws IOException {
        System.out.println( "\t--- Welcome to WORTH CLI client! ---\t");
        BufferedReader bif =  new BufferedReader(new InputStreamReader(System.in));
        while(true){
            System.out.print("> ");
            switch (bif.readLine()){
                case "help"  : printHelp(); break;
                case "signup": signUp(); break;
                case "login" : login(); break;
                default      : break;
            }
        }
    }


    /** connects to RMI server in order to let user sign up. */
    private static void signUp(){
        String username;
        String password;
        BufferedReader input =  new BufferedReader(new InputStreamReader(System.in)
        );

        try {
            Registry registry = LocateRegistry.getRegistry("localhost", SERVER_PORT);
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

    private static void login(){
        System.out.println("< Not implemented yet");
        return;
    }

    private static void printHelp(){
        System.out.println("login - login utility");
        System.out.println("signup - register user utility");
        System.out.println("help - show this message");
    }
}
