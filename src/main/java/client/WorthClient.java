package client;

import org.apache.commons.cli.*;
import server.utils.Printer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.rmi.NotBoundException;


public class WorthClient {

    private static String ADDRESS;
    private static String CHAT_SOCKET_ADDR;
    private static String REGISTRY_NAME;
    private static int TCP_PORT;
    private static int RMI_PORT;
    private static int CHAT_PORT;


    public static void main(String[] args) throws IOException, NotBoundException {
        Options options = new Options();
        options.addOption("s", "server-address",true,"Server Address  - default local machine address");
        options.addOption("p", "tcp-port",    true,  "Server TCP Port   - default 6789");
        options.addOption("r", "rmi-port",    true,  "Server RMI Port   - default 6790");
        options.addOption("n", "registry-name",    true,  "RMI Registry name   - default WORTH-RMI");
        options.addOption("c", "chat-port", true,    "UDP Multicast chat port - default 5678");
        options.addOption("u", "chat-socket", true,    "UDP socket address  - default machine network ip address");
        options.addOption("h", "help",        false, "Prompt help dialog");

        HelpFormatter helpFormatter = new HelpFormatter();

        //try to parse command line. if argument missing using default
        try {
            CommandLineParser commandLineParser = new DefaultParser();
            CommandLine commandLine = commandLineParser.parse(options, args);

            if(args.length == 1 && (commandLine.hasOption("h")  || commandLine.hasOption("--help"))){
                throw new ParseException("help dialog");
            }

            if (commandLine.hasOption("s") || commandLine.hasOption("--bind-address"))
                 ADDRESS = (commandLine.getOptionValues("s")[0]);
            else ADDRESS = InetAddress.getLocalHost().getHostAddress();

            if (commandLine.hasOption("p") || commandLine.hasOption("--tcp-port") )
                 TCP_PORT = Integer.parseInt(commandLine.getOptionValues("p")[0]);
            else TCP_PORT = 6789;

            if (commandLine.hasOption("r") || commandLine.hasOption("--rmi-port") )
                 RMI_PORT = Integer.parseInt(commandLine.getOptionValues("r")[0]);
            else RMI_PORT = 6790;

            if (commandLine.hasOption("c") || commandLine.hasOption("--chat-port"))
                 CHAT_PORT = Integer.parseInt(commandLine.getOptionValues("d")[0]);
            else CHAT_PORT = 5678;

            if (commandLine.hasOption("u") || commandLine.hasOption("--chat-socket"))
                 CHAT_SOCKET_ADDR = commandLine.getOptionValues("u")[0];
            else CHAT_SOCKET_ADDR = InetAddress.getLocalHost().getHostAddress();

            if (commandLine.hasOption("n") || commandLine.hasOption("--registry-name"))
                REGISTRY_NAME = commandLine.getOptionValues("n")[0];
            else REGISTRY_NAME = "WORTH-RMI";

        }catch(ParseException p){
            helpFormatter.printHelp("java WorthClient", options);
            System.exit(-1);
        }

        ChatHelper chatHelper;
        Client client;
        try {
            chatHelper = new ChatHelper(CHAT_PORT, CHAT_SOCKET_ADDR);
            client = new Client(ADDRESS, TCP_PORT, RMI_PORT, REGISTRY_NAME, chatHelper);

        }catch (IOException | NotBoundException e){
            Printer.println("Failed to connect WORTH server, quit.", "red");
            return;
        }

        Thread chat = new Thread(new ChatListener(chatHelper));
        chat.start();
        try {
            System.out.println("--- Welcome to WORTH CLI client! ---\n");
            client.printHelp();
            BufferedReader buf = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                System.out.print("> ");
                switch (buf.readLine()) {
                    case "read-chat":
                        client.readChat();
                        break;
                    case "send-chat":
                        client.sendChat();
                        break;
                    case "help":
                        client.printHelp();
                        break;
                    case "signup":
                        client.signUp();
                        break;
                    case "login":
                        client.login();
                        break;
                    case "logout":
                        client.logout();
                        break;
                    case "list-projects":
                        client.listProjects();
                        break;
                    case "add-project":
                        client.createProject();
                        break;
                    case "add-member":
                        client.addMember();
                        break;
                    case "add-card":
                        client.addCard();
                        break;
                    case "show-card":
                        client.showCard();
                        break;
                    case "list-cards":
                        client.listCards();
                        break;
                    case "list-members":
                        client.showMembers();
                        break;
                    case "move-card":
                        client.moveCard();
                        break;
                    case "card-history":
                        client.getCardHistory();
                        break;
                    case "delete-project":
                        client.deleteProject();
                        break;
                    case "quit":
                        client.quit();
                        break;
                    case "list-users":
                        client.listUsers(false);
                        break;
                    case "list-online":
                        client.listUsers(true);
                        break;
                    default:
                        System.out.println("< Unknown command - type help for command list.");
                        break;
                }
            }
        } catch (Exception e){
            Printer.println("Fatal IO exception. Check your connection status.", "red");
            return;
        }
    }
}
