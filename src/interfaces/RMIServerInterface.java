package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIServerInterface extends Remote {

    /** Called by client when signing up */
    public int  signUp(String Username, String password) throws RemoteException;
    public void registerForCallback(RMIClientInterface client) throws RemoteException;
    public void unregisterForCallback(RMIClientInterface client) throws RemoteException;

}
