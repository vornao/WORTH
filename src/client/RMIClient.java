package client;

import interfaces.RMIClientInterface;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.concurrent.ConcurrentHashMap;


public class RMIClient extends RemoteObject implements RMIClientInterface {
    private final ConcurrentHashMap<String, Boolean> users;
    private final ChatHelper chatHelper;
    private final String loginName;

    /** creates new callback client */
    public RMIClient(ConcurrentHashMap<String, Boolean> worthUsers, ChatHelper chatHelper, String loginName){
        super();
        this.chatHelper = chatHelper;
        this.users = worthUsers;
        this.loginName = loginName;
    }

    /** called from server when event occurs*/
    @Override
    public synchronized void notifyUser(String username, Boolean status) throws RemoteException {
        users.put(username, status);
    }

    @Override
    public synchronized void notifyChat(String address, String projectname) throws RemoteException{
        try{
            chatHelper.joinGroup(address, projectname);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public synchronized String getUsername() throws RemoteException{
        return loginName;
    }

    @Override
    public synchronized void leaveGroup(String address, String projectname) throws RemoteException {
        chatHelper.leaveGroup(address, projectname);
    }

}
