package server;

public class MainServer {

    //todo handle exceptions
    public static void main(String[] args) {
        Server server = new Server("localhost", 6789, 6790);
        server.start();
    }

}
