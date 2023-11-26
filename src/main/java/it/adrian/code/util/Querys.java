package it.adrian.code.util;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Querys {

    /***
     *
     * @param username account username (unique on database)
     * @param password account password (this was hashed in database)
     * @return this function return boolean to see if registration process work correctly.
     */
    public static boolean registerUser(String username, String password) {
        try (MongoClient mongoClient = MongoClients.create(Config.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(Config.DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(Config.COLLECTION_NAME);
            String randomId;
            do {
                randomId = MathUtil.generateRandomId();
            } while (MathUtil.isIdAlreadyUsed(collection, randomId));
            if (existed(username)) {
                return false;
            }
            Document userDocument = new Document().append("_id", randomId).append("username", username).append("hash_password", MathUtil.encrypt(password));
            collection.insertOne(userDocument);
        }
        return true;
    }

    /***
     *
     * @param username account username (unique on database)
     * @param password account password (this was hashed and checked in database)
     * @return this function return response data about login request (example it return jwt and some errors).
     */
    public static String authJWTLogin(String username, String password) {
        try (MongoClient mongoClient = MongoClients.create(Config.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(Config.DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(Config.COLLECTION_NAME);
            if (!existed(username)) {
                return "{\"error\": \"user isn't present in database\"}";
            }
            Document userDocument = collection.find(Filters.eq("username", username)).first();
            String storedHashPassword = Objects.requireNonNull(userDocument).getString("hash_password");
            if (MathUtil.verifyPassword(password, storedHashPassword)) {
                String data = "{\"state\": \"success\", \"username\": \"" + username + "\", \"hash_password\": \"" + storedHashPassword + "\"}";
                String jwt = Base64.getEncoder().encodeToString(data.getBytes());
                return "{\"token\": \"" + jwt + "\"}";
            } else {
                return "{\"error\": \"401 Unauthorized\"}";
            }
        } catch (Exception e) {
            return "{\"error\": \"500 Internal Server Error\"}";
        }
    }

    /***
     *
     * @param jwt your jwt token (basically json encoded using base64)
     * @return this function return boolean if is true your jwt token is valid else isn't valid.
     */
    public static boolean validateJWT(String jwt) {
        try (MongoClient mongoClient = MongoClients.create(Config.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(Config.DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(Config.COLLECTION_NAME);

            byte[] decodedBytes = Base64.getDecoder().decode(jwt.getBytes(StandardCharsets.UTF_8));
            String decodedJwt = new String(decodedBytes, StandardCharsets.UTF_8);

            JSONObject jsonObject = new JSONObject(decodedJwt);

            Document userDocument = collection.find(Filters.eq("hash_password", jsonObject.getString("hash_password"))).first();
            String storedUsername = Objects.requireNonNull(userDocument).getString("username");

            return storedUsername != null;
        } catch (Exception e) {
            return false;
        }
    }

    /***
     *
     * @param username account username (unique on database)
     * @param oldPassword account password you want changed (this was hashed and checked in database)
     * @param newPassword new account password you choose (this was hashed and updated in database after old password check)
     * @return this function was made to grant at user possibility to change his password.
     */
    public static boolean changePassword(String username, String oldPassword, String newPassword) {
        try (MongoClient mongoClient = MongoClients.create(Config.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(Config.DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(Config.COLLECTION_NAME);
            if (!existed(username)) {
                return false;
            }
            Document userDocument = collection.find(Filters.eq("username", username)).first();
            String storedHashPassword = Objects.requireNonNull(userDocument).getString("hash_password");
            if (!MathUtil.verifyPassword(oldPassword, storedHashPassword)) {
                return false;
            }
            collection.updateOne(Filters.eq("username", username), new Document("$set", new Document("hash_password", MathUtil.encrypt(newPassword))));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /***
     *
     * @param username account username (unique on database)
     * @return this function return boolean to see if user are existed in database.
     */
    private static boolean existed(String username) {
        try (MongoClient mongoClient = MongoClients.create(Config.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(Config.DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(Config.COLLECTION_NAME);
            Document query = new Document("username", username);
            FindIterable<Document> result = collection.find(query);
            return result.iterator().hasNext();
        }
    }

    /***
     *
     * @param userId this is your user id (every account have 1 id unique)
     * @return this function indicate your account data by id, it contains some information's like username, chat_id etc..
     */
    public static HashMap<String, String> findUserById(String userId) {
        final HashMap<String, String> user_information = new LinkedHashMap<>();
        try (MongoClient mongoClient = MongoClients.create(Config.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(Config.DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(Config.COLLECTION_NAME);

            Document query = new Document("_id", userId);
            FindIterable<Document> result = collection.find(query);

            if (result.iterator().hasNext()) {
                Document userDocument = result.iterator().next();
                user_information.put("user_id", userDocument.getString("_id"));
                user_information.put("username", userDocument.getString("username"));
                user_information.put("biography", userDocument.getString("biography"));
                user_information.put("profile_pic", userDocument.getString("profile_pic"));
                return user_information;
            } else {
                return null;
            }
        }
    }

    public static boolean updateBiography(String userId, String biography) {
        try (MongoClient mongoClient = MongoClients.create(Config.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(Config.DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(Config.COLLECTION_NAME);
            Document query = new Document("_id", userId);
            Document userDocument = collection.find(query).first();
            if (userDocument != null) {
                Bson update = Updates.set("biography", biography.replace("%20", " "));
                Bson filter = Filters.eq("_id", userId);
                collection.updateOne(filter, update);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean updateProfilePhoto(String userId, String profile_pic) {
        try (MongoClient mongoClient = MongoClients.create(Config.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(Config.DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(Config.COLLECTION_NAME);
            Document query = new Document("_id", userId);
            Document userDocument = collection.find(query).first();
            if (userDocument != null) {
                Bson update = Updates.set("profile_pic", profile_pic);
                Bson filter = Filters.eq("_id", userId);
                collection.updateOne(filter, update);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /***
     *
     * @param username account username (unique on database)
     * @return this function return your account data by username, it contains some information's like username, profile_photo, chat_id etc..
     */
    public static HashMap<String, String>  findUserByUsername(String username) {
        final HashMap<String, String> user_information = new LinkedHashMap<>();
        try (MongoClient mongoClient = MongoClients.create(Config.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(Config.DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(Config.COLLECTION_NAME);

            Document query = new Document("username", username);
            FindIterable<Document> result = collection.find(query);

            if (result.iterator().hasNext()) {
                Document userDocument = result.iterator().next();
                user_information.put("user_id", userDocument.getString("_id"));
                user_information.put("username", userDocument.getString("username"));
                user_information.put("biography", userDocument.getString("biography"));
                user_information.put("profile_pic", userDocument.getString("profile_pic"));
                return user_information;
            } else {
                return null;
            }
        }
    }

    public static Map<String, String> parseURLQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query != null)
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                String key;
                try {
                    key = URLDecoder.decode(pair[0], String.valueOf(StandardCharsets.UTF_8));
                    String value = (pair.length > 1) ? URLDecoder.decode(pair[1], String.valueOf(StandardCharsets.UTF_8)) : "";
                    result.put(key, value);
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        return result;
    }
}