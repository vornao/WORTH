package server;

import interfaces.RemoteSignUpInterface;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RemoteSignUp extends RemoteServer implements RemoteSignUpInterface {
    private final List<User> userList;

    public RemoteSignUp(List<User> userList){
        this.userList = Collections.synchronizedList(userList);
    }

    @Override
    public int signUp(String username, String password) throws RemoteException {

        synchronized (userList){
            for(User u : userList){
                if(u.getUsername().equalsIgnoreCase(username)){
                    System.out.println("> User exists, aborting");
                    return 2;
                }
            }
            userList.add(new User(username, password));
            System.out.println("USER CREATED");
        }
        return 1;
    }
}
