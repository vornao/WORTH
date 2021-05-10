package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface RMIClientInterface extends Remote {
    void notifyUser(String username, Boolean status) throws RemoteException;
}
