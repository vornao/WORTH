package client;

import interfaces.RMIClientInterface;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.concurrent.ConcurrentHashMap;


public class RMIClient extends RemoteObject implements RMIClientInterface {
    private final ConcurrentHashMap<String, Boolean> users;

    /** creates new callback client */
    public RMIClient(ConcurrentHashMap<String, Boolean> worthUsers){
        super();
        this.users = worthUsers;
    }

    /** called from server when event occurs*/
    @Override
    public synchronized void notifyUser(String username, Boolean status) throws RemoteException {
        users.put(username, status);
    }
}
