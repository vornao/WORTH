package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIServerInterface extends Remote {

    int  signUp(String Username, String password) throws RemoteException;
    void registerForCallback(RMIClientInterface client) throws RemoteException;
    void unregisterForCallback(RMIClientInterface client) throws RemoteException;

}
