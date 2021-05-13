package server;

import client.RMIClient;
import interfaces.RMIClientInterface;
import interfaces.RMIServerInterface;
import utils.FileHandler;
import utils.PasswordHandler;
import utils.Printer;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class RMIServer extends RemoteServer implements RMIServerInterface {
    private final ConcurrentHashMap<String, User> userList;
    private final List<RMIClientInterface> clients = new ArrayList<>();
    private final FileHandler fh;
    public RMIServer(ConcurrentHashMap<String, User> userList, String projectdir){
        this.userList = userList;
        this.fh = new FileHandler(projectdir);
    }

    @Override
    public synchronized int signUp(String username, String password) throws RemoteException {

            if (userList.containsKey(username)) return 2;
            //todo add persistence
            String salt = PasswordHandler.salt();
            String hash = PasswordHandler.hash(password, salt);
            User u = new User(username, hash, salt);
            userList.put(username, u);
            fh.saveUser(u);

        Printer.println("> DEBUG: REGISTER USER: USER CREATED", "blue");
            updateUsers(username, false);

        return 1;
    }

    @Override
    public synchronized void registerForCallback(RMIClientInterface client) throws RemoteException {
        if(!clients.contains(client)){
            Printer.println("> INFO: CLIENT REGISTERED TO CALLBACK", "green");
            clients.add(client);
        }
    }

    @Override
    public synchronized void unregisterForCallback(RMIClientInterface client) throws RemoteException{
        if(clients.remove(client)){
            Printer.println("> INFO: CLIENT UNREGISTERED FROM CALLBACK", "green");
        }
    }

    public synchronized void updateUsers(String username, Boolean status){
        for (RMIClientInterface client : clients) {
            try {
                client.notifyUser(username, status);
            } catch (RemoteException e) {
                //client no longer available
                clients.remove(client);
                e.printStackTrace();
            }
        }
    }
}
