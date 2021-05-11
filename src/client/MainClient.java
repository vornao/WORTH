package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.NotBoundException;

public class MainClient {

    public static void main(String[] args) throws IOException, NotBoundException {
        Client client = new Client("localhost", 6789, 6790);

        System.out.println( "--- Welcome to WORTH CLI client! ---\n");
        client.printHelp();
        BufferedReader buf =  new BufferedReader(new InputStreamReader(System.in));
        while(true){
            System.out.print("> ");
            switch (buf.readLine()){
                case "help"       : client.printHelp(); break;
                case "signup"     : client.signUp();    break;
                case "login"      : client.login();     break;
                case "logout"     : client.logout();    break;
                case "list-users" : client.listUsers(); break;
                case "list-projects" : client.listProjects(); break;
                case "add-project": client.createProject(); break;
                case "list-online": client.listOnlineUsers(); break;
                case "clear"      : client.clear();     break;
                case "quit"       : client.quit();      break;
                default           : System.out.println("< Unknown command - type help for command list."); break;
            }
        }
    }
}
