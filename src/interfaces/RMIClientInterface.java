package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIClientInterface extends Remote {
    void notifyUser(String username, Boolean status) throws RemoteException;
    void notifyChat(String address, String projectname) throws RemoteException;
    void leaveGroup(String address, String projectname) throws RemoteException;

    String getUsername() throws RemoteException;

}
