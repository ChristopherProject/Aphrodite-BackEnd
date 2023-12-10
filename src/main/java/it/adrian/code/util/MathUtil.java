package it.adrian.code.util;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

    public static String encrypt(final String base) {
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
        LocalDateTime dateTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0); // Anno, mese, giorno, ore, minuti, secondi
        long timestampSeconds = dateTime.toEpochSecond(ZoneOffset.UTC);
        return timestampSeconds;
    }

    public static boolean verifyPassword(String enteredPassword, String storedHashPassword) {
        String enteredPasswordHash = encrypt(enteredPassword);
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


    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String secretKey = "HmacSHA256";

    public static byte[] signateDocument(String input) {
        try{
            Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            hmac.init(secretKeySpec);
            return hmac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        }catch (Exception e){
            return null;
        }
    }

    public static boolean checkSignature(String input, byte[] signature)  {
        byte[] calculatedSignature = signateDocument(input);
        return MessageDigest.isEqual(calculatedSignature, signature);
    }
}