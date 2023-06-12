package concept.omno.service.PlatformTokenExchangeById;

import concept.omno.ApplicationContext;
import concept.omno.object.PlatformToken;
import concept.utility.FileUtility;
import concept.utility.JsonFunction;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Event implements Cloneable {

    ApplicationContext applicationContext;

    public long id = -1;
    public long offerId = -1;

    public long accountTake = 0;
    public long accountGive = 0;
    public long multiplier = 0;

    public PlatformToken give = new PlatformToken();
    public PlatformToken take = new PlatformToken();

    public void define(JSONObject jsonObject) {
        if (jsonObject == null) {
            return;
        }

        id = JsonFunction.getLongFromStringUnsigned(jsonObject, "id", 1);
        offerId = JsonFunction.getLongFromStringUnsigned(jsonObject, "offerId", -1);

        accountGive = JsonFunction.getLongFromStringUnsigned(jsonObject, "accountGive", 0);
        accountTake = JsonFunction.getLongFromStringUnsigned(jsonObject, "accountTake", 0);
        multiplier = JsonFunction.getLongFromStringUnsigned(jsonObject, "multiplier", 0);

        give = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "give", null));
        take = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "take", null));
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();

        JsonFunction.put(jsonObject, "id", Long.toUnsignedString(id));
        JsonFunction.put(jsonObject, "offerId", Long.toUnsignedString(offerId));

        JsonFunction.put(jsonObject, "accountGive", Long.toUnsignedString(accountGive));
        JsonFunction.put(jsonObject, "accountTake", Long.toUnsignedString(accountTake));
        JsonFunction.put(jsonObject, "multiplier", Long.toUnsignedString(multiplier));

        JsonFunction.put(jsonObject, "give", give.toJSONObject());
        JsonFunction.put(jsonObject, "take", take.toJSONObject());

        return jsonObject;
    }

    public Event(ApplicationContext applicationContext, long id, long offerId, long accountGive, long accountTake, PlatformToken give, PlatformToken take, long multiplier) {

        if (give == null || !give.isValid() || take == null || !take.isValid() || multiplier <= 0) {
            return;
        }

        this.applicationContext = applicationContext;

        this.id = id;
        this.offerId = offerId;

        this.accountGive = accountGive;
        this.accountTake = accountTake;

        this.give = give;
        this.take = take;

        this.multiplier = multiplier;
    }

    Event(ApplicationContext applicationContext, JSONObject jsonObject) {
        this.applicationContext = applicationContext;
        define(jsonObject);
    }

    public Event clone() {
        final Event clone;

        try {
            clone = (Event) super.clone();
        } catch (CloneNotSupportedException e) {
            throw  new RuntimeException();
        }

        clone.applicationContext = applicationContext;

        clone.id = id;
        clone.offerId = offerId;

        clone.accountGive = accountGive;
        clone.accountTake = accountTake;

        clone.give = give.clone();
        clone.take = take.clone();

        clone.multiplier = multiplier;

        return clone;
    }

    public boolean write (Path pathDirectory, Path path, JSONObject jsonObject) {

//        Path pathDirectory = getSaveDirectoryById(applicationContext, id);

        try {
            Files.createDirectories(pathDirectory);
        } catch (IOException e) {
            applicationContext.logErrorMessage("WARNING : could not create directories for save event : " + pathDirectory);
            return false;
        }

//        Path path = getEventSaveName(applicationContext, id);

        FileOutputStream fileOutputStream;

        try {
            fileOutputStream = new FileOutputStream(path.toString());
            fileOutputStream.write(jsonObject.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            applicationContext.logErrorMessage("WARNING : could not save event : " + path);
            applicationContext.logErrorMessage(e.toString());
            return false;
        }

        return true;
    }
    public boolean save(boolean writeLast) {

        JSONObject jsonObject = toJSONObject();

        if (!write (getSaveDirectoryById(applicationContext, id), getEventSaveNameById(applicationContext, id), jsonObject)) {
            return false;
        }

        if (!write (getSaveDirectoryByAccount(applicationContext, accountGive, id), getEventSaveNameByAccount(applicationContext, accountGive, id), jsonObject)) {
            return false;
        }

        if (writeLast) {
            if (!write(getSaveDirectoryByIdRoot(applicationContext), getEventSaveNameIdLast(applicationContext), jsonObject)) {
                return false;
            }

            if (!write(getSaveDirectoryByAccountRoot(applicationContext, accountGive), getEventSaveNameAccountLast(applicationContext, accountGive), jsonObject)) {
                return false;
            }
        }

        return true;
    }

    public static Event loadEventById(ApplicationContext applicationContext, long id) {

        if (applicationContext == null || !applicationContext.isConfigured || id < 1) {
            return null;
        }

        Path path = getEventSaveNameById(applicationContext, id);

        JSONObject jsonObject = FileUtility.jsonObjectReadFile(path.toString());

        if (jsonObject == null) {
            return null;
        }

        return new Event(applicationContext, jsonObject);
    }

    public static Path getSaveDirectoryById(ApplicationContext applicationContext, long id) {

        long z = 100;
        long a = id % z;
        long b = (id / z) % z;
        return Paths.get(getSaveDirectoryByIdRoot(applicationContext) + "/" + a + "/" + b);
    }

    public static Path getSaveDirectoryByAccount(ApplicationContext applicationContext, long account, long id) {

        long z = 100;
        long a = id % z;
        long b = (id / z) % z;
        return Paths.get(getSaveDirectoryByAccountRoot(applicationContext, account) + "/" + a + "/" + b);
    }

    public static Path getSaveDirectoryByIdRoot(ApplicationContext applicationContext) {
        return Paths.get(applicationContext.stateRootDirectory.toString() + "/trade/id");
    }

    public static Path getSaveDirectoryByAccountRoot(ApplicationContext applicationContext, long account) {
        return Paths.get(applicationContext.stateRootDirectory.toString() + "/trade/account/" + Long.toUnsignedString(account) + "/");
    }

    public static Path getEventSaveNameById(ApplicationContext applicationContext, long id) {
        return Paths.get(getSaveDirectoryById(applicationContext, id) + "/trade.event." + Long.toUnsignedString(id) + ".json");
    }

    public static Path getEventSaveNameByAccount(ApplicationContext applicationContext, long account, long id) {
        return Paths.get(getSaveDirectoryByAccount(applicationContext, account, id) + "/trade.event." + Long.toUnsignedString(id) + ".json");
    }
    public static Path getEventSaveNameIdLast(ApplicationContext applicationContext) {
        return Paths.get(getSaveDirectoryByIdRoot(applicationContext) + "/trade.event.last.json");
    }

    public static Path getEventSaveNameAccountLast(ApplicationContext applicationContext, long id) {
        return Paths.get(getSaveDirectoryByAccountRoot(applicationContext, id) + "/trade.event.last.json");
    }

    public static JSONObject eventLoad(ApplicationContext applicationContext, long id) {

        Path path = getEventSaveNameById(applicationContext, id);

        try {
            InputStream inputStream = Files.newInputStream(Paths.get(path.toString()));
            JSONParser jsonParser = new JSONParser();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); // Java 8

            byte[] buffer = new byte[1024];
            int length;

            while ((length = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }

            String inputString = byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());

            return (JSONObject) jsonParser.parse(inputString);

        } catch (Exception e) {
            applicationContext.logErrorMessage("WARNING : could not load event : " + path);
            applicationContext.logErrorMessage(e.toString());
            return null;
        }
    }
}
