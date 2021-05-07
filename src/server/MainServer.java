package server;

import interfaces.RemoteSignUpInterface;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MainServer {
    private static final String REGISTRY_NAME =  "SIGN-UP-SERVER";
    private static final int PORT = 6789;

    private static final List<User> registeredUsers = Collections.synchronizedList(new ArrayList<>());
    private static final List<User> onlineUsers = Collections.synchronizedList(new ArrayList<>());
    private static final List<Project> projects = Collections.synchronizedList(new ArrayList<>());


    public static void main(String[] args){
        startRMI();
    }

    //starting rmi method for user sign up operation
    private static void startRMI(){
        try {
            RemoteSignUp remoteSignUpServer = new RemoteSignUp(registeredUsers);
            RemoteSignUpInterface stub = (RemoteSignUpInterface) UnicastRemoteObject.exportObject(remoteSignUpServer, PORT);
            LocateRegistry.createRegistry(PORT);
            //@todo try to bind to non-localhost address!
            Registry registry = LocateRegistry.getRegistry("localhost", PORT);
            registry.rebind("SIGN-UP-SERVER", stub);
        }catch (RemoteException e){
            e.printStackTrace();
            //@todo should exit
        }
    }
}
