package concept.omno.object;

import concept.utility.JsonFunction;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class PlatformTokenRate implements Cloneable {

    HashMap<Long, Double> mapChainToken = new HashMap<>();
    HashMap<Long, Double> mapAssetToken = new HashMap<>();
    HashMap<Long, Double> mapNativeAssetToken = new HashMap<>();

    public PlatformTokenRate() {}

    public PlatformTokenRate(JSONObject jsonObject) {
        define(jsonObject);
    }

    public boolean isValid() {
        return true;
    }

    public PlatformTokenRate clone() {
        final PlatformTokenRate clone;

        try {
            clone = (PlatformTokenRate) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException();
        }

        if (mapAssetToken != null) {
            clone.mapAssetToken = new HashMap<>();
            clone.mapAssetToken.putAll(mapAssetToken);
        }

        if (mapChainToken != null) {
            clone.mapChainToken = new HashMap<>();
            clone.mapChainToken.putAll(mapChainToken);
        }

        if (mapNativeAssetToken != null) {
            clone.mapNativeAssetToken = new HashMap<>();
            clone.mapNativeAssetToken.putAll(mapNativeAssetToken);
        }

        return clone;
    }

    public void define(JSONObject jsonObject) {
        if (jsonObject == null) {
            return;
        }

        mapAssetToken = JsonFunction.getHashMapLongDoubleFromUnsignedStringKeyValuePairs(jsonObject, "asset", null);
        mapChainToken = JsonFunction.getHashMapLongDoubleFromUnsignedStringKeyValuePairs(jsonObject, "chain", null);
        mapNativeAssetToken = JsonFunction.getHashMapLongDoubleFromUnsignedStringKeyValuePairs(jsonObject, "native", null);
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();

        if (mapAssetToken != null && mapAssetToken.size() > 0){
            JsonFunction.put(jsonObject, "asset", JsonFunction.jsonObjectStringUnsignedLongDoublePairsFromMap(mapAssetToken, false));
        }

        if (mapChainToken != null && mapChainToken.size() > 0){
            JsonFunction.put(jsonObject, "chain", JsonFunction.jsonObjectStringUnsignedLongDoublePairsFromMap(mapChainToken, false));
        }

        if (mapNativeAssetToken != null && mapNativeAssetToken.size() > 0){
            JsonFunction.put(jsonObject, "native", JsonFunction.jsonObjectStringUnsignedLongDoublePairsFromMap(mapNativeAssetToken, false));
        }

        return  jsonObject;
    }

    private boolean isZero(HashMap<Long, Double> map) {

        if (map != null && !map.isEmpty()) {
            for (double value: map.values()) {
                if (value != 0) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean isZero() {

        if (!isValid()) {
            return false;
        }

        if (!isZero(mapChainToken)) {
            return false;
        }

        if (!isZero(mapAssetToken)) {
            return false;
        }

        return isZero(mapNativeAssetToken);
    }

    private void multiply(HashMap<Long, Double> map, long multiplier) {
        if (map == null || map.size() == 0) {
            return;
        }

        if (multiplier == 0) {
            map.clear();
            return;
        }

        if (multiplier < 0) {
            throw new IllegalArgumentException("negative not implemented");
        }

        for (Map.Entry<Long, Double> entry: map.entrySet()) {
            Long key = entry.getKey();
            Double value = entry.getValue();

            double newAmountNQT = value * multiplier;

            map.put(key, newAmountNQT);
        }
    }

    private void multiply(HashMap<Long, Double> map, double multiplier) {
        if (map == null || map.size() == 0) {
            return;
        }

        if (multiplier == 0) {
            map.clear();
            return;
        }

        if (multiplier < 0) {
            throw new IllegalArgumentException("negative not implemented");
        }

        for (Map.Entry<Long, Double> entry: map.entrySet()) {
            Long key = entry.getKey();
            Double value = entry.getValue();

            double newAmountNQT = value * multiplier;

            map.put(key, newAmountNQT);
        }
    }

    public void multiply(double multiplier) {
        multiply(mapChainToken, multiplier);
        multiply(mapAssetToken, multiplier);
        multiply(mapNativeAssetToken, multiplier);
    }

    private void setMapToken(HashMap<Long, Double> hashMap, long key, double value) {
        if (hashMap == null || key == 0) {
            return;
        }

        hashMap.put(key, value);
    }

    public void setMapChainToken(long key, double value) {
        setMapToken(mapChainToken, key, value);
    }

    public void setMapAssetToken(long key, double value) {
        setMapToken(mapAssetToken, key, value);
    }

    public void setMapNativeAssetToken(long key, double value) {
        setMapToken(mapNativeAssetToken, key, value);
    }

    public HashMap<Long, Double> getChainTokenMap() {
        return mapChainToken;
    }

    public HashMap<Long, Double> getAssetTokenMap() {
        return mapAssetToken;
    }

    public HashMap<Long, Double> getNativeAssetTokenMap() {
        return mapNativeAssetToken;
    }
}
