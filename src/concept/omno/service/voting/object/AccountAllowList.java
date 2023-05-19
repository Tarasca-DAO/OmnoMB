package concept.omno.service.voting.object;

import concept.utility.JsonFunction;
import org.json.simple.JSONObject;

import java.util.HashSet;

public class AccountAllowList {
    public int id;
    int signatureAlgorithm = 1;
    HashSet<String> publicKey = new HashSet<>();

    public AccountAllowList(int id, int signatureAlgorithm) {
        this.id = id;
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public AccountAllowList(JSONObject jsonObject) {
        define(jsonObject);
    }

    public boolean isValid() {
        return (id > 0 && signatureAlgorithm > 0 && signatureAlgorithm <= 1);
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();

        JsonFunction.put(jsonObject, "id", id);
        JsonFunction.put(jsonObject, "signatureAlgorithm", signatureAlgorithm);

        if (publicKey != null && publicKey.size() != 0) {
            JsonFunction.put(jsonObject, "publicKey", JsonFunction.jsonArrayFromStringHashSet(publicKey));
        }

        return  jsonObject;
    }

    public boolean define(JSONObject jsonObject) {
        if (jsonObject == null) {
            return false;
        }

        id = JsonFunction.getInt(jsonObject, "id", -1);
        signatureAlgorithm = JsonFunction.getInt(jsonObject, "signatureAlgorithm", 1);

        publicKey = JsonFunction.getHashSetStringFromJsonArray(jsonObject, "publicKey", new HashSet<>());

        return true;
    }

    public boolean isAllowed(byte[] account) {
        if (publicKey == null || publicKey.size() == 0 || account == null || account.length == 0) {
            return false;
        }

        return publicKey.contains(JsonFunction.hexStringFromBytes(account));
    }

    public void allow(byte[] account) {
        if (account == null || account.length == 0) {
            return;
        }

        if (publicKey == null) {
            publicKey = new HashSet<>();
        }

        publicKey.add(JsonFunction.hexStringFromBytes(account));
    }

    public boolean remove(byte[] account) {
        if (account == null || account.length == 0) {
            return false;
        }

        if (publicKey == null || publicKey.size() == 0) {
            return false;
        }

        if (!publicKey.contains(JsonFunction.hexStringFromBytes(account))) {
            return false;
        }

        publicKey.remove(JsonFunction.hexStringFromBytes(account));

        return  true;
    }
}
