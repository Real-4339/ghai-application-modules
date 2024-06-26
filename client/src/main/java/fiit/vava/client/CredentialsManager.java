package fiit.vava.client;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CredentialsManager {

    /*
     * TODO rewrite to use Dotenv
     */
    private static final String CREDENTIALS_FILE = System.getProperty("user.home") + "/.ghai/credentials";

    private static final Logger logger = LoggerFactory.getLogger("client." + CredentialsManager.class);

    public static void storeCredentials(String email, String password) {
        try {
            File directory = new File(System.getProperty("user.home") + "/.ghai");
            if (!directory.exists())
                directory.mkdirs();

            BufferedWriter writer = new BufferedWriter(new FileWriter(CREDENTIALS_FILE));
            writer.write(email);
            writer.newLine();
            writer.write(password);
            writer.close();

        } catch (IOException e) {
            logger.warn("Unable to store credentials" + e);
        }
    }

    public static String[] retrieveCredentials() {
        try {
            Path path = Paths.get(CREDENTIALS_FILE);
            if (!Files.exists(path))
                return null;

            BufferedReader reader = Files.newBufferedReader(path);
            String email = reader.readLine();
            String password = reader.readLine();
            reader.close();

            return new String[]{email, password};
        } catch (IOException e) {
            logger.warn("Unable to retrieve credentials" + e);
            return null;
        }
    }

    public static boolean removeCredentials() {
        try {
            Path path = Paths.get(CREDENTIALS_FILE);
            Files.deleteIfExists(path);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}