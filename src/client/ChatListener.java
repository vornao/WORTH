package client;

public class ChatListener implements Runnable{
    private final ChatHelper chatHelper;

    public ChatListener(ChatHelper chatHelper){
        this.chatHelper = chatHelper;
    }

    @Override
    public void run() {
        while(!Thread.interrupted()) {
            chatHelper.startChatListener();
        }
    }
}
