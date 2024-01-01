package it.adrian.code.util.math;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Random;

public class MathUtil {

    public static String generateRandomId() {
        Random random = new Random();
        int randomInt = 1000000 + random.nextInt(10000000);
        return Integer.toString(randomInt);
    }

    public static long secondsUnixTimeStamp() {
        return Instant.now().getEpochSecond();
    }

    public static String getUnixTimestampEpoch() {
        long currentTimeMillis = System.currentTimeMillis();
        long epochTimeSeconds = currentTimeMillis / 1000;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(epochTimeSeconds * 1000);
        String formattedTime = sdf.format(date);
        return formattedTime;
    }

    public static boolean isIdAlreadyUsedInCollection(MongoCollection<Document> collection, String randomId) {
        Document query = new Document("_id", randomId);
        FindIterable<Document> result = collection.find(query);
        return result.iterator().hasNext();
    }

    public static boolean isTokenExpiredTime(long now, long renewal, long expiration) {
        return now >= renewal && now <= expiration;
    }

    public static String formatPhoneNumber(String numeroTelefono) {
        StringBuilder stringBuilder = new StringBuilder(numeroTelefono);
        stringBuilder.insert(3, ' ');
        stringBuilder.insert(7, ' ');
        stringBuilder.insert(11, ' ');
        return stringBuilder.toString();
    }
}