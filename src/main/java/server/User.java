package server;

public class User {

    private int sessionHash;
    private String username;
    private String password;
    private String salt;
    private boolean online = false;

    public User(String username, String password, String salt){
        this.username =  username;
        this.password = password;
        this.salt = salt;
    }

    public String getUsername(){
        return this.username;
    }

    public void setUsername(String username){
        this.username = username;
    }

    public void setPassword(String password){
        this.password = password;
    }

    public String getPassword(){
        return password;
    }

    public String getSalt(){ return this.salt; }

    public boolean getStatus() { return this.online; }

    public void setStatus(boolean status){ this.online = status; }

    public void setSalt(String salt) { this.salt = salt; }

    public void setSessionPort(int s){
        this.sessionHash = s;
    }

    public int getSessionPort(){
        return this.sessionHash;
    }
}
