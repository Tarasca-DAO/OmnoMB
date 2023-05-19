package concept.omno.object;

import concept.platform.ArdorApi;
import concept.utility.JsonFunction;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlatformToken implements Cloneable {

    // id, value
    HashMap<Long, Long> mapChainToken = new HashMap<>();
    HashMap<Long, Long> mapAssetToken = new HashMap<>();
    HashMap<Long, Long> mapNativeAssetToken = new HashMap<>();

    private boolean isValid = true; // overflow etc.

    public PlatformToken() {}

    public PlatformToken(JSONObject jsonObject) {
        define(jsonObject);
    }

    public boolean isValid() {
        return isValid;
    }

    public void invalid() {
        isValid = false;
    }

    public PlatformToken clone() {
        final PlatformToken clone;

        try {
            clone = (PlatformToken) super.clone();
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

        clone.isValid = isValid;

        return clone;
    }

    public void define(JSONObject jsonObject) {
        if (jsonObject == null) {
            return;
        }

        mapAssetToken = JsonFunction.getHashMapLongLongFromUnsignedStringKeyValuePairs(jsonObject, "asset", null);
        mapChainToken = JsonFunction.getHashMapLongLongFromUnsignedStringKeyValuePairs(jsonObject, "chain", null);
        mapNativeAssetToken = JsonFunction.getHashMapLongLongFromUnsignedStringKeyValuePairs(jsonObject, "native", null);
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();

        if (mapAssetToken != null && mapAssetToken.size() > 0){
            JsonFunction.put(jsonObject, "asset", JsonFunction.jsonObjectStringUnsignedLongPairsFromMap(mapAssetToken, false));
        }

        if (mapChainToken != null && mapChainToken.size() > 0){
            JsonFunction.put(jsonObject, "chain", JsonFunction.jsonObjectStringUnsignedLongPairsFromMap(mapChainToken, false));
        }

        if (mapNativeAssetToken != null && mapNativeAssetToken.size() > 0){
            JsonFunction.put(jsonObject, "native", JsonFunction.jsonObjectStringUnsignedLongPairsFromMap(mapNativeAssetToken, false));
        }

        return  jsonObject;
    }

    public HashMap<Long, Long> getChainTokenMap() {
        return mapChainToken;
    }

    public HashMap<Long, Long> getAssetTokenMap() {
        return mapAssetToken;
    }

    public HashMap<Long, Long> getNativeAssetTokenMap() {
        return mapNativeAssetToken;
    }

    public void zero() {

        removeChainTokens();
        removeAssets();
        removeNativeAssets();

        isValid = true;
    }

    public void removeChainTokens() {

        if (mapChainToken != null) {
            mapChainToken.clear();
        }
    }

    public void removeNativeAssets() {

        if (mapNativeAssetToken != null) {
            mapNativeAssetToken.clear();
        }
    }

    public void removeAssets() {

        if (mapAssetToken != null) {
            mapAssetToken.clear();
        }
    }

    public boolean isAssetTokenUnique() {

        removeZeroBalanceAll();

        return (mapAssetToken != null && mapAssetToken.size() == 1);
    }

    public boolean isChainTokenUnique() {

        removeZeroBalanceAll();

        return (mapChainToken != null && mapChainToken.size() == 1);
    }

    public long getUnitValueByUnique(ArdorApi ardorApi) {

        if (!isValid || countUniqueTokensAll() != 1) {
            return 0;
        }

        long uniqueId = getUniqueId();

        if (uniqueId == 0) {
            return 0;
        }

        if (isAssetTokenUnique()) {
            return ardorApi.getAsset(uniqueId).getUnit();
        } else if (isChainTokenUnique()) {
            return ardorApi.getChainToken((int) uniqueId).ONE_COIN;
        }

        return 0;
    }

    private long add(long a, long b) {

        if (a + b < a || a + b < b) {
            invalid();
        }

        return a + b;
    }

    private long subtract(long a, long b) {

        if (a - b > a || (a - b < 0)) {
            invalid();
        }

        return a - b;
    }

    public void mergeAssetToken(long id, long delta, boolean isAdd) {

        if (id == 0) {
            return;
        }

        long value = 0;

        if (mapAssetToken.containsKey(id)) {
            value = mapAssetToken.get(id);
        }

        if (isAdd) {
            value = add(value, delta);
        } else {
            value = subtract(value, delta);
        }

        mapAssetToken.put(id, value);
    }

    public void mergeChainToken(long id, long delta, boolean isAdd) {

        if (id == 0) {
            return;
        }

        long value = 0;

        if (mapChainToken.containsKey(id)) value = mapChainToken.get(id);

        if (isAdd) {
            value = add(value, delta);
        } else {
            value = subtract(value, delta);
        }

        mapChainToken.put(id, value);
    }

    public void mergeNativeAssetToken(long id, long delta, boolean isAdd) {

        if (id == 0) {
            return;
        }

        long value = 0;

        if (mapNativeAssetToken.containsKey(id)) {
            value = mapNativeAssetToken.get(id);
        }

        if (isAdd) {
            value = add(value, delta);
        } else {
            value = subtract(value, delta);
        }

        mapNativeAssetToken.put(id, value);
    }

    public void mergeAsId(PlatformToken platformToken) {

        if (!isValid || platformToken == null || !platformToken.isValid() || platformToken.isZero()) {
            return;
        }

        boolean isZero = isZero();

        PlatformToken ids = platformToken.clone();

        ids.setValues(1);

        merge(ids, true);

        if (!isZero) {
            setValues(1);
        }
    }

    public PlatformToken getAsId() {

        if (!isValid()) {
            return null;
        }

        PlatformToken result = clone();
        result.setValues(1);

        return result;
    }

    public long getUniqueKey() {

        if (!isValid() || isZero() || countUniqueTokensAll() != 1) {
            return 0;
        }

        if (mapChainToken != null && mapChainToken.size() == 1) {
            return (long) mapChainToken.keySet().toArray()[0];
        }

        if (mapAssetToken != null && mapAssetToken.size() == 1) {
            return (long) mapAssetToken.keySet().toArray()[0];
        }

        if (mapNativeAssetToken != null && mapNativeAssetToken.size() == 1) {
            return (long) mapNativeAssetToken.keySet().toArray()[0];
        }

        return 0;
    }

    public long getChainTokenValue(long id) {
        long value = 0;

        if (mapChainToken != null && mapChainToken.containsKey(id)) {
            value = mapChainToken.get(id);
        }

        return value;
    }

    public long getAssetTokenValue(long id) {

        long value = 0;

        if (mapAssetToken != null && mapAssetToken.containsKey(id)) {
            value = mapAssetToken.get(id);
        }

        return value;
    }

    public long getNativeAssetTokenValue(long id) {
        long value = 0;

        if (mapNativeAssetToken != null &&  mapNativeAssetToken.containsKey(id)) {
            value = mapNativeAssetToken.get(id);
        }

        return value;
    }

    private long getValueByUniqueId(HashMap<Long, Long> hashMap, HashMap<Long, Long> hashMapFilter) {

        if (hashMap != null && hashMap.size() > 0 && hashMapFilter != null && hashMapFilter.size() == 1) {

            long key = new ArrayList<>(hashMapFilter.keySet()).get(0);

            if (hashMap.containsKey(key)) {
                return hashMap.get(key);
            }
        }

        return -1;
    }

    public long getValueByUniqueId(PlatformToken platformToken) {

        if (platformToken == null || !platformToken.isValid() || platformToken.isZero()) {
            return 0;
        }

        if (platformToken.countUniqueTokensAll() != 1) {
            return -1;
        }

        if (platformToken.mapAssetToken != null && platformToken.mapAssetToken.size() == 1) {
            return getValueByUniqueId(mapAssetToken, platformToken.mapAssetToken);
        }

        if (platformToken.mapChainToken != null && platformToken.mapChainToken.size() == 1) {
            return getValueByUniqueId(mapChainToken, platformToken.mapChainToken);
        }

        if (platformToken.mapNativeAssetToken != null && platformToken.mapNativeAssetToken.size() == 1) {
            return  getValueByUniqueId(mapNativeAssetToken, platformToken.mapNativeAssetToken);
        }

        return 0;
    }

    private long getValueSum(HashMap<Long, Long> hashMap) {
        if (hashMap == null || hashMap.size() == 0) {
            return 0;
        }

        long result = 0;

        for (long value: hashMap.values()) {
            long sum = result + value;

            if (sum < result) {
                invalid();
                break;
            }

            result = sum;
        }

        return result;
    }

    public long getValueSum() {

        if (!isValid() || countUniqueTokensAll() == 0) {
            return 0;
        }

        long result = getValueSum(mapAssetToken);

        result += getValueSum(mapChainToken);

        result += getValueSum(mapNativeAssetToken);

        if (!isValid()) {
            result = -1;
        }

        return result;
    }

    public long getAssetTokenValueSum() {

        if (! isValid()) {
            return 0;
        }

        long result =  getValueSum(mapAssetToken);

        if (!isValid()) {
            result = -1;
        }

        return result;
    }

    private boolean isZero(HashMap<Long, Long> map) {

        if (map != null && !map.isEmpty()) {
            for (Long value: map.values()) {
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

    private boolean isGreaterOrEqual(HashMap<Long, Long> mapDestination, HashMap<Long, Long> mapSource) {

        if (mapSource == null || mapSource.size() == 0) { // negative map not handled
            return true;
        }

        if (mapDestination == null || mapDestination.size() == 0) {
            return false;
        }

        for (Map.Entry<Long, Long> entry: mapSource.entrySet()) {
            Long key = entry.getKey();
            Long value = entry.getValue();

            long balance = 0;

            if (mapDestination.containsKey(key)) {
                balance = mapDestination.get(key);
            }

            if (balance < value) {
                return false;
            }
        }

        return true;
    }

    public boolean isGreaterOrEqual(PlatformToken platformToken) {
        if (platformToken == null) {
            return true;
        }

        if (! isGreaterOrEqual(mapChainToken, platformToken.getChainTokenMap())) {
            return false;
        }

        if (! isGreaterOrEqual(mapAssetToken, platformToken.getAssetTokenMap())) {
            return false;
        }

        return isGreaterOrEqual(mapNativeAssetToken, platformToken.getNativeAssetTokenMap());
    }

    // returns is-balance-positive if modified, or fee invalid
    public boolean applyTransactionFee(PlatformToken feeCost, int assetTransactionChain, boolean isAdd) {

        if (feeCost == null || feeCost.isZero()) {
            return true;
        }

        if (mapChainToken == null) {
            mapChainToken = new HashMap<>();
        }

        removeZeroBalanceAll();
        feeCost.removeZeroBalanceAll();

        HashMap<Long, Long> map = new HashMap<>();

        mapChainToken.forEach(map::put);

        HashMap<Long, Long> mapFee = feeCost.getChainTokenMap();

        if (!map.isEmpty()) {
            for (Map.Entry<Long, Long> entry: map.entrySet()) {
                Long key = entry.getKey();
                Long balance = entry.getValue();

                if (mapFee.containsKey(key)) {
                    long fee = mapFee.get(key);

                    if (isAdd) {
                        balance += fee;
                    } else {

                        if (balance <= fee) {
                            balance = 0L;
                            map.put(key, balance);
                            continue;
                        }

                        balance -= fee;
                    }

                    if (balance < 0) {
                        return false;
                    }
                }

                map.put(key, balance);
            }
        }

        if (mapAssetToken != null && ! mapAssetToken.isEmpty()) {

            int uniqueAssetCount = mapAssetToken.size();

            long feeNQT = 0;
            long balance = 0;

            if (mapFee.containsKey((long) assetTransactionChain)) {
                feeNQT = mapFee.get((long) assetTransactionChain);

                if (map.containsKey(((long) assetTransactionChain))) {
                    balance = map.get((long) assetTransactionChain);
                }
            }

            feeNQT *= uniqueAssetCount;

            if (isAdd) {
                balance += feeNQT;
            } else {
                balance -= feeNQT;
            }

            if (balance < 0) {
                return false;
            }

            map.put((long) assetTransactionChain, balance);
        }

        mapChainToken = map;

        removeZeroBalanceAll();

        return true;
    }

    private boolean merge(HashMap<Long, Long> mapDestination, HashMap<Long, Long> mapSource, boolean add) {
        boolean isNegative = false;

        if (mapSource == null || mapSource.size() == 0 || mapDestination == null) {
            return false;
        }

        for (Map.Entry<Long, Long> entry: mapSource.entrySet()) {
            Long key = entry.getKey();
            Long value = entry.getValue();

            long balance = 0;

            if (mapDestination.size() != 0 && mapDestination.containsKey(key)) {
                balance = mapDestination.get(key);
            }

            if (add) {
                balance = add(balance, value);
            } else {
                balance = subtract(balance, value);
            }

            if (balance < 0) {
                isNegative = true;
            }

            mapDestination.put(key, balance);
        }

        return isNegative;
    }

    public boolean merge(PlatformToken platformToken, boolean isAdd) {
        boolean isNegative = false;

        if (platformToken == null || !platformToken.isValid()) {
            return false;
        }

        if (merge(mapChainToken, platformToken.mapChainToken, isAdd)) {
            isNegative = true;
        }

        if (merge(mapAssetToken, platformToken.mapAssetToken, isAdd)) {
            isNegative = true;
        }

        if (merge(mapNativeAssetToken, platformToken.mapNativeAssetToken, isAdd)) {
            isNegative = true;
        }

        return isNegative;
    }

    public void merge(HashMap<Long, ArdorApi.AssetTokenBalance> assetTokenBalanceHashMap, boolean isAdd) {

        if (assetTokenBalanceHashMap == null || assetTokenBalanceHashMap.size() == 0) {
            return;
        }

        for (ArdorApi.AssetTokenBalance assetTokenBalance: assetTokenBalanceHashMap.values()) {
            mergeAssetToken(assetTokenBalance.assetToken.id, assetTokenBalance.quantityQNT, isAdd);
        }
    }

    private void subtractLimitedWithRemainder(HashMap<Long, Long> mapDestination, HashMap<Long, Long> mapSource) {

        if (mapSource == null || mapSource.size() == 0 || mapDestination == null || mapDestination.size() == 0) {
            return;
        }

        for (long key: mapSource.keySet()) {

            if (mapDestination.containsKey(key)) {
                long valueDestination = mapDestination.get(key);
                long valueSource = mapSource.get(key);

                if (valueDestination < valueSource) {
                    valueSource -= valueDestination;
                    valueDestination = 0;
                } else {
                    valueDestination -= valueSource;
                    valueSource = 0;
                }

                mapDestination.put(key, valueDestination);
                mapSource.put(key, valueSource);
            }
        }
    }

    public void subtractLimitedWithRemainder(PlatformToken platformToken) {

        if (platformToken == null || !platformToken.isValid) {
            return;
        }

        subtractLimitedWithRemainder(mapAssetToken, platformToken.mapAssetToken);
        subtractLimitedWithRemainder(mapChainToken, platformToken.mapChainToken);
        subtractLimitedWithRemainder(mapNativeAssetToken, platformToken.mapNativeAssetToken);
    }

    private void removeById(HashMap<Long, Long> mapDestination, HashMap<Long, Long> mapSource) {

        if (mapSource == null || mapSource.size() == 0 || mapDestination == null || mapDestination.size() == 0) {
            return;
        }

        for (long key: mapSource.keySet()) {

            if (mapDestination.containsKey(key)) {
                mapDestination.put(key, 0L);
            }
        }
    }

    public void removeById(PlatformToken platformToken) {

        if (platformToken == null || !platformToken.isValid) {
            return;
        }

        platformToken.removeZeroBalanceAll();

        removeById(mapAssetToken, platformToken.mapAssetToken);
        removeById(mapChainToken, platformToken.mapChainToken);
        removeById(mapNativeAssetToken, platformToken.mapNativeAssetToken);

        removeZeroBalanceAll();
    }

    private void multiply(HashMap<Long, Long> map, long multiplier) {
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

        for (Map.Entry<Long, Long> entry: map.entrySet()) {
            Long key = entry.getKey();
            Long value = entry.getValue();

            long newAmountNQT = value * multiplier;

            if (newAmountNQT < value) {
                invalid();
            }

            map.put(key, newAmountNQT);
        }
    }

    private void multiply(HashMap<Long, Long> map, double multiplier) {
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

        for (Map.Entry<Long, Long> entry: map.entrySet()) {
            Long key = entry.getKey();
            Long value = entry.getValue();

            long newAmountNQT = (long) (value * multiplier);

            if (newAmountNQT < value && multiplier > 1) {
                invalid();
            }

            map.put(key, newAmountNQT);
        }
    }

    public void multiply(long multiplier) {
        multiply(mapChainToken, multiplier);
        multiply(mapAssetToken, multiplier);
        multiply(mapNativeAssetToken, multiplier);
    }

    public void multiply(double multiplier) {
        multiply(mapChainToken, multiplier);
        multiply(mapAssetToken, multiplier);
        multiply(mapNativeAssetToken, multiplier);
    }

    private void setMapToken(HashMap<Long, Long> hashMap, long key, long value) {

        if (hashMap == null || key == 0) {
            return;
        }

        hashMap.put(key, value);
    }

    public void setMapChainToken(long key, long value) {
        setMapToken(mapChainToken, key, value);
    }

    public void keepChainTokenOnly() {
        removeAssets();
        removeNativeAssets();
    }

    public boolean removeZeroBalanceAll() {

        boolean result = false;

        if (removeZeroBalance(mapAssetToken)) {
            result = true;
        }

        if (removeZeroBalance(mapChainToken)) {
            result = true;
        }

        if (removeZeroBalance(mapNativeAssetToken)) {
            result = true;
        }

        return result;
    }

    private void setValues(HashMap<Long, Long> hashMap, long value) {

        if (hashMap == null || hashMap.size() == 0) {
            return;
        }

        for (long key: hashMap.keySet()) {
            if (hashMap.get(key) != 0) {
                hashMap.put(key, value);
            }
        }
    }

    public void setValues (long value) {
        setValues(mapAssetToken, value);
        setValues(mapChainToken, value);
        setValues(mapNativeAssetToken, value);
    }

    private void keepById(HashMap<Long, Long> hashMap, HashMap<Long, Long> hashMapFilter) {

        if (hashMap == null || hashMap.size() == 0 || hashMapFilter == null) {
            return;
        }

        if (hashMapFilter.size() == 0) {
            hashMap.clear();
            return;
        }

        boolean requireClear = false;

        for (long key: hashMap.keySet()) {
            if (!hashMapFilter.containsKey(key)) {
                hashMap.put(key, 0L);
                requireClear = true;
            }
        }

        if (requireClear) {
            removeZeroBalance(hashMap);
        }
    }

    public void keepById(PlatformToken platformToken) {
        keepById(mapAssetToken, platformToken.getAssetTokenMap());
        keepById(mapChainToken, platformToken.getChainTokenMap());
        keepById(mapNativeAssetToken, platformToken.getNativeAssetTokenMap());
    }

    private boolean removeZeroBalance(HashMap<Long, Long> hashMap) {

        if (hashMap == null || hashMap.size() == 0) {
            return false;
        }

        boolean result = false;

        List<Long> keys = new ArrayList<>(hashMap.keySet());

        for (long key: keys) {
            synchronized (hashMap) {
                if (hashMap.get(key) == 0) {
                    hashMap.remove(key);
                    result = true;
                }
            }
        }

        return result;
    }

    public long getUniqueAssetTokenId() {
        removeZeroBalanceAll();

        if (mapAssetToken != null && mapAssetToken.size() == 1) {
            return (long) mapAssetToken.keySet().toArray()[0];
        }

        return 0;
    }

    public long getUniqueId() {
        removeZeroBalanceAll();

        if (mapAssetToken != null && mapAssetToken.size() == 1) {
            return (long) mapAssetToken.keySet().toArray()[0];
        }

        if (mapChainToken != null && mapChainToken.size() == 1) {
            return (long) mapChainToken.keySet().toArray()[0];
        }

        if (mapNativeAssetToken != null && mapNativeAssetToken.size() == 1) {
            return (long) mapNativeAssetToken.keySet().toArray()[0];
        }

        return 0;
    }

    public int countUniqueTokensAll() {
        removeZeroBalanceAll();

        int count = 0;

        if (mapAssetToken != null) {
            count += mapAssetToken.size();
        }

        if (mapChainToken != null) {
            count += mapChainToken.size();
        }

        if (mapNativeAssetToken != null) {
            count += mapNativeAssetToken.size();
        }

        return count;
    }

    private void mask(HashMap<Long, Long> hashMap, HashMap<Long, Long> hashMapMask) {
        if (hashMap == null || hashMap.size() == 0 || hashMapMask == null) {
            return;
        }

        if (hashMapMask.size() == 0) {
            hashMap.clear();
            return;
        }

        for (long key: hashMapMask.keySet()) {
            if (!hashMapMask.containsKey(key)) {
                hashMap.remove(key);
            }
        }

    }

    public void mask(PlatformToken platformToken) {
        if (platformToken == null || !platformToken.isValid) {
            return;
        }

        mask(mapAssetToken, platformToken.mapAssetToken);
        mask(mapChainToken, platformToken.mapChainToken);
        mask(mapNativeAssetToken, platformToken.mapNativeAssetToken);
    }

    private void maskFromRate(HashMap<Long, Long> hashMap, HashMap<Long, Double> hashMapMask) {
        if (hashMap == null || hashMap.size() == 0 || hashMapMask == null) {
            return;
        }

        if (hashMapMask.size() == 0) {
            hashMap.clear();
            return;
        }

        for (long key: hashMapMask.keySet()) {
            if (!hashMapMask.containsKey(key)) {
                hashMap.remove(key);
            }
        }

    }

    public void mask(PlatformTokenRate platformTokenRate) {
        if (platformTokenRate == null) {
            return;
        }

        maskFromRate(mapAssetToken, platformTokenRate.getAssetTokenMap());
        maskFromRate(mapChainToken, platformTokenRate.getChainTokenMap());
        maskFromRate(mapNativeAssetToken, platformTokenRate.getNativeAssetTokenMap());
    }

    private void multiply(HashMap<Long, Long> hashMap, HashMap<Long, Double> hashMapRate) {
        if (hashMap == null || hashMap.size() == 0 || hashMapRate == null) {
            return;
        }

        if (hashMapRate.size() == 0) {
            hashMap.clear();
            return;
        }

        for (long key: hashMapRate.keySet()) {
            if (!hashMap.containsKey(key)) {
                continue;
            }

            double multiplier = hashMapRate.get(key);

            if (multiplier < 0) {
                continue;
            }

            if (multiplier == 0) {
                hashMap.remove(key);
                continue;
            }

            long value = hashMap.get(key);
            long valueNew = (long) (value * multiplier);

            if (multiplier > 1 && valueNew <= value) {
                invalid();
            }

            hashMap.put(key, value);
        }
    }

    public void multiply(PlatformTokenRate platformTokenRate) {
        if (platformTokenRate == null) {
            return;
        }

        multiply(mapAssetToken, platformTokenRate.getAssetTokenMap());
        multiply(mapChainToken, platformTokenRate.getChainTokenMap());
        multiply(mapNativeAssetToken, platformTokenRate.getNativeAssetTokenMap());
    }
}
