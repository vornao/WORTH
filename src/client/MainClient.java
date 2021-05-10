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

    public static void main(String[] args) throws IOException {
        Client client = new Client("localhost", 6789, 6790);

        System.out.println( "\t--- Welcome to WORTH CLI client! ---\t");
        BufferedReader buf =  new BufferedReader(new InputStreamReader(System.in));
        while(true){
            System.out.print("> ");
            switch (buf.readLine()){
                case "help"   : client.printHelp(); break;
                case "signup" : client.signUp(); break;
                case "login"  : client.login(); break;
                case "logout" : client.logout(); break;
                case "quit"   : client.logout(); System.exit(0);
                default       : client.printHelp(); break;
            }
        }
    }
}
