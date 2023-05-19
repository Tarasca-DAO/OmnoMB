package concept.platform;

import concept.utility.JsonFunction;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Transaction {
    public byte[] unsignedTransactionBytes;
    public byte[] transactionBytes;
    public byte[] fullHash;
    public byte[] referencedTransactionFullHash = null;
    public int referencedChain = 0;
    public Transaction referencedTransaction;
    public int chain;
    public EconomicCluster commitEC;
    public int ecBlockHeight;
    public long ecBlockId;
    public int timestamp, timestampDeadline;
    public int deadlineMinutes;
    public int type, subtype; //byte
    public long sender;
    public String senderRS;
    public long recipient;
    public long amountNQT, attachmentId;
    public JSONObject attachment = null;
    public JSONObject messageJson = null;
    public boolean messageIsPrunable;
    public JSONObject source = null;

    public boolean isValid(ArdorApi ardorApi) {
        return commitEC.isValid(ardorApi) && fullHash != null;
    }

    public boolean isValidUnconfirmed(ArdorApi ardorApi) {
        JSONObject response = ardorApi.getBlockWithRetry(ecBlockHeight, 0, -1, false, 30, true);

        long block = JsonFunction.getLongFromStringUnsigned(response, "block", 0);

        return block == ecBlockId;
    }

    public Transaction(JSONObject jsonObject) {

        if (jsonObject == null) {
            return;
        }

        source = jsonObject;

        fullHash = JsonFunction.getBytesFromHexString(jsonObject, "fullHash", null);
        unsignedTransactionBytes = JsonFunction.getBytesFromHexString(jsonObject, "unsignedTransactionBytes", null);
        transactionBytes = JsonFunction.getBytesFromHexString(jsonObject, "transactionBytes", null);

        JSONObject referencedJson  = JsonFunction.getJSONObject(jsonObject, "referencedTransaction", null);

        if (referencedJson != null) {
            referencedTransactionFullHash = JsonFunction.getBytesFromHexString(referencedJson, "transactionFullHash", null);
            referencedChain = JsonFunction.getInt(referencedJson, "chain", 0);
        }

        transactionBytes = JsonFunction.getBytesFromHexString(jsonObject, "transactionBytes", null);

        chain = JsonFunction.getInt(jsonObject, "chain", 0);

        type = JsonFunction.getInt(jsonObject, "type", 0);
        subtype = JsonFunction.getInt(jsonObject, "subtype", 0);

        timestamp = JsonFunction.getInt(jsonObject, "timestamp", 0);
        deadlineMinutes = JsonFunction.getInt(jsonObject, "deadline", 0);
        timestampDeadline = timestamp + deadlineMinutes * 60;

        int height = JsonFunction.getInt(jsonObject, "height", 0);
        long blockId = JsonFunction.getLongFromStringUnsigned(jsonObject, "block", 0);
        int blockTimestamp = JsonFunction.getInt(jsonObject, "blockTimestamp", 0);
        commitEC = new EconomicCluster(height, blockId, blockTimestamp);

        ecBlockHeight = JsonFunction.getInt(jsonObject, "ecBlockHeight", 0);
        ecBlockId = JsonFunction.getLongFromStringUnsigned(jsonObject, "ecBlockId", 0);

        sender = JsonFunction.getLongFromStringUnsigned(jsonObject, "sender", 0);
        senderRS = JsonFunction.getString(jsonObject, "senderRS", null);
        recipient = JsonFunction.getLongFromStringUnsigned(jsonObject, "recipient", 0);

        amountNQT = JsonFunction.getLongFromStringUnsigned(jsonObject, "amountNQT", 0);

        attachment = JsonFunction.getJSONObject(jsonObject, "attachment", null);

        if (attachment != null) {

            if (type == 2 && subtype == 1) {
                attachmentId = JsonFunction.getLongFromStringUnsigned(attachment, "asset", 0);
                amountNQT = JsonFunction.getLongFromStringUnsigned(attachment, "quantityQNT", 0);
            }

            if (attachment.containsKey("messageIsText") && (boolean) attachment.get("messageIsText") && attachment.containsKey("message")) {
                String message = (String) attachment.get("message");
                JSONParser jsonParser = new JSONParser();

                try {
                    messageJson = (JSONObject) jsonParser.parse(message);
                } catch (Exception ignored) {}

                messageIsPrunable = (JsonFunction.getInt(attachment, "version.PrunablePlainMessage", 0) != 0);
            }
        }
    }

    public boolean getReferencedTransaction(ArdorApi ardorApi) {

        if (ardorApi == null) {
            return false;
        }

        if (referencedTransactionFullHash == null || referencedChain < 1) {
            return true;
        }

        referencedTransaction = new Transaction(ardorApi.getTransaction(referencedChain, referencedTransactionFullHash));

        if (!referencedTransaction.isValid(ardorApi)) {
            return false;
        }

        return referencedTransaction.getReferencedTransaction(ardorApi);
    }
}
