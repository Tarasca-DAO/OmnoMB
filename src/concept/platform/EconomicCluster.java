package concept.platform;

import concept.utility.JsonFunction;
import org.json.simple.JSONObject;

public class EconomicCluster implements Cloneable {
    public int height = 0;
    public long blockId = 0L;
    public int timestamp = 0;

    public int getHeight() {
        return height;
    }

    public long getBlockId() {
        return blockId;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public EconomicCluster clone() {
        final EconomicCluster clone;

        try {
            clone = (EconomicCluster) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException();
        }

        clone.height = height;
        clone.blockId = blockId;
        clone.timestamp = timestamp;

        return clone;
    }

    public boolean isValid(ArdorApi ardorApi) {
        JSONObject response;

        response = ardorApi.getBlockWithRetry(height, 0, -1, false, 30, true);

        if (response == null) {
            return false;
        }

        if (ardorApi.isErrorResponse(response)) {
            return false;
        }

        if (! response.containsKey("block") || blockId != JsonFunction.getLongFromStringUnsigned(response, "block", 0)) {
            return false;
        }

        return timestamp != 0;
    }

    public void update(ArdorApi ardorApi, int height) {
        JSONObject response;

        if (height < 0) {
            height = 0;
        }

        response = ardorApi.getBlockWithRetry(height, 0, -1, false, 50, true);

        if (response == null) {
            throw new NullPointerException("Block response is null");
        }

        if (! response.containsKey("block")) {
            throw new NullPointerException("Block response does not contain block");
        }

        if (! response.containsKey("timestamp")) {
            throw new NullPointerException("Block response does not contain timestamp");
        }

        this.height = height;
        blockId = JsonFunction.getLongFromStringUnsigned(response, "block", 0);
        timestamp = JsonFunction.getInt(response, "timestamp", 0);
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();

        JsonFunction.put(jsonObject, "height", height);
        JsonFunction.put(jsonObject, "blockId", Long.toUnsignedString(blockId));
        JsonFunction.put(jsonObject, "timestamp", timestamp);

        return  jsonObject;
    }

    public void define(JSONObject jsonObject) {
        if (jsonObject == null) {
            return;
        }

        height = JsonFunction.getInt(jsonObject, "height", 0);
        blockId = JsonFunction.getLongFromStringUnsigned(jsonObject, "blockId", 0);
        timestamp = JsonFunction.getInt(jsonObject, "timestamp", 0);
    }

    public boolean isEqual(EconomicCluster economicCluster) {

        if (economicCluster == null) {
            return false;
        }

        return (economicCluster.height == height && economicCluster.blockId == blockId && economicCluster.timestamp == timestamp);
    }

    public EconomicCluster(JSONObject jsonObject) {
        define(jsonObject);
    }

    public EconomicCluster(ArdorApi ardorApi, int height) {
        update(ardorApi, height);
    }

    public EconomicCluster(int height, long blockId, int timestamp) {
        this.height = height;
        this.blockId = blockId;
        this.timestamp = timestamp;
    }
}
