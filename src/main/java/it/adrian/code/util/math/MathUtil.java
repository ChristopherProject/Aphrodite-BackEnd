package it.adrian.code.util.math;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Random;

public class MathUtil {


    public static String generateRandomId() {
        Random random = new Random();
        int randomInt = 1000000 + random.nextInt(10000000);
        return Integer.toString(randomInt);
    }

    public static boolean isIdAlreadyUsed(MongoCollection<Document> collection, String randomId) {//andrebbe fatta la stessa cosa anche per il check degli username (al momento hard coded)
        Document query = new Document("_id", randomId);
        FindIterable<Document> result = collection.find(query);
        return result.iterator().hasNext();
    }

    public static String encryptPassword(final String base) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            final StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                final String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static long secondsUnixTimeStamp() {
        return Instant.now().getEpochSecond();
    }

    public static boolean verifyPassword(String enteredPassword, String storedHashPassword) {
        String enteredPasswordHash = encryptPassword(enteredPassword);
        return enteredPasswordHash.equals(storedHashPassword);
    }

    public static String getUnixTimestampEpoch() {
        long currentTimeMillis = System.currentTimeMillis();
        long epochTimeSeconds = currentTimeMillis / 1000;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(epochTimeSeconds * 1000);
        String formattedTime = sdf.format(date);
        return formattedTime;
    }

    public static boolean isTokenExpiredTime(long now, long renewal, long expiration) {
        return now >= renewal && now <= expiration;
    }
}