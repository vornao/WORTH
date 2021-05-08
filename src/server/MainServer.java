package server;

import interfaces.RemoteSignUpInterface;
import utils.PasswordHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class MainServer {

    //todo handle exceptions
    public static void main(String[] args) {
        Server server = new Server("localhost", 6789, 6790);
        server.start();
    }

}
