package server;

import interfaces.RemoteSignUpInterface;
import utils.PasswordHandler;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.concurrent.ConcurrentHashMap;

public class RemoteSignUp extends RemoteServer implements RemoteSignUpInterface {
    private final ConcurrentHashMap<String, User> userList;

    public RemoteSignUp(ConcurrentHashMap<String, User> userList){
        this.userList = userList;
    }

    @Override
    public int signUp(String username, String password) throws RemoteException {

        /** START SYNCHRONIZED BLOCK */
        synchronized (userList){
            if (userList.containsKey(username)) return 2;
            //@todo add persistence
            String salt = PasswordHandler.salt();
            String hash = PasswordHandler.hash(password, salt);
            userList.put(username, new User(username, hash, salt));
            System.out.println("> DEBUG: REGISTER USER: USER CREATED");
        }
        /**END SYNCHRONIZED BLOCK */

        return 1;
    }


}
