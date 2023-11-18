package io.marcnowakowski;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

public class Wichtler {

    private static Properties mailProp;
    private static Properties config;
    private static Session session;

    public static void main(String[] args) throws MessagingException, IOException {
        boolean isProductive = Boolean.parseBoolean(args[0]);

        HashMap<String, String> namesToMailsMap = parseAndMapParticipants();

        ArrayList<String> nameList1 = new ArrayList<>(namesToMailsMap.keySet());
        ArrayList<String> nameList2 = new ArrayList<>(namesToMailsMap.keySet());

        setupNewSession();

        Random random = new Random();
        ArrayList<MimeMessage> messages = new ArrayList<>();
        while(!nameList1.isEmpty()) {

            int i = random.nextInt(nameList1.size());
            int j = random.nextInt(nameList2.size());

            while(i == j && nameList1.size()-1 != 0) {
                j = random.nextInt(nameList2.size());
            }

            String firstName = nameList1.get(i);
            String secondName = nameList2.get(j);
            String mail = namesToMailsMap.get(secondName);

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.getProperty("email.address")));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mail));
            message.setSubject(String.format("Wichteln %s", LocalDateTime.now().getYear()));

            String msg = String.format("Moin, dein Wichtelpartner ist leider %s. \n Behalte deinen Wichtelpartner für dich (Shoutout Pfand). \n Limit ist 25€, btw. ", firstName);

            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(msg, "text/html; charset=utf-8");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);
            message.setContent(multipart);

            messages.add(message);

            nameList1.remove(firstName);
            nameList2.remove(secondName);
        }

        for(MimeMessage m : messages) {
            Address recipient = m.getRecipients(Message.RecipientType.TO)[0];
            Multipart content = (MimeMultipart) m.getContent();
            MimeBodyPart bodyPart = (MimeBodyPart) content.getBodyPart(0);
            String msg = bodyPart.getContent().toString();

            if(isProductive) {
                Transport.send(m);
                System.out.println("Finished");
            } else {
                System.out.printf("Sending mail to %s with text message: %s%n", recipient, msg);
            }
        }
    }

    private static void setupNewSession() {

        mailProp = new Properties();
        config = new Properties();

        try {
            mailProp.load(ClassLoader.getSystemResourceAsStream("mail.properties"));
            config.load(ClassLoader.getSystemResourceAsStream("config.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        session = Session.getInstance(mailProp, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getProperty("email.address"), config.getProperty("password"));
            }
        });
    }

    private static HashMap<String, String> parseAndMapParticipants() {
        HashMap<String, String> nameToMailMap = new HashMap<>();

        try {
            File file = new File("src/main/resources/participants.txt");
            Scanner reader = new Scanner(file);

            while(reader.hasNextLine()) {
                String[] split = reader.nextLine().split(";");
                nameToMailMap.put(split[0], split[1]);
            }
            return nameToMailMap;

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
