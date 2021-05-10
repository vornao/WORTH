package server;

import com.google.gson.*;
import interfaces.RemoteSignUpInterface;
import utils.PasswordHandler;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final int CHANNEL_BUFFER_SIZE = 8192;
    private final int SOCKETPORT;
    private final int RMIPORT;
    private final String ADDRESS;
    private static final String REGISTRY_NAME =  "SIGN-UP-SERVER";

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
        System.out.println("---- Welcome to WORTH server (Progetto Reti Laboratorio a.a. 2020/20201) ----");
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

    private void registerRead(Selector s, SelectionKey sk) throws IOException {

        //setting things ready for SocketChannel to receive data
        System.out.println("Client connected!");
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) sk.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);

        //now link a Buffer to store channel's data, (8KB for connection)
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[CHANNEL_BUFFER_SIZE]);

        //socket is now ready for reading up to 8kb of incoming data from client.
        socketChannel.register(s, SelectionKey.OP_READ, byteBuffer);

    }

    private void readSocketChannel(Selector selector, SelectionKey selectionKey) throws IOException {
        String message;
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        socketChannel.configureBlocking(false);
        ByteBuffer buffer = (ByteBuffer) selectionKey.attachment();

        //client disconnected
        if( socketChannel.read(buffer) < 0) {

            for(User u: registeredUsers.values()){
                //if user was logged in, log out.
                if(u.getSessionPort() == socketChannel.getRemoteAddress().hashCode()){
                    u.setStatus(false);
                    u.setSessionPort(-1);
                }
            }
            System.out.println("CLIENT DISCONNECTED.");
            socketChannel.close();
            return;
        }

        buffer.flip();
        message = StandardCharsets.UTF_8.decode(buffer).toString();

        System.out.println("> DEBUG: RECEIVED:" + message);
        buffer.clear();

        //parse message as JSON object to handle it easily
        Gson gson = new Gson();

        JsonObject request = gson.fromJson(message, JsonObject.class);
        JsonObject response =  null;

        //fetching requested method and computing response
        assert request != null;
        switch (request.get("method").getAsString()){
            case "login":
                response = login(request.get("username").getAsString(),
                        request.get("password").getAsString(),
                        socketChannel.getRemoteAddress().hashCode());
                break;
            case "logout":
                response = logout(request.get("username").getAsString());
                break;
            default: break;
        }

        assert response != null;
        buffer.put(response.toString().getBytes(StandardCharsets.UTF_8));
        buffer.flip(); //ready to be read again from zero to limit

        while(buffer.hasRemaining()) socketChannel.write(buffer);
        buffer.flip();
        System.out.println("> DEBUG: buffer content" + StandardCharsets.UTF_8.decode(buffer));
        buffer.clear(); //limit = pos = 0
        System.out.println("> DEBUG: " + response);
        socketChannel.register(selector, SelectionKey.OP_READ, buffer); //channel ready for new client requests.
    }

    private void writeSocketChannel(Selector selector, SelectionKey selectionKey) throws IOException {
    }

    private JsonObject login(String username, String password, int socketHash){
        JsonObject response = new JsonObject();
        User u =  registeredUsers.get(username);

        //user not found or auth failed
        if (u == null || u.getStatus() || !PasswordHandler.authenticate(password, u.getPassword(), u.getSalt())) {
            //401 - unauthorized - http like
            response.addProperty("return-code", 401);
            return response;
        }

        //auth ok
        registeredUsers.get(username).setStatus(true);    //set user state to online
        registeredUsers.get(username).setSessionPort(socketHash);
        response.addProperty("return-code", 200); //send 200 OK code

        //fetch registered user and send status.
        JsonArray jsonArray = new JsonArray();
        for(User t : registeredUsers.values()){
            JsonObject userJson = new JsonObject();
            userJson.addProperty("username", t.getUsername());
            userJson.addProperty("status", t.getStatus());
            jsonArray.add(userJson);
        }
        response.add("registered-users", jsonArray);
        return response;
    }

    private JsonObject logout(String username){
        JsonObject response = new JsonObject();

        if(!registeredUsers.get(username).getStatus()){
            response.addProperty("return-code", 300);
            return response;
        }
        registeredUsers.get(username).setStatus(false);
        registeredUsers.get(username).setSessionPort(-1);
        response.addProperty("return-code", 200);
        return response;
    }
}
