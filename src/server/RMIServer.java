package server;

import client.RMIClient;
import interfaces.RMIClientInterface;
import interfaces.RMIServerInterface;
import utils.PasswordHandler;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class RMIServer extends RemoteServer implements RMIServerInterface {
    private final ConcurrentHashMap<String, User> userList;
    private final List<RMIClientInterface> clients = new ArrayList<>();
    public RMIServer(ConcurrentHashMap<String, User> userList){
        this.userList = userList;
    }

    @Override
    public synchronized int signUp(String username, String password) throws RemoteException {

            if (userList.containsKey(username)) return 2;
            //todo add persistence
            String salt = PasswordHandler.salt();
            String hash = PasswordHandler.hash(password, salt);
            userList.put(username, new User(username, hash, salt));
            System.out.println("> DEBUG: REGISTER USER: USER CREATED");
            updateUsers(username, false);

        return 1;
    }

    @Override
    public synchronized void registerForCallback(RMIClientInterface client) throws RemoteException {
        if(!clients.contains(client)){
            System.out.println("CLIENT REGISTERED TO CALLBACK");
            clients.add(client);
        }
    }

    @Override
    public synchronized void unregisterForCallback(RMIClientInterface client) throws RemoteException{
        if(clients.remove(client)){
            System.out.println("CLIENT UNREGISTERED FROM CALLBACK");
        }
    }

    public synchronized void updateUsers(String username, Boolean status){
        for (RMIClientInterface client : clients) {
            try {
                client.notifyUser(username, status);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
