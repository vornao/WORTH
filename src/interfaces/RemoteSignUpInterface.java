package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteSignUpInterface extends Remote {

    /** Called by client when signing up */
    public int signUp(String Username, String password) throws RemoteException;
}
