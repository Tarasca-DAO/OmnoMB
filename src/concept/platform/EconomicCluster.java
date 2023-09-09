package concept.platform;

import concept.utility.JsonFunction;

import java.util.concurrent.TimeUnit;

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
    
        if (height < 0) {
            height = 0;
        }

        JSONObject response = null;
        int retryAttempts = 0;
        int MAX_RETRY_ATTEMPTS = 60;

        while (retryAttempts < MAX_RETRY_ATTEMPTS) {
            response = ardorApi.getBlockWithRetry(height, 0, -1, false, 50, true);
            if (response != null) {
                if (response.containsKey("block") && response.containsKey("timestamp")) {
                    break; // Successfully fetched the response, exit the loop
                }
            }
            retryAttempts++;

            try {
                TimeUnit.MILLISECONDS.sleep(1500);
            } catch (Exception e) {}
        }

        if (retryAttempts >= MAX_RETRY_ATTEMPTS) {
            throw new NullPointerException("Failed to fetch block response after " + MAX_RETRY_ATTEMPTS + " attempts.");
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
