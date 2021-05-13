package utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import shared.serializers.FileSerializerHelper;
import server.User;
import shared.Card;
import shared.Project;

import java.io.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class FileHandler {
    private final String basepath;
    private final String userspath;
    private final String projectpath;

    private final File base;
    private final File users;
    private final File projects;

    private final Gson serializer = setGson();

    public FileHandler(String projectdir){
        this.basepath = projectdir + "/worth";
        userspath = basepath + "/users";
        projectpath = basepath + "/projects";

        base = new File(basepath);
        users = new File(userspath);
        projects= new File(projectpath);

        if(!base.exists()){
            Printer.println("> Creating Project directories...", "yellow");
            boolean res = base.mkdir() && users.mkdir() && projects.mkdir();
            if(!res){
                Printer.println(">ERROR: Failed to create project directories! Quitting.", "red");
                System.exit(-1);
            }
        }
    }

    private Gson setGson(){
        FileSerializerHelper serializers = new FileSerializerHelper();
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Project.class, serializers.projectJsonSerializer);
        builder.registerTypeAdapter(Project.class, serializers.projectJsonDeserializer);
        builder.registerTypeAdapter(User.class, serializers.userJsonSerializer);
        builder.registerTypeAdapter(User.class, serializers.deserializeUser);
        builder.registerTypeAdapter(Card.class, serializers.deserializeCard);
        builder.registerTypeAdapter(Card.class, serializers.serializeCard);
        builder.setPrettyPrinting();
        builder.excludeFieldsWithoutExposeAnnotation();
        return builder.create();
    }

    public void saveUser(User user) {
        String userJson = serializer.toJson(user);
        File newUser = new File(userspath + "/" + user.getUsername() + ".json");
        try {
            FileWriter fw = new FileWriter(newUser);
            fw.write(userJson);
            fw.close();
        }catch(IOException e){
            Printer.println("> ERROR: failed to save user" + user.getUsername(), "red");
        }
    }

    public void saveProject(Project project) throws IOException {
        String projectJson = serializer.toJson(project);
        File projectDir = new File(projectpath + "/" + project.getName());
        if(!projectDir.exists()){
            if (!projectDir.mkdir())
                Printer.println("> ERROR: failed to create project directory" + project.getName(), "red");
        }

        File projectFile = new File(projectpath + "/" + project.getName() + "/property.json");
        FileWriter fw = new FileWriter(projectFile);
        Printer.println(projectJson, "yellow");
        fw.write(projectJson);
        fw.close();
    }

    public void saveCard(String projectname, Card card){
        String cardJson = serializer.toJson(card);
        File newCard = new File(projectpath + "/" + projectname + "/" + card.getName() + ".json");
        try {
            FileWriter fw = new FileWriter(newCard);
            fw.write(cardJson);
            fw.close();
        }catch (IOException e){
            e.printStackTrace();
            Printer.println("> ERROR: failed to save card" + card.getName(), "red");
        }
    }

    public ConcurrentHashMap<String, User> loadUsers() throws IOException {

        ConcurrentHashMap<String, User> userlist = new ConcurrentHashMap<>();
        if(users == null || users.list() == null){
            return userlist;
        }
        for(String file : Objects.requireNonNull(users.list())){
            if(!file.contains(".json")) continue;
            User user = serializer.fromJson(fileToString(userspath + "/" + file), User.class);
            userlist.put(user.getUsername(), user);
        }

        return userlist;
    }

    public ConcurrentHashMap<String, Project> loadProjects(){
        ConcurrentHashMap<String, Project> projectsMap = new ConcurrentHashMap<>();

        if(projects == null || projects.list() == null){
            return projectsMap;
        }

        //for each project directory
        for(String file : projects.list()){
            Project p;
            try {
                p = loadProject(projectpath + "/" + file);
                p.restoreCards(loadCards(projectpath + "/" + file));
                projectsMap.put(p.getName(), p);
                Printer.println("> INFO: Project " + file + " loaded", "green");
            }catch (IOException e){
                Printer.println(
                        "> WARNING: Error restoring project "+ file + ": skipping",
                        "yellow");
            }
        }

        return projectsMap;
    }

    private Project loadProject(String projectName) throws IOException {
        return serializer.fromJson(fileToString(projectName + "/property.json"), Project.class);
    }

    private Card loadCard(String cardname) throws IOException {
        return serializer.fromJson(fileToString(cardname), Card.class);
    }

    private ArrayList<Card> loadCards(String projectdir){
        ArrayList<Card> cardlist = new ArrayList<>();

        File cards = new File(projectdir);

        try {
            if(cards.list() == null) throw new IOException();

            for (String cardname : cards.list()) {
                if (cardname.equals("property.json")) continue;
                if (!cardname.contains(".json")) continue;
                Card c = loadCard(projectdir + "/" + cardname);
                cardlist.add(c);
            }

        }catch (IOException e){
            e.printStackTrace();
        }
        return cardlist;
    }

    private String fileToString(String filepath) throws IOException {
        FileReader p = new FileReader(filepath);
        BufferedReader br = new BufferedReader(p);
        StringBuilder sb = new StringBuilder();
        String line;
        while((line = br.readLine()) != null){
            sb.append(line);
        }
        return sb.toString();
    }
}
