package client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import exceptions.ProjectNotFoundException;
import server.utils.Printer;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatHelper {

    //key value store for mcast addresses and projects <projectname, address>
    private final ConcurrentHashMap<String, String> projectChats;
    private final ConcurrentHashMap<String, MembershipKey> groupKeys;
    //key value stores for last-received messages <projectnme, last read messages>
    private final ConcurrentHashMap<String, List<String>> messages;


    //multicast "connection" handlers
    private final NetworkInterface networkInterface;
    private final DatagramChannel datagramChannel;
    private final Selector selector;

    private final Gson gson = new Gson();

    public ChatHelper(int port, String address) throws IOException {
        projectChats = new ConcurrentHashMap<>();
        messages = new ConcurrentHashMap<>();
        groupKeys = new ConcurrentHashMap<>();
        selector = Selector.open();
        networkInterface = NetworkInterface.getByInetAddress(InetAddress.getByName(address));
        datagramChannel = DatagramChannel.open(StandardProtocolFamily.INET);
        datagramChannel.socket().setReuseAddress(true);
        datagramChannel.socket().bind(new InetSocketAddress(port));
        datagramChannel.configureBlocking(false);
        datagramChannel.register(selector, SelectionKey.OP_READ);
        Printer.println(String.format("> DEBUG: bind UDP socket to %s:%d", address, port), "yellow");
   }

    //synchronized on selector, to avoid concurrent selector modifications.
    public void startChatListener(){

        synchronized (selector) {
            try {
                if (selector.select() == 0) return;
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keysIterator = selectedKeys.iterator();

                while (keysIterator.hasNext()) {
                    SelectionKey selectionKey = keysIterator.next();
                    keysIterator.remove();
                    if (selectionKey.isReadable()) readSocketChannel(selector, selectionKey);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void joinGroup(String address, String projectname) throws IOException {
        MembershipKey membershipKey = datagramChannel.join(InetAddress.getByName(address), networkInterface);
        projectChats.put(projectname, address);

        groupKeys.put(address, membershipKey);
        //the arraylist in the hashmap need to be synchronized too.
        messages.put(projectname, Collections.synchronizedList(new ArrayList<>()));

        Printer.print("> DEBUG: " + projectChats, "yellow");
    }

    public synchronized void leaveGroup(String address, String projectname){
        projectChats.remove(projectname, address);
        messages.remove(projectname);
        groupKeys.remove(address).drop();
        Printer.print("> DEBUG: " + projectChats, "yellow");
    }

    /**
     * try to read from datagramChannel: reads message and dispatch message to appropriate project chat.
     * synchronized on this to prevent concurrent modifications to datagramChannel (e.g. a thread calls joinGroup)
     */

    private synchronized void readSocketChannel(Selector selector, SelectionKey selectionKey) throws IOException {
        DatagramChannel dc = (DatagramChannel) selectionKey.channel();
        dc.configureBlocking(false);
        ByteBuffer buffer = ByteBuffer.wrap(new byte[512]);
        buffer.clear();
        dc.receive(buffer);
        buffer.flip();
        String msg  = StandardCharsets.UTF_8.decode(buffer).toString();

        JsonObject message = gson.fromJson(msg, JsonObject.class);
        try{
            String formattedMessage = String.format(
                    "%s: %s",
                    message.get("from").getAsString(),
                    message.get("body").getAsString());

            messages.get(message.get("projectname").getAsString()).add(formattedMessage);
        }catch (Exception e){
            e.printStackTrace();
        }
        dc.register(selector, SelectionKey.OP_READ);
    }

    public synchronized void sendMessage(String project, String from, String body) throws IOException, ProjectNotFoundException {
        JsonObject message = new JsonObject();
        message.addProperty("projectname", project);
        message.addProperty("from", from);
        message.addProperty("body", body);
        ByteBuffer buffer = ByteBuffer.wrap(message.toString().getBytes(StandardCharsets.UTF_8));
        buffer.clear();
        buffer.put(message.toString().getBytes(StandardCharsets.UTF_8));
        buffer.flip();

        String address = projectChats.get(project);
        if(project == null) throw new ProjectNotFoundException();
        datagramChannel.send(buffer, new InetSocketAddress(address, 5678));
    }

    //Concurrent collection is returned, thread safe method
    public List<String> getProjectMessages(String projectname){
        return this.messages.get(projectname);
    }


}
