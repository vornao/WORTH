package server;

import com.sun.org.apache.regexp.internal.RE;
import org.apache.commons.cli.*;
import server.utils.Printer;

import java.io.IOException;
import java.net.InetAddress;

public class WorthServer {

    private static int TCP_PORT = 0;
    private static int RMI_PORT = 0;
    private static String ADDRESS;
    private static String WORTHDIR;
    private static String REGISTRY_NAME;


    //todo handle exceptions
    public static void main(String[] args) throws IOException {

        Options options = new Options();
        options.addOption("b", "bind-address",true,  "Server Address    - default localhost");
        options.addOption("p", "tcp-port",    true,  "Server TCP Port   - default 6789");
        options.addOption("r", "rmi-port",    true,  "Server RMI Port   - default 6790");
        options.addOption("n", "registry-name",    true,  "RMI Registry name   - default WORTH-RMI");
        options.addOption("d", "project-dir", true,  "Project Directory - default your_current_directory/WORTH");
        options.addOption("h", "help",        false, "Prompt help dialog");

        HelpFormatter helpFormatter = new HelpFormatter();

        //try to parse command line. if argument missing using default
        try {
            CommandLineParser commandLineParser = new DefaultParser();
            CommandLine commandLine = commandLineParser.parse(options, args);

            if(args.length == 1 && (commandLine.hasOption("h")  || commandLine.hasOption("--help"))){
                throw new ParseException("help dialog");
            }

            if (commandLine.hasOption("b") || commandLine.hasOption("--bind-address"))
                 ADDRESS = (commandLine.getOptionValues("b")[0]);
            else ADDRESS = InetAddress.getLocalHost().getHostAddress();;

            if (commandLine.hasOption("p") || commandLine.hasOption("--tcp-port") )
                 TCP_PORT = Integer.parseInt(commandLine.getOptionValues("p")[0]);
            else TCP_PORT = 6789;

            if (commandLine.hasOption("r") || commandLine.hasOption("--rmi-port") )
                 RMI_PORT = Integer.parseInt(commandLine.getOptionValues("r")[0]);
            else RMI_PORT = 6790;

            if (commandLine.hasOption("d") || commandLine.hasOption("--project-dir"))
                 WORTHDIR = commandLine.getOptionValues("d")[0];
            else WORTHDIR = System.getProperty("user.dir");

            if (commandLine.hasOption("n") || commandLine.hasOption("--registry-name"))
                 REGISTRY_NAME = commandLine.getOptionValues("n")[0];
            else REGISTRY_NAME = "WORTH-RMI";

        }catch(ParseException p){

            helpFormatter.printHelp("java WorthServer", options);
            System.exit(-1);
        }

        Printer.println(
                "\n--- WELCOME TO W.O.R.T.H. SERVER - (PROGETTO RETI LABORATORIO, A.A. 2020/2021) ---\n",
                "purple");
        Server server = new Server(ADDRESS, TCP_PORT, REGISTRY_NAME, RMI_PORT, WORTHDIR);
        server.start();
    }

}
