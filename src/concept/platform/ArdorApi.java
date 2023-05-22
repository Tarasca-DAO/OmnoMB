package concept.platform;

import concept.utility.JsonFunction;
import concept.utility.NxtCryptography;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ArdorApi extends NxtApi {

    final HashMap<Integer, ChainToken> chainToken = new HashMap<>();

    public static class ChainToken {
        public int id;
        public long totalAmount;
        public long ONE_COIN;
        public int decimals;
        public String name;

        ChainToken(JSONObject jsonObject) {

            if (jsonObject == null) {
                return;
            }

            id = JsonFunction.getInt(jsonObject, "id", 0);
            totalAmount = JsonFunction.getLongFromStringUnsigned(jsonObject, "totalAmount", 0);
            ONE_COIN = JsonFunction.getLongFromStringUnsigned(jsonObject, "ONE_COIN", 0);
            decimals = JsonFunction.getInt(jsonObject, "decimals", -1);

            name = JsonFunction.getString(jsonObject, "name", null);
        }

        public boolean isValid() {
            return (id > 0 && totalAmount > 0 && decimals >= 0);
        }
    }

    public static class ChainTokenBalance {
        public ChainToken chainToken;
        public long unconfirmedBalanceNQT;
        public long balanceNQT;

        ChainTokenBalance(ArdorApi ardorApi, int chainId, JSONObject jsonObject) {

            if (jsonObject == null) {
                return;
            }

            chainToken = ardorApi.getChainToken(chainId);

            unconfirmedBalanceNQT = JsonFunction.getLongFromStringUnsigned(jsonObject, "unconfirmedBalanceNQT", 0);
            balanceNQT = JsonFunction.getLongFromStringUnsigned(jsonObject, "balanceNQT", 0);
        }

        public boolean isValid() {
            return (chainToken != null && chainToken.isValid());
        }
    }

    public ArdorApi(String hostProtocolString, String hostNameString, String hostPortString) {
        super(hostProtocolString, hostNameString, hostPortString);
    }

    // @Override
    public List<Transaction> getUnconfirmedTransactions(int chainId, long recipient) {

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("chain", Integer.toString(chainId));

        /*
         * if (sender != 0) {
         * parameters.put("account", Long.toUnsignedString(sender));
         * }
         */

        if (recipient != 0) {
            parameters.put("account", Long.toUnsignedString(recipient));
        }

        parameters.put("requestType", "getUnconfirmedTransactions");

        List<Transaction> result = new ArrayList<>();
        JSONObject response;

        try {
            response = jsonObjectHttpApi(true, parameters);

            if (!response.containsKey("unconfirmedTransactions")) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }

        JSONArray jsonArray = JsonFunction.getJSONArray(response, "unconfirmedTransactions", null);

        if (jsonArray == null || jsonArray.size() == 0) {
            return null;
        }

        for (Object o : jsonArray) {

            JSONObject jsonObject = (JSONObject) o;
            Transaction transaction = new Transaction(jsonObject);

            if (recipient != 0 && transaction.recipient != recipient) {
                continue;
            }

            result.add(transaction);
        }

        return result;
    }

    public ChainTokenBalance getBalance(long accountId, int chainId) {

        if (accountId == 0 || chainId == 0) {
            return null;
        }

        ChainTokenBalance result = null;

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("chain", Integer.toString(chainId));
        parameters.put("account", Long.toUnsignedString(accountId));
        parameters.put("requestType", "getBalance");

        try {
            JSONObject response = jsonObjectHttpApi(true, parameters);

            if (response.containsKey("balanceNQT")) {
                result = new ChainTokenBalance(this, chainId, response);

                if (!result.isValid()) {
                    return null;
                }
            }
        } catch (Exception ignored) {
        }

        return result;
    }

    public ChainToken getChainToken(int chainId) {

        if (chainId <= 0) {
            return null;
        }

        synchronized (chainToken) {
            if (chainToken.size() != 0 && chainToken.containsKey(chainId)) {
                return chainToken.get(chainId);
            }
        }

        JSONObject constantsJson = getConstants();

        if (constantsJson == null) {
            return null;
        }

        JSONObject chainProperties = JsonFunction.getJSONObject(constantsJson, "chainProperties", null);

        if (chainProperties == null) {
            return null;
        }

        JSONObject chain = JsonFunction.getJSONObject(chainProperties, Integer.toString(chainId), null);

        if (chain == null) {
            return null;
        }

        ChainToken result = new ChainToken(chain);

        if (!result.isValid()) {
            return null;
        }

        synchronized (chainToken) {
            chainToken.put(chainId, result);
        }

        return result;
    }

    public NxtCryptography nxtCryptographyFromString(String string) {

        if (string == null || string.length() == 0) {
            return null;
        }

        JSONObject jsonObject;
        NxtCryptography nxtCryptography = null;

        if (string.length() == 0x40) {
            jsonObject = getAccount(string, null, null, 0);

            if (jsonObject != null) {
                nxtCryptography = new NxtCryptography(JsonFunction.bytesFromHexString(string));
                nxtCryptography.setAccountRS(JsonFunction.getString(jsonObject, "accountRS", null));
                return nxtCryptography;
            }

            jsonObject = getAccount(null, null, string, 0);

            if (jsonObject != null) {
                nxtCryptography = new NxtCryptography();
                nxtCryptography.setPublicKeyString(string);
                nxtCryptography.setAccountRS(JsonFunction.getString(jsonObject, "accountRS", null));
                return nxtCryptography;
            }
        }

        jsonObject = getAccount(null, string, null, 0);

        if (jsonObject != null) {
            nxtCryptography = new NxtCryptography(
                    NxtCryptography.getHash(string.getBytes(StandardCharsets.UTF_8), "SHA-256"));
            nxtCryptography.setAccountRS(JsonFunction.getString(jsonObject, "accountRS", null));
            return nxtCryptography;
        }

        try {
            jsonObject = getAccount(null, null, null, Long.parseUnsignedLong(string));
        } catch (Exception ignored) {
        }

        if (jsonObject != null) {
            nxtCryptography = new NxtCryptography();
            nxtCryptography.setPublicKeyString(JsonFunction.getString(jsonObject, "publicKey", null));
            nxtCryptography.setAccountRS(JsonFunction.getString(jsonObject, "accountRS", null));
        }

        return nxtCryptography;
    }

    // @Override
    public boolean broadcast(JSONObject combinedTransactionJson, byte[] privateKey) {

        if (combinedTransactionJson == null) {
            return false;
        }

        Transaction transaction = new Transaction(combinedTransactionJson);

        return transactionBytesBroadcast(transaction.unsignedTransactionBytes, transaction.attachment, privateKey);
    }

    // @Override
    public boolean transactionBroadcast(JSONObject jsonObject, byte[] privateKey) {

        if (jsonObject == null) {
            return false;
        }

        JSONObject prunableAttachment = JsonFunction.getJSONObject(jsonObject, "attachment", null);

        byte[] transactionBytes = JsonFunction.getBytesFromHexString(jsonObject, "unsignedTransactionBytes", null);

        if (transactionBytes == null) {
            transactionBytes = JsonFunction.getBytesFromHexString(jsonObject, "transactionBytes", null);
        }

        return transactionBytesBroadcast(transactionBytes, prunableAttachment, privateKey);
    }

    public boolean transactionBytesBroadcast(byte[] transactionBytes, JSONObject prunableAttachment,
            byte[] privateKey) {

        if (transactionBytes == null || prunableAttachment == null) {
            return false;
        }

        if (privateKey != null && privateKey.length == 0x20) {
            ArdorTransaction.signUnsignedTransactionBytes(transactionBytes, privateKey);
        }

        return transactionBytesBroadcast(transactionBytes, prunableAttachment);
    }

    public long transactionResponseCalculateFeeFQT(JSONObject jsonTransactionResponse) throws IOException {
        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("transactionJSON", ((JSONObject) jsonTransactionResponse.get("transactionJSON")).toJSONString());

        parameters.put("requestType", "calculateFee");
        JSONObject response = jsonObjectHttpApi(true, parameters);

        return JsonFunction.getLongFromStringUnsigned(response, "feeNQT", 0);
    }

    @Override
    public JSONObject getTransaction(int chain, byte[] fullHash) {
        JSONObject response;

        if (fullHash == null) {
            return null;
        }

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("requestType", "getTransaction");

        parameters.put("chain", Integer.toString(chain));

        parameters.put("fullHash", NxtCryptography.hexStringFromBytes(fullHash));

        try {
            response = jsonObjectHttpApi(true, parameters);
        } catch (IOException e) {
            response = null;
        }

        if (response != null && response.containsKey("errorCode")) {
            response = null;
        }

        return response;
    }

    @Override
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

    @Override
    public JSONObject getExecutedTransactions(String adminPassword, int chain, int height, long recipient, long sender)
            throws IOException {
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

    @Override
    public JSONObject transferAsset(int ecBlockHeight, long ecBlockId, int timestamp, short deadline, byte[] privateKey,
            byte[] publicKey, long recipient, int chainId, long feeNQT, String message, boolean broadcast, long assetId,
            long quantityQNT) {
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

    @Override
    public JSONObject sendMoney(int ecBlockHeight, long ecBlockId, int timestamp, short deadline, byte[] privateKey,
            byte[] publicKey, long recipient, int chainId, long feeNQT, String message, boolean broadcast,
            long amountNQT) {
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

    public JSONObject sendMessage(int ecBlockHeight, long ecBlockId, int timestamp, short deadline, byte[] privateKey,
            byte[] publicKey, long recipient, int chainId, long feeNQT, String message, boolean broadcast) {
        JSONObject transactionObject;
        JSONObject response;

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("requestType", "sendMessage");

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
}
