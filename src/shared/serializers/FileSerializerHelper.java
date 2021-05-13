package shared.serializers;

import com.google.gson.*;
import server.User;
import shared.Card;
import shared.CardEvent;
import shared.Project;

import java.util.ArrayList;

public class FileSerializerHelper {

    public JsonSerializer<Project> projectJsonSerializer = (project, type, jsonSerializationContext) -> {
        JsonObject serialized = new JsonObject();
        serialized.addProperty("name", project.getName());
        JsonArray members =  new JsonArray();
        for(String s : project.getMembers()){
            members.add(s);
        }
        serialized.add("members", members);
        return serialized;
    };

    public JsonDeserializer<Project> projectJsonDeserializer = (json, typeOfT, context) -> {
        JsonObject jsonObject = json.getAsJsonObject();
        JsonArray jsonArray = jsonObject.getAsJsonArray("members");
        ArrayList<String> members = new ArrayList<>();
        for(JsonElement e : jsonArray){
            members.add(e.getAsString());
        }
        return new Project(jsonObject.get("name").getAsString(), members, null, "/users/Vornao/Desktop/");
    };

    public JsonSerializer<User> userJsonSerializer = (user, type, jsonSerializationContext) -> {
        JsonObject serialized = new JsonObject();
        serialized.addProperty("name", user.getUsername());
        serialized.addProperty("password", user.getPassword());
        serialized.addProperty("salt", user.getSalt());
        return serialized;
    };

    public JsonDeserializer<User> deserializeUser = (json, typeOfT, context) -> {
        JsonObject jsonObject = json.getAsJsonObject();
        return new User(
                jsonObject.get("name").getAsString(),
                jsonObject.get("password").getAsString(),
                jsonObject.get("salt").getAsString());
    };

    public JsonSerializer<Card> serializeCard = (card, type, jsonSerializationContext) -> {
        JsonObject serialized = new JsonObject();
        serialized.addProperty("name", card.getName());
        serialized.addProperty("description", card.getDescription());
        serialized.addProperty("status", card.getStatus());
        JsonArray cardHistory = new JsonArray();
        for(CardEvent e : card.getCardHistory()){
            JsonObject event = new JsonObject();
            event.addProperty("from", e.getFrom());
            event.addProperty("to", e.getTo());
            event.addProperty("date", e.getDate());
            cardHistory.add(event);
        }
        serialized.add("history", cardHistory);
        return serialized;
    };

    public JsonDeserializer<Card> deserializeCard = (json, typeOfT, context) -> {
        JsonObject jsonCard = json.getAsJsonObject();
        ArrayList<CardEvent> cardEvents = new ArrayList<>();

        JsonArray jsonEvents = jsonCard.getAsJsonArray("history");
        for(JsonElement e : jsonEvents){
            JsonObject o = e.getAsJsonObject();
            CardEvent event = new CardEvent(o.get("date").getAsInt(), o.get("from").getAsString(), o.get("to").getAsString());
            cardEvents.add(event);
        }
        return new Card(jsonCard.get("name").getAsString(),
                jsonCard.get("description").getAsString(),
                jsonCard.get("status").getAsString(),
                cardEvents);
    };
}
