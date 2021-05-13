package server;

import utils.Printer;

import java.io.IOException;

public class MainServer {
    private final static String WORTHDIR = "/users/Vornao/Desktop";
    //todo handle exceptions
    public static void main(String[] args) throws IOException {
        Printer.println(
                "\n--- WELCOME TO W.O.R.T.H. SERVER - (PROGETTO RETI LABORATORIO, A.A. 2020/2021) ---\n",
                "purple");
        Server server = new Server("localhost", 6789, 6790, WORTHDIR);
        server.start();
    }

}
