package server;

import interfaces.RMIClientInterface;
import interfaces.RMIServerInterface;
import server.utils.FileHandler;
import server.utils.PasswordHandler;
import server.utils.Printer;
import shared.Project;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class RMIServer extends RemoteServer implements RMIServerInterface {
    private final ConcurrentHashMap<String, User> userList;
    private final List<RMIClientInterface> clients = Collections.synchronizedList(new ArrayList<>());
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
            userList.putIfAbsent(username, u);
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

    //implements online user state callback for clients.
    public synchronized void updateUsers(String username, Boolean status) throws RemoteException{

        //using an iterator to avoid ConcurrentModificationException for removing while iterating on collection
        Iterator<RMIClientInterface> iterator = clients.iterator();
        while(iterator.hasNext()) {
            try {
                iterator.next().notifyUser(username, status);
            } catch (RemoteException e) {
                //client no longer available
                iterator.remove();
                //e.printStackTrace();
            }
        }
    }

    /**
     * implements callback to notify an online client that has been added to a project.
     * client can now join project chats without user to manually update project lists.
     */
    public synchronized void updateChat(String username, String projectname, String address) throws RemoteException{
        //using an iterator to avoid ConcurrentModificationException for removing while iterating on collection
        Iterator<RMIClientInterface> iterator = clients.iterator();
        while(iterator.hasNext()) {
            try {
                RMIClientInterface client = iterator.next();
                if(client.getUsername().equals(username)) client.notifyChat(address, projectname);
            } catch (RemoteException e) {
                //client no longer available
                iterator.remove();
                //e.printStackTrace();
            }
        }
    }

    public synchronized void leaveGroup(Project p){
        ArrayList<String> users = p.getMembers();
        Iterator<RMIClientInterface> iterator = clients.iterator();
        while(iterator.hasNext()) {
            try {
                RMIClientInterface client = iterator.next();
                if(users.contains(client.getUsername())) client.leaveGroup(p.getChatAddress(), p.getName());
            } catch (RemoteException e) {
                //client no longer available
                iterator.remove();
                //e.printStackTrace();
            }
        }
    }

}
