package concept.utility;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class FileUtility {

    public static boolean writeFile(String pathString, byte[] data) {

        if (pathString == null || data == null) {
            return false;
        }

        try {
            File file = new File(pathString);
            file.getParentFile().mkdirs();
            FileOutputStream fileOutputStream;
            fileOutputStream = new FileOutputStream(file, false);
            fileOutputStream.write(data);
            fileOutputStream.close();
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public static byte[] readFile(String pathString) {
        byte[] result = null;

        if (pathString == null || pathString.length() == 0) {
            return null;
        }

        Path path = Paths.get(pathString);

        if (!Files.exists(path)) {
            return null;
        }

        try {
            InputStream inputStream = Files.newInputStream(Paths.get(pathString));
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); // Java 8

            byte[] buffer = new byte[1024];
            int length;

            while ((length = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }

            result = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();
            inputStream.close();

        } catch (Exception ignored) {}

        return result;
    }

    public static JSONObject jsonObjectReadFile(String pathString) {

        byte[] bytes = readFile(pathString);

        if (bytes == null || bytes.length < 2) {
            return null;
        }

        JSONParser jsonParser = new JSONParser();

        JSONObject result;

        try {
            result = (JSONObject) jsonParser.parse(new String(bytes));
        } catch (Exception e) {
            return null;
        }

        return result;
    }

    public static byte[] getHash(String pathString, String algorithm) {

        if (algorithm == null || algorithm.length() == 0) {
            return null;
        }

        byte[] data = readFile(pathString);

        if (data == null) {
            return null;
        }

        MessageDigest messageDigest;

        try {
            messageDigest = MessageDigest.getInstance(algorithm);
        } catch ( NoSuchAlgorithmException e) {
            return null;
        }

        return messageDigest.digest(data);
    }

    public static boolean verifyHash(String pathString, byte[] hash, String algorithm) {

        if (hash == null || hash.length == 0) {
            return false;
        }

        byte[] data = getHash(pathString, algorithm);

        if (data == null || data.length != hash.length) {
            return false;
        }

        return Arrays.equals(data, hash);
    }
}
