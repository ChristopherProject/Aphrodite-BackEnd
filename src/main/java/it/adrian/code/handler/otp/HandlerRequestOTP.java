package it.adrian.code.handler.otp;


import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Properties;

public class HandlerRequestOTP {//TODO: aggiungere al json utente dopo la chiamata un codice otp hashato, da rimuovere dopo il reset e da comparare prima di fare la richiesta per il cambio pass


    public static void main(String[] args) {
        String example = "12345";

    }

    public static void sendOTP(String username, String otpCode, String userMail) throws MessagingException {
        String body = username + " your otp code is " + otpCode + ", if you dosnt have request password reset ignore this email!";
        Properties prop = new Properties();
        prop.put("mail.debug", "false");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.ssl.protocols", "TLSv1.2");
        prop.put("mail.smtp.host", "ssl0.ovh.net");
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.ssl.trust", "ssl0.ovh.net");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.socketFactory.port", "587");
        prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("support@nobusware.it", "");
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("support@nobusware.it"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(userMail));
        message.setSubject("Aphrodite Password Reset");
        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(body, "text/html; charset=utf-8");
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);
        message.setContent(multipart);
        Transport.send(message);
    }
}