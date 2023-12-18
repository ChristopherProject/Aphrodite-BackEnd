package it.adrian.code.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedList;
import java.util.stream.IntStream;

public class Encryption {

    private static final String SECRET_KEY_HEX = "B374A26A71490437";
    private static final int IV_SIZE = 16;

    public static String signateDocument(String input) {
        LinkedList<String> stored = new LinkedList<>();
        try {
            byte[] encrypted = encrypt(input, SECRET_KEY_HEX);
            for (byte b : encrypted) stored.add(String.format("%02X", b));
            if (stored.isEmpty()) return null;
            String result = stored.toString();
            return Base64.getEncoder().encodeToString(result.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String readSignature(String input) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(input.getBytes(StandardCharsets.UTF_8));
            String encryptedText = new String(decodedBytes, StandardCharsets.UTF_8);
            String[] bytes = encryptedText.replace("[", "").replace("]", "").split(",");
            int length = bytes.length;
            byte[] byteArray = new byte[length];
            IntStream.range(0, length).forEachOrdered(i -> byteArray[i] = (byte) Integer.parseInt(bytes[i].replace(" ", ""), 16));
            return decrypt(byteArray, SECRET_KEY_HEX);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static byte[] encrypt(String input, String key) throws Exception {
        byte[] clean = input.getBytes();
        byte[] iv = new byte[IV_SIZE];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(key.getBytes(StandardCharsets.UTF_8));
        byte[] keyBytes = new byte[16];
        System.arraycopy(digest.digest(), 0, keyBytes, 0, keyBytes.length);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
        byte[] encrypted = cipher.doFinal(clean);
        byte[] encryptedIVAndText = new byte[IV_SIZE + encrypted.length];
        System.arraycopy(iv, 0, encryptedIVAndText, 0, IV_SIZE);
        System.arraycopy(encrypted, 0, encryptedIVAndText, IV_SIZE, encrypted.length);
        return Base64.getEncoder().encode(encryptedIVAndText);
    }

    public static String decrypt(byte[] base64, String key) throws Exception {
        byte[] fresh = Base64.getDecoder().decode(base64);
        byte[] iv = new byte[IV_SIZE];
        System.arraycopy(fresh, 0, iv, 0, iv.length);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        int encryptedSize = fresh.length - IV_SIZE;
        byte[] encryptedBytes = new byte[encryptedSize];
        System.arraycopy(fresh, IV_SIZE, encryptedBytes, 0, encryptedSize);
        byte[] keyBytes = new byte[IV_SIZE];
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(key.getBytes());
        System.arraycopy(md.digest(), 0, keyBytes, 0, keyBytes.length);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
        Cipher cipherDecrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipherDecrypt.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
        byte[] decrypted = cipherDecrypt.doFinal(encryptedBytes);
        return new String(decrypted);
    }
}
