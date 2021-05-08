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

public class Server {
    private final int CHANNEL_BUFFER_SIZE = 8192;
    private final int SOCKETPORT;
    private final int RMIPORT;
    private final String ADDRESS;
    private final String REGISTRY_NAME =  "SIGN-UP-SERVER";

    private final ConcurrentHashMap<String, User> registeredUsers = new ConcurrentHashMap<>();
    private final List<Project> projects = Collections.synchronizedList(new ArrayList<>());

    private static Selector selector;
    private static ServerSocketChannel serverSocketChannel;

    public Server(String address, int channelport, int rmiport){
        this.SOCKETPORT = channelport;
        this.RMIPORT = rmiport;
        this.ADDRESS = address;
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(SOCKETPORT));
            serverSocketChannel.configureBlocking(false);
            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        }catch(IOException e){
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void start(){

        startRMI();

        while(true){
            try{
                if(selector.select() == 0) continue;
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keysIterator = selectedKeys.iterator();

                while(keysIterator.hasNext()){
                    SelectionKey sk = keysIterator.next();
                    keysIterator.remove();
                    if (sk.isAcceptable()) registerRead(selector, sk);
                    else if (sk.isReadable()) readSocketChannel(selector, sk);
                    else if (sk.isWritable()) writeSocketChannel(selector, sk);
                }

            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private void startRMI(){

        try {
            RemoteSignUp remoteSignUpServer = new RemoteSignUp(registeredUsers);
            RemoteSignUpInterface stub = (RemoteSignUpInterface) UnicastRemoteObject.exportObject(remoteSignUpServer, RMIPORT);
            LocateRegistry.createRegistry(RMIPORT);
            //todo try to bind to non-localhost address!
            Registry registry = LocateRegistry.getRegistry(ADDRESS, RMIPORT);
            registry.rebind(REGISTRY_NAME, stub);

        }catch (RemoteException e){
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private boolean login(String username, String password){
        //retrieve user from usermap
        User u =  registeredUsers.get(username);
        if(u== null) return false;

        if(PasswordHandler.authenticate(password, u.getPassword(), u.getSalt())){
            return true;
        }
        //check username and pass
        //if ok set user status as online
        //return true
        return false;

    }

    private void registerRead(Selector s, SelectionKey sk) throws IOException {

        //setting things ready for SocketChannel to receive data
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) sk.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);

        //now link a Buffer to store channel's data, (8KB for connection)
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[CHANNEL_BUFFER_SIZE]);

        //socket is now ready for reading up to 8kb of incoming data from client.
        socketChannel.register(s, SelectionKey.OP_READ, byteBuffer);

    }

    private void readSocketChannel(Selector selector, SelectionKey selectionKey){
        //todo handle client message with some methods
    }

    private void writeSocketChannel(Selector selector, SelectionKey selectionKey){
        //todo handle client response with some appropriate methods
    }
}
