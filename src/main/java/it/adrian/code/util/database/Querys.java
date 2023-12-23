package it.adrian.code.util.database;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.*;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.GridFSUploadStream;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import it.adrian.code.util.encryption.Encryption;
import it.adrian.code.util.json.JSON;
import it.adrian.code.util.math.MathUtil;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
            MongoCollection<Document> collection = database.getCollection(Config.USER_COLLECTION_NAME);
            String randomId;
            do {
                randomId = MathUtil.generateRandomId();
            } while (MathUtil.isIdAlreadyUsedInCollection(collection, randomId));
            if (uniqueUserIDCheck(username)) {
                return false;
            }
            Document userDocument = new Document().append("_id", randomId).append("username", username).append("hash_password", Encryption.encryptPassword(password));
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
            MongoCollection<Document> collection = database.getCollection(Config.USER_COLLECTION_NAME);
            if (!uniqueUserIDCheck(username)) {
                return "{\"error\": \"user isn't present in database\"}";
            }
            Document userDocument = collection.find(Filters.eq("username", username)).first();
            String storedHashPassword = Objects.requireNonNull(userDocument).getString("hash_password");
            if (Encryption.verifyPassword(password, storedHashPassword)) {
                long renewal = MathUtil.secondsUnixTimeStamp();
                long expiration = (renewal + 7000);
                String header = "{\"certificate\":\"Aphrodite\",\"type\":\"JWT\", \"version\": \"1.0\"" + ",\"delivered\": " + renewal + "}";
                String data = "{\"state\": \"success\", \"username\": \"" + username + "\", \"hash_password\": \"" + storedHashPassword + "\"}";
                String jwt = Base64.getEncoder().encodeToString(data.getBytes());
                String session = Encryption.signateDocument("{\"session\":\"" + jwt + "\"," + "\"renewal\": " + renewal + "," + "\"serial\": " + findUserByUsername(username).get("user_id") + "," + "\"expiration\": " + expiration + "}");
                String jsonSigned = "{\"accessToken\": \"" + Base64.getEncoder().encodeToString(session.getBytes()) + "\", \"signature\": \"" + Encryption.signateDocument(header) + "\"}";
                return "{\"authToken\": \"" + Base64.getEncoder().encodeToString(jsonSigned.getBytes()) + "\"}";
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
            MongoCollection<Document> collection = database.getCollection(Config.USER_COLLECTION_NAME);
            long now = MathUtil.secondsUnixTimeStamp();
            byte[] decodedBytes = Base64.getDecoder().decode(jwt.getBytes(StandardCharsets.UTF_8));
            String decodedJwt = new String(decodedBytes, StandardCharsets.UTF_8);
            JsonNode jsonObject = JSON.parseStringToJson(decodedJwt);
            String signature = jsonObject.get("signature").asText();
            if (signature != null) {
                JsonNode signatureHeadersJson = JSON.parseStringToJson(Encryption.readSignature(signature));
                long creationTime = signatureHeadersJson.get("delivered").asLong();
                byte[] decoderJsonSigned = Base64.getDecoder().decode(jsonObject.get("accessToken").asText().getBytes(StandardCharsets.UTF_8));
                String jsonSignedEncrypt = new String(decoderJsonSigned, StandardCharsets.UTF_8);
                String jsonSignedDecoded = Encryption.readSignature(jsonSignedEncrypt);
                JsonNode sessionDataJson = JSON.parseStringToJson(jsonSignedDecoded);
                if (sessionDataJson.has("renewal") && sessionDataJson.has("expiration")) {
                    long renewal = sessionDataJson.get("renewal").asLong();
                    long expiration = sessionDataJson.get("expiration").asLong();
                    long diff = now - creationTime;
                    if (!(diff > expiration)) {
                        boolean validate = MathUtil.isTokenExpiredTime(now, renewal, expiration);
                        if (validate) {
                            String realJWT = sessionDataJson.get("session").asText();
                            byte[] decoderJwtJson = Base64.getDecoder().decode(realJWT.getBytes(StandardCharsets.UTF_8));
                            String jwtJsonDecoded = new String(decoderJwtJson, StandardCharsets.UTF_8);
                            JsonNode json = JSON.parseStringToJson(jwtJsonDecoded);
                            Document userDocument = collection.find(Filters.eq("hash_password", json.get("hash_password").asText())).first();
                            String storedUsername = Objects.requireNonNull(userDocument).getString("username");
                            return storedUsername != null;
                        }
                    }
                }
            }
            return false;
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
            MongoCollection<Document> collection = database.getCollection(Config.USER_COLLECTION_NAME);
            if (!uniqueUserIDCheck(username)) {
                return false;
            }
            Document userDocument = collection.find(Filters.eq("username", username)).first();
            String storedHashPassword = Objects.requireNonNull(userDocument).getString("hash_password");
            if (!Encryption.verifyPassword(oldPassword, storedHashPassword)) {
                return false;
            }
            collection.updateOne(Filters.eq("username", username), new Document("$set", new Document("hash_password", Encryption.encryptPassword(newPassword))));
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
    private static boolean uniqueUserIDCheck(String username) {
        try (MongoClient mongoClient = MongoClients.create(Config.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(Config.DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(Config.USER_COLLECTION_NAME);
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
            MongoCollection<Document> collection = database.getCollection(Config.USER_COLLECTION_NAME);

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

    /***
     *
     * @param userId this is your user id (every account have 1 id unique)
     * @param biography this is a biography of your profile (every profile can have 1)
     * @return this function return boolean about update profile biography process working correctly.
     */
    public static boolean updateBiography(String userId, String biography) {
        try (MongoClient mongoClient = MongoClients.create(Config.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(Config.DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(Config.USER_COLLECTION_NAME);
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

    /***
     *
     * @param userId this is your user id (every account have 1 id unique)
     * @param profile_pic this is path of your profile pic choose
     * @return this function return boolean about update profile photo process working correctly.
     */
    public static boolean updateProfilePhoto(String userId, String profile_pic) {
        try (MongoClient mongoClient = MongoClients.create(Config.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(Config.DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(Config.USER_COLLECTION_NAME);
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
    public static HashMap<String, String> findUserByUsername(String username) {
        final HashMap<String, String> user_information = new LinkedHashMap<>();
        try (MongoClient mongoClient = MongoClients.create(Config.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(Config.DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(Config.USER_COLLECTION_NAME);

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

    /***
     *
     * @param fromID basically message id of mitten
     * @param toID basically message id destination
     * @param message this is message sent from user to user
     * @param timestamp this is an unix timestamp of message UTC
     * @return this function return boolean true when message sent.
     */
    public static boolean sendMessage(String fromID, String toID, String message, String timestamp) {
        try (MongoClient mongoClient = MongoClients.create(Config.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(Config.DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(Config.MESSAGE_COLLECTION_NAME);
            String randomId;
            do {
                randomId = MathUtil.generateRandomId();
            } while (MathUtil.isIdAlreadyUsedInCollection(collection, randomId));
            Document userDocument = new Document().append("_id", randomId).append("from", fromID).append("to", toID).append("message", message.replace("%20", " ")).append("timestamp", timestamp);
            collection.insertOne(userDocument);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /***
     *
     * @param messageID this id indicate current message (every message have unique)
     * @return this function returned all data of message by id in one hashmap.
     */
    public static HashMap<String, String> getMessageByID(String messageID) {
        final HashMap<String, String> user_information = new LinkedHashMap<>();
        try (MongoClient mongoClient = MongoClients.create(Config.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(Config.DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(Config.MESSAGE_COLLECTION_NAME);

            Document query = new Document("_id", messageID);
            FindIterable<Document> result = collection.find(query);

            if (result.iterator().hasNext()) {
                Document userDocument = result.iterator().next();
                user_information.put("message_id", userDocument.getString("_id"));
                user_information.put("from", userDocument.getString("from"));
                user_information.put("to", userDocument.getString("to"));
                user_information.put("timestamp", userDocument.getString("timestamp"));
                return user_information;
            } else {
                return null;
            }
        }
    }

    /***
     *
     * @param userID this data are your user_id (this data is unique for every account)
     * @param chatID this data represent other account user_id (unique data)
     * @return this function returned correspondence between two users.
     */
    public static List<JsonNode> getMessagesBetweenUsers(String userID, String chatID) {
        final List<JsonNode> messages = new ArrayList<>();
        try (MongoClient mongoClient = MongoClients.create(Config.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(Config.DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(Config.MESSAGE_COLLECTION_NAME);
            Document query = new Document("$or",
                    Arrays.asList(
                            new Document("from", userID).append("to", chatID),
                            new Document("from", chatID).append("to", userID),
                            new Document("from", userID).append("to", userID),
                            new Document("from", chatID).append("to", chatID)
                    )
            );
            FindIterable<Document> result = collection.find(query);
            List<Document> allResults = StreamSupport.stream(result.spliterator(), false).collect(Collectors.toList());
            for (Document userDocument : allResults) {
                messages.add(JSON.parseStringToJson(userDocument.toJson()));
            }
        } catch (Exception e) {
            return null;
        }
        return messages;
    }

    /***
     *
     * @param messageID this id indicate current message (every message have unique)
     * @param newMessage this indicates new message to replace current message value
     * @return this function return boolean to change current message text successfully.
     */
    public static boolean updateMessage(String messageID, String newMessage) {
        try (MongoClient mongoClient = MongoClients.create(Config.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(Config.DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(Config.MESSAGE_COLLECTION_NAME);
            Document query = new Document("_id", messageID);
            Document userDocument = collection.find(query).first();
            if (userDocument != null) {
                Bson message = Updates.set("message", newMessage.replace("%20", " "));
                Bson timestamp = Updates.set("timestamp", MathUtil.getUnixTimestampEpoch());
                Bson filter = Filters.eq("_id", messageID);
                List<Bson> updates = new LinkedList<>();
                updates.add(message);
                updates.add(timestamp);
                collection.updateMany(filter, updates);
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
     * @param messageID this id indicate current message (every message have unique)
     * @return this function return boolean to check deletion successfully of current message.
     */
    public static boolean deleteMessage(String messageID) {
        try (MongoClient mongoClient = MongoClients.create(Config.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(Config.DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(Config.MESSAGE_COLLECTION_NAME);
            Bson query = Filters.eq("_id", messageID);
            DeleteResult result = collection.deleteOne(query);
            return result.getDeletedCount() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /***
     *
     * @param userId this is your user id (every account has a unique id)
     * @param file the file to be inserted
     * @param fileName the name of the file
     * @param contentType the content type of the file (e.g., "image/jpeg", "application/pdf")
     * @return this function return boolean of uploading media from user successfully.
     */
    public static boolean uploadMedia(String userId, File file, String fileName, String contentType) {
        try (MongoClient mongoClient = MongoClients.create(Config.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(Config.DATABASE_NAME);
            GridFSBucket gridFSBucket = GridFSBuckets.create(database, Config.MEDIA_COLLECTION_NAME);
            ObjectId fileId = saveFileToGridFS(gridFSBucket, file, fileName, contentType);
            String mediaId;
            do {
                mediaId = MathUtil.generateRandomId();
            } while (MathUtil.isIdAlreadyUsedInCollection(database.getCollection(Config.MEDIA_COLLECTION_NAME), mediaId));
            if (uniqueMediaIDCheck(mediaId)) {
                return false;
            }
            Document mediaDocument = new Document("_id", new ObjectId())
                    .append("user_id", userId)
                    .append("file_data_id", fileId)
                    .append("file_name", fileName)
                    .append("content_type", contentType);
            database.getCollection(Config.MEDIA_COLLECTION_NAME).insertOne(mediaDocument);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /***
     *
     * @param file_data_id id of file to read.
     * @return it returned a byte array from object id of file in medias collection.
     */
    public static byte[] readFileBytesFromMediaCollection(ObjectId file_data_id) {
        try (MongoClient mongoClient = MongoClients.create(Config.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(Config.DATABASE_NAME);
            GridFSBucket gridFSBucket = GridFSBuckets.create(database, Config.MEDIA_COLLECTION_NAME);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            GridFSDownloadStream downloadStream = gridFSBucket.openDownloadStream(file_data_id);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = downloadStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            downloadStream.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    /***
     *
     * @param mediaID account username (unique on database)
     * @return this function return boolean to see if media id existed in database.
     */
    private static boolean uniqueMediaIDCheck(String mediaID) {
        try (MongoClient mongoClient = MongoClients.create(Config.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(Config.DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(Config.MEDIA_COLLECTION_NAME);
            Document query = new Document("_id", mediaID);
            FindIterable<Document> result = collection.find(query);
            return result.iterator().hasNext();
        }
    }

    /***
     *
     * @param from (basically message id of mitten)
     * @param messageID (basically message id of current message)
     * @param content this field representing current reply message
     * @param timestamp this is an unix timestamp of message UTC
     * @return this function return boolean to check replies of current message was updated successful.
     */
    public static boolean replyToMessage(String from, String messageID, String content, String timestamp) {
        try (MongoClient mongoClient = MongoClients.create(Config.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(Config.DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(Config.MESSAGE_COLLECTION_NAME);
            Document query = new Document("_id", messageID);
            FindIterable<Document> result = collection.find(query);
            if (result.iterator().hasNext()) {
                Document messageDocument = result.iterator().next();
                if (messageDocument != null) {
                    List<Document> replies = messageDocument.getList("replies", Document.class);
                    if (replies == null) replies = new ArrayList<>();
                    String replyId = MathUtil.generateRandomId();
                    Document replyDocument = new Document("reply_id", replyId).append("from", findUserById(from).get("id")).append("content", content).append("timestamp", timestamp);
                    String finalReplyId = replyId;
                    while (replies.stream().anyMatch(doc -> doc.getString("reply_id").equals(finalReplyId))) {
                        replyId = MathUtil.generateRandomId();
                        replyDocument.put("reply_id", replyId);
                    }
                    replies.add(replyDocument);
                    Bson update = Updates.set("replies", replies);
                    Bson filter = Filters.eq("_id", messageID);
                    collection.updateOne(filter, update);
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    /***
     *
     * @param messageID (basically message id of current message)
     * @param replyID this is a reply id of current message (every reply has a unique id)
     * @return this function return reply content.
     */
    public static Document getReplyById(String messageID, String replyID) {
        try (MongoClient mongoClient = MongoClients.create(Config.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(Config.DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(Config.MESSAGE_COLLECTION_NAME);
            Document query = new Document("_id", messageID);
            FindIterable<Document> result = collection.find(query);
            if (result.iterator().hasNext()) {
                Document messageDocument = result.iterator().next();
                if (messageDocument != null) {
                    List<Document> replies = messageDocument.getList("replies", Document.class);
                    if (replies != null) {
                        return replies.stream().filter(reply -> reply.getString("reply_id").equals(replyID)).findFirst().orElse(null);
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /***
     *
     * @param messageID (basically message id of current message)
     * @return this function return all replies for message.
     */
    public static List<Document> getRepliesByMessageID(String messageID) {
        try (MongoClient mongoClient = MongoClients.create(Config.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(Config.DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(Config.MESSAGE_COLLECTION_NAME);
            Document query = new Document("_id", messageID);
            FindIterable<Document> result = collection.find(query);
            if (result.iterator().hasNext()) {
                Document messageDocument = result.iterator().next();
                if (messageDocument != null) {
                    List<Document> replies = messageDocument.getList("replies", Document.class);
                    if (replies != null) {
                        return replies;
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    //GridFS Helper method to save a file in collection.
    private static ObjectId saveFileToGridFS(GridFSBucket gridFSBucket, File file, String fileName, String contentType) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            GridFSUploadStream uploadStream = gridFSBucket.openUploadStream(fileName, new GridFSUploadOptions().metadata(new Document("contentType", contentType)));
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                uploadStream.write(buffer, 0, bytesRead);
            }
            uploadStream.close();
            return uploadStream.getObjectId();
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