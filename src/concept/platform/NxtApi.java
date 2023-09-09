package concept.platform;

import concept.utility.JsonFunction;
import concept.utility.NxtCryptography;
import concept.utility.RestApi;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class NxtApi extends RestApi {

    JSONObject constantsJson = null;
    final HashMap<Long, AssetToken> assetToken = new HashMap<>();
    final HashMap<Long, String> accountRSAbbreviated = new HashMap<>();

    public static class AssetToken {
        public long id;
        public long quantityQNT;
        public int decimals;
        private long unit;
        public long account;
        public String accountRS;
        public String name;
        public String description;
        public boolean hasPhasingAssetControl;

        AssetToken(JSONObject jsonObject) {

            if (jsonObject == null) {
                return;
            }

            id = JsonFunction.getLongFromStringUnsigned(jsonObject, "asset", 0);
            quantityQNT = JsonFunction.getLongFromStringUnsigned(jsonObject, "quantityQNT", 0);
            account = JsonFunction.getLongFromStringUnsigned(jsonObject, "account", 0);
            decimals = JsonFunction.getInt(jsonObject, "decimals", -1);

            accountRS = JsonFunction.getString(jsonObject, "accountRS", null);
            name = JsonFunction.getString(jsonObject, "name", null);
            description = JsonFunction.getString(jsonObject, "description", null);

            hasPhasingAssetControl = JsonFunction.getBoolean(jsonObject, "hasPhasingAssetControl", false);

        }

        public long getUnit() {

            if (unit == 0) {
                unit = (long) Math.pow(10, decimals);
            }

            return unit;
        }

        public boolean isValid () {
            return (id > 0 && account > 0 && quantityQNT > 0 && decimals >= 0 && accountRS != null);
        }
    }

    public static class AssetTokenBalance {
        public AssetToken assetToken;
        public long quantityQNT;
        public long unconfirmedQuantityQNT;

        AssetTokenBalance(NxtApi nxtApi, JSONObject jsonObject) {

            if (jsonObject == null) {
                return;
            }

            long id = JsonFunction.getLongFromStringUnsigned(jsonObject, "asset", 0);
            assetToken = nxtApi.getAsset(id);

            quantityQNT = JsonFunction.getLongFromStringUnsigned(jsonObject, "quantityQNT", 0);
            unconfirmedQuantityQNT = JsonFunction.getLongFromStringUnsigned(jsonObject, "unconfirmedQuantityQNT", 0);
        }
    }

    public NxtApi(String hostProtocolString, String hostNameString, String hostPortString) {
        super(hostProtocolString, hostNameString, hostPortString, "nxt");
    }

    public boolean isErrorResponse(JSONObject jsonObject) {

        if (jsonObject == null || jsonObject.containsKey("errorCode")) {
            return true;
        }

        return false;
    }

    public EconomicCluster getEconomicCluster() {

        JSONObject response = getBlockWithRetry(false);

        if (response == null) {
            return null;
        }

        int height = JsonFunction.getInt(response, "height", -1);
        long blockId = JsonFunction.getLongFromStringUnsigned(response, "block", 0);
        int timestamp = JsonFunction.getInt(response, "timestamp", -1);

        if (height < 0 || blockId == 0 || timestamp < 0) {
            return null;
        }

        return new EconomicCluster(height, blockId, timestamp);
    }

    public int getTime() {

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("requestType", "getBlockchainStatus");

        JSONObject response;

        try {
            response = jsonObjectHttpApi(true, parameters);
        } catch (IOException e) {
            return 0;
        }

        if (response == null || response.containsKey("errorCode") || ! response.containsKey("time")) {
            return 0;
        }

        return JsonFunction.getInt(response, "time", 0);
    }

    public int getBlockGenerationTimeRemaining() {

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("limit", Integer.toString(1));
        parameters.put("requestType", "getNextBlockGenerators");

        JSONObject response;

        try {
            response = jsonObjectHttpApi(true, parameters);
        } catch (IOException e) {
            return 0;
        }

        if (response == null || response.containsKey("errorCode") || ! response.containsKey("generators")) {
            return 0;
        }

        JSONArray generators = JsonFunction.getJSONArray(response, "generators", null);

        if (generators == null || generators.size() == 0) {
            return 0;
        }

        JSONObject generator = (JSONObject) generators.get(0);

        int hitTime = JsonFunction.getInt(generator, "hitTime", 0);

        if (hitTime == 0) {
            return 0;
        }

        int timestamp = getTime();

        if (timestamp == 0) {
            return 0;
        }

        hitTime += 10;

        if (timestamp > hitTime) {
            return 0;
        }

        return hitTime - timestamp;
    }

    public String getAccountRSAbbreviated(long accountId) {

        if (accountRSAbbreviated.size() != 0 && accountRSAbbreviated.containsKey(accountId)) {
            return accountRSAbbreviated.get(accountId);
        }

        JSONObject response = getAccount(accountId);

        if (response == null || ! response.containsKey("accountRS")) {
            return null;
        }

        String result = JsonFunction.getString(response, "accountRS", null);

        if (result == null || result.length() < 7) {
            return null;
        }

        byte[] bytes = result.getBytes(StandardCharsets.UTF_8);

        if (bytes[3] == '-') {
            result = result.substring(4);
        } else if (bytes[5] == '-') {
            result = result.substring(6);
        }

        if (accountRSAbbreviated.size() > 1000000) {
            accountRSAbbreviated.clear();
        }

        accountRSAbbreviated.put(accountId, result);

        return result;
    }

    public byte[] getTaggedDataBytesByAccountPropertyReference(String accountPropertyName, long accountId) {
        return getTaggedDataBytesByAccountPropertyReference(accountPropertyName, accountId, 2);
    }

    public byte[] getTaggedDataBytesByAccountPropertyReference(String accountPropertyName, long accountId, int chainId) {

        if (accountPropertyName == null || accountPropertyName.length() == 0) {
            return null;
        }

        String propertyContentString = getAccountProperty(accountPropertyName, accountId);

        if (propertyContentString == null || propertyContentString.length() != 0x40) {
            return null;
        }

        if (chainId == 0) {
            chainId = 2;
        }

        return getTaggedData(propertyContentString, chainId);
    }

    public byte[] getTaggedData(String transactionFullHash) {
        return getTaggedData(transactionFullHash, 2);
    }

    public byte[] getTaggedData(String transactionFullHash, int chainId) {

        if (transactionFullHash == null || transactionFullHash.length() != 0x40) {
            return null;
        }

        if (chainId == 0) {
            chainId = 2;
        }

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("chain", Integer.toString(chainId));
        parameters.put("transactionFullHash", transactionFullHash);
        parameters.put("includeData", Boolean.toString(true));
        parameters.put("retrieve", Boolean.toString(true));
        parameters.put("detectText", Boolean.toString(false));
        parameters.put("requestType", "getTaggedData");

        JSONObject response;

        try {
            response = jsonObjectHttpApi(true, parameters);
        } catch (IOException e) {
            return null;
        }

        if (response == null || response.containsKey("errorCode") || !response.containsKey("data")) {
            return null;
        }

        return JsonFunction.getBytesFromHexString(response, "data", null);
    }

    public String getAccountProperty(String accountPropertyName, long accountId) {

        if (accountPropertyName == null || accountPropertyName.length() == 0 || accountId == 0) {
            return null;
        }

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("recipient", Long.toUnsignedString(accountId));
        parameters.put("setter", Long.toUnsignedString(accountId));
        parameters.put("property", accountPropertyName);
        parameters.put("requestType", "getAccountProperties");

        JSONObject response;

        try {
            response = jsonObjectHttpApi(true, parameters);
        } catch (IOException e) {
            return null;
        }

        if (response == null || response.containsKey("errorCode")) {
            return null;
        }

        JSONArray properties = JsonFunction.getJSONArray(response, "properties", null);

        if (properties == null || properties.size() != 1) {
            return null;
        }

        try {
            return JsonFunction.getString((JSONObject) properties.get(0), "value", null);
        } catch (Exception e) {
            return null;
        }
    }

    public JSONObject getConstants() {

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("requestType", "getConstants");

        if (constantsJson == null) {
            try {
                constantsJson = jsonObjectHttpApi(true, parameters);
            } catch (IOException e) {
                return null;
            }
        }

        return constantsJson;
    }

    public HashMap<Long, AssetTokenBalance> getAccountAssets(long accountId) {

        if (accountId == 0) {
            return null;
        }

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("account", Long.toUnsignedString(accountId));
        parameters.put("requestType", "getAccountAssets");

        JSONObject response;

        try {
            response = jsonObjectHttpApi(true, parameters);
        } catch (IOException e) {
            return null;
        }

        JSONArray jsonArray = JsonFunction.getJSONArray(response, "accountAssets", null);

        if (jsonArray == null || jsonArray.size() == 0) {
            return null;
        }

        HashMap<Long, AssetTokenBalance> result = new HashMap<>();

        for (Object object: jsonArray) {
            AssetTokenBalance item = new AssetTokenBalance(this, (JSONObject) object);
            result.put(item.assetToken.id, item);
        }

        return result;
    }

    public AssetToken getAsset(long assetId) {

        if (assetId == 0) {
            return null;
        }

        synchronized (assetToken) {

            // Lior Yaffe (Jelurida) pointed out that this could return a stale asset if a block was replaced that had
            // contained an issueAsset with a new (or later) issueAsset but with a different asset and a coincidental assetId.

            if (assetToken.size() != 0 && assetToken.containsKey(assetId)) {
                return assetToken.get(assetId);
            }
        }

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("asset", Long.toUnsignedString(assetId));
        parameters.put("requestType", "getAsset");

        JSONObject response;

        try {
            response = jsonObjectHttpApi(true, parameters);
        } catch (IOException e) {
            return null;
        }

        synchronized (assetToken) {

            AssetToken item = new AssetToken(response);

            assetToken.put(assetId, item);

            return item;
        }
    }

    public JSONObject getAccount(long accountId) {

        JSONObject result = null;

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("account", Long.toUnsignedString(accountId));
        parameters.put("requestType", "getAccount");

        try {

            JSONObject response = jsonObjectHttpApi(true, parameters);

            if (response.containsKey("publicKey")) {
                result = response;
            }

        } catch (Exception ignored) {}

        return result;
    }

    public JSONObject getAccount(String privateKey, String secretPhrase, String publicKey, long accountId) {

        JSONObject result = null;

        HashMap<String, String> parameters = new HashMap<>();

        NxtCryptography nxtCryptography;

        try {
            if (privateKey != null && privateKey.length() == 0x40) {
                nxtCryptography = new NxtCryptography(JsonFunction.bytesFromHexString(privateKey));
                parameters.put("account", nxtCryptography.getAccountIdString());
            } else if (secretPhrase != null && secretPhrase.length() != 0) {
                byte[] privateKeyBytes = NxtCryptography.getHash(secretPhrase.getBytes(StandardCharsets.UTF_8), "SHA-256");
                nxtCryptography = new NxtCryptography(privateKeyBytes);
                parameters.put("account", nxtCryptography.getAccountIdString());
            } else if (publicKey != null && publicKey.length() == 0x40) {
                nxtCryptography = new NxtCryptography();
                nxtCryptography.setPublicKeyString(publicKey);
                parameters.put("account", nxtCryptography.getAccountIdString());
            } else if (accountId != 0) {
                parameters.put("account", Long.toUnsignedString(accountId));
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }

        parameters.put("requestType", "getAccount");

        try {
            JSONObject response = jsonObjectHttpApi(true, parameters);

            if (response.containsKey("publicKey")) {
                result = response;
            }

        } catch (Exception ignored) {}

        return result;
    }

    public byte[] getAccountPublicKey(long accountId) {
        byte[] publicKey = null;

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("account", Long.toUnsignedString(accountId));

        parameters.put("requestType", "getAccount");

        try {
            JSONObject response = jsonObjectHttpApi(true, parameters);

            if (response.containsKey("publicKey")) {
                publicKey = NxtCryptography.bytesFromHexString((String) response.get("publicKey"));
            }
        } catch (Exception ignored) {}

        return publicKey;
    }

    public boolean transactionBytesBroadcast(byte[] transactionBytes, JSONObject prunableAttachment) {
        boolean result = true;

        HashMap<String, String> parameters = new HashMap<>();

        hashMapAddByteArrayAsHexStringParameter(parameters, "transactionBytes", transactionBytes);

        if (prunableAttachment != null) {
            parameters.put("prunableAttachmentJSON", prunableAttachment.toJSONString());
        }

        parameters.put("requestType", "broadcastTransaction");
        JSONObject response = null;

        try {
            response = jsonObjectHttpApi(true, parameters);
        } catch (IOException e) {
            return false;
       }

        if (response.containsKey("errorCode")) {
            result = false;
        }

        return result;
    }

    public JSONObject getBlockWithRetry(boolean includeTransactions) {

        JSONObject result = null;

        while(result == null) {

            result = getBlock(-1, 0, -1, includeTransactions);

            if (result == null) {

                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (Exception ignored) {}
            }
        }

        return result;
    }

    public JSONObject getBlockWithRetry(int height, long blockId, int timestamp, boolean includeTransactions, int timeOutSeconds, boolean showSpinMessage) {

        JSONObject result = null;

        int tryCount = 0;
        final int sleepTimeMillisSeconds = 1000;

        while (result == null) {

            result = getBlock(height, blockId, timestamp, includeTransactions);

            if (result == null) {

                try {
                    TimeUnit.MILLISECONDS.sleep(sleepTimeMillisSeconds);
                } catch (Exception ignored) {}

                if (timeOutSeconds > 0 && (++tryCount * sleepTimeMillisSeconds) >= (timeOutSeconds * 1000)) {
                    System.out.println("blockchain platform API timed-out");
                    return null;
                }

                if (showSpinMessage && tryCount % 10 == 0) {
                    System.out.println("Retrying getBlockWithRetry()... " + tryCount);
                }
            }
        }

        return result;
    }

    public JSONObject getBlock(boolean includeTransactions) {
        return getBlock(-1, 0, -1, includeTransactions);
    }

    public JSONObject getBlock(int height, long blockId, int timestamp, boolean includeTransactions) {
        JSONObject response;

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("requestType", "getBlock");

        if (height >= 0) {
            parameters.put("height", Integer.toString(height));
        } else if (blockId != 0) {
            parameters.put("block", Long.toUnsignedString(blockId));
        } else if (timestamp >= 0) {
            parameters.put("timestamp", Integer.toString(timestamp));
        }

        parameters.put("includeTransactions", Boolean.toString(includeTransactions));

        try {
            response = jsonObjectHttpApi(true, parameters);
        } catch (Exception e) {
            return null;
        }

        return response;
    }

    public JSONObject getTransaction(int chain, byte[] fullHash) throws IOException {
        JSONObject response;

        if (fullHash == null) {
            return null;
        }

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("requestType", "getTransaction");

        parameters.put("chain", Integer.toString(chain));

        parameters.put("fullHash", NxtCryptography.hexStringFromBytes(fullHash));

        response = jsonObjectHttpApi(true, parameters);

        if (response.containsKey("errorCode")) {
            response = null;
        }

        return response;
    }

    public long accountIdFromAlias(String aliasString, int chain) throws IOException {
        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("aliasName", aliasString);
        parameters.put("chain", Integer.toString(chain));

        parameters.put("requestType", "getAlias");
        JSONObject response = jsonObjectHttpApi(true, parameters);

        if (!response.containsKey("account")) {
            return 0;
        }

        return Long.parseUnsignedLong((String) response.get("account"));
    }


    public JSONObject broadcastTransaction(JSONObject jsonObject) throws IOException {
        JSONObject response = null;

        if (jsonObject != null)
        {
            HashMap<String, String> parameters = new HashMap<>();

            parameters.put("transactionJSON", jsonObject.toJSONString());

            parameters.put("requestType", "broadcastTransaction");

            response = jsonObjectHttpApi(true, parameters);
        }

        return response;
    }

    public JSONObject getExecutedTransactions(String adminPassword, int chain, int height, long recipient, long sender) throws IOException {
        JSONObject response;

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("requestType", "getExecutedTransactions");

        parameters.put("chain", Integer.toString(chain));

        if (adminPassword != null && adminPassword.length() != 0) {
            parameters.put("adminPassword", adminPassword);
        }

        if (height >= 0) {
            parameters.put("height", Integer.toString(height));
        }

        if (recipient != 0) {
            parameters.put("recipient", Long.toUnsignedString(recipient));
        }

        if (sender != 0) {
            parameters.put("sender", Long.toUnsignedString(sender));
        }

        response = jsonObjectHttpApi(true, parameters);

        return response;
    }

    public JSONObject transferAsset(int ecBlockHeight, long ecBlockId, int timestamp, short deadline, byte[] privateKey, byte[] publicKey, long recipient, int chainId, long feeNQT, String message, boolean broadcast, long assetId, long quantityQNT) {
        JSONObject transactionObject;
        JSONObject response;

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("requestType", "transferAsset");

        if (privateKey != null) {
            parameters.put("privateKey", NxtCryptography.hexStringFromBytes(privateKey));
            parameters.put("broadcast", Boolean.toString(broadcast));
        } else {
            parameters.put("publicKey", NxtCryptography.hexStringFromBytes(publicKey));
            parameters.put("broadcast", "false");
        }

        parameters.put("ecBlockHeight", Integer.toString(ecBlockHeight));
        parameters.put("ecBlockId", Long.toUnsignedString(ecBlockId));

        if (timestamp != 0) {
            parameters.put("timestamp", Integer.toString(timestamp));
        }

        parameters.put("recipient", Long.toUnsignedString(recipient));
        parameters.put("feeNQT", Long.toUnsignedString(feeNQT));
        parameters.put("asset", Long.toUnsignedString(assetId));
        parameters.put("quantityQNT", Long.toUnsignedString(quantityQNT));

        parameters.put("deadline", Integer.toString(deadline));
        parameters.put("chain", Integer.toString(chainId));

        if (message != null) {
            parameters.put("message", message);
            parameters.put("messageIsText", Boolean.toString(true));
            parameters.put("messageIsPrunable", Boolean.toString(true));
        }

        try {
            response = jsonObjectHttpApi(true, parameters);
        } catch (Exception e) {
            return null;
        }

        transactionObject = combineTransactionResponse(response);

        return transactionObject;
    }

    public JSONObject sendMoney(int ecBlockHeight, long ecBlockId, int timestamp, short deadline, byte[] privateKey, byte[] publicKey, long recipient, int chainId, long feeNQT, String message, boolean broadcast, long amountNQT) {
        JSONObject transactionObject;
        JSONObject response;

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("requestType", "sendMoney");

        if (privateKey != null) {
            parameters.put("privateKey", NxtCryptography.hexStringFromBytes(privateKey));
            parameters.put("broadcast", Boolean.toString(broadcast));
        } else {
            parameters.put("publicKey", NxtCryptography.hexStringFromBytes(publicKey));
            parameters.put("broadcast", "false");
        }

        parameters.put("ecBlockHeight", Integer.toString(ecBlockHeight));
        parameters.put("ecBlockId", Long.toUnsignedString(ecBlockId));

        if (timestamp != 0) {
            parameters.put("timestamp", Integer.toString(timestamp));
        }

        parameters.put("recipient", Long.toUnsignedString(recipient));
        parameters.put("feeNQT", Long.toUnsignedString(feeNQT));
        parameters.put("amountNQT", Long.toUnsignedString(amountNQT));

        parameters.put("deadline", Integer.toString(deadline));
        parameters.put("chain", Integer.toString(chainId));

        if (message != null) {
            parameters.put("message", message);
            parameters.put("messageIsText", Boolean.toString(true));
            parameters.put("messageIsPrunable", Boolean.toString(true));
        }

        try {
            response = jsonObjectHttpApi(true, parameters);
        } catch (Exception e) {
            return null;
        }

        transactionObject = combineTransactionResponse(response);

        return transactionObject;
    }

    public JSONObject combineTransactionResponse(JSONObject response) {

        if (response == null) {
            return null;
        }

        JSONObject result = JsonFunction.getJSONObject(response, "transactionJSON", null);

        String string = JsonFunction.getString(response, "unsignedTransactionBytes", null);

        if (string != null) {
            JsonFunction.put(result,"unsignedTransactionBytes", string);
        }

        string = JsonFunction.getString(response, "transactionBytes", null);

        if (string != null) {
            JsonFunction.put(result,"transactionBytes", string);
        }

        return result;
    }

    private static void hashMapAddByteArrayAsHexStringParameter(HashMap<String, String> hashMap, String keyString, byte[] byteParameter) {
        hashMap.put(keyString, String.format("%0" + (byteParameter.length << 1) + "x", new BigInteger(1, byteParameter)));
    }
}
