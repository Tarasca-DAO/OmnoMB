package concept.omno.service.PlatformTokenExchangeById;

import concept.omno.object.PlatformToken;
import concept.omno.service.NativeAssetState;
import concept.utility.JsonFunction;
import org.json.simple.JSONObject;

public class Offer implements Cloneable {

    public long id = -1;
    public long account = 0;
    public long multiplier = 0;

    public PlatformToken give = new PlatformToken();
    public PlatformToken take = new PlatformToken();

    public void define(JSONObject jsonObject) {
        if (jsonObject == null) {
            return;
        }

        id = JsonFunction.getLongFromStringUnsigned(jsonObject, "id", -1);
        account = JsonFunction.getLongFromStringUnsigned(jsonObject, "account", 0);
        multiplier = JsonFunction.getLongFromStringUnsigned(jsonObject, "multiplier", 0);

        give = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "give", null));
        take = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "take", null));
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();

        JsonFunction.put(jsonObject, "id", Long.toUnsignedString(id));
        JsonFunction.put(jsonObject, "account", Long.toUnsignedString(account));
        JsonFunction.put(jsonObject, "multiplier", Long.toUnsignedString(multiplier));

        JsonFunction.put(jsonObject, "give", give.toJSONObject());
        JsonFunction.put(jsonObject, "take", take.toJSONObject());

        return  jsonObject;
    }

    public Offer(long account, PlatformToken give, PlatformToken take, long multiplier) {
        if (give == null || !give.isValid() || take == null || !take.isValid() || multiplier <= 0) {
            return;
        }

        this.account = account;
        this.give = give;
        this.take = take;
        this.multiplier = multiplier;
    }

    Offer(JSONObject jsonObject) {
        define(jsonObject);
    }

    public Offer clone() {
        final Offer clone;

        try {
            clone = (Offer) super.clone();
        } catch (CloneNotSupportedException e) {
            throw  new RuntimeException();
        }

        clone.account = account;
        clone.give = give;
        clone.take = take;
        clone.multiplier = multiplier;
        clone.id = id;

        return clone;
    }

    public boolean hasRoyalty(NativeAssetState nativeAssetState) {
        return (nativeAssetState.countUniqueRoyaltyAsset(give) + nativeAssetState.countUniqueRoyaltyAsset(take) != 0);
    }

    public boolean isValid(NativeAssetState nativeAssetState) {

        if (give == null && take == null) {
            return false;
        }

        if (take == null && give.isZero()) {
            return false;
        }

        if (give == null && take.isZero()) {
            return false;
        }

        int countRoyaltyGive = nativeAssetState.countUniqueRoyaltyAsset(give);
        int countRoyaltyTake = nativeAssetState.countUniqueRoyaltyAsset(take);

        if (countRoyaltyGive + countRoyaltyTake > 1) {
            return false;
        }

        if (countRoyaltyGive != 0 && give.countUniqueTokensAll() > 1) {
            return false;
        }

        if (countRoyaltyTake != 0 && take.countUniqueTokensAll() > 1) {
            return false;
        }

        return true;
    }

    public boolean isSafeMultiplier(long multiplier) {

        if (multiplier <= 0) {
            return false;
        }

        long multiplierCombined = this.multiplier + multiplier;

        return multiplierCombined > 0 && multiplierCombined >= this.multiplier;
    }

    public boolean canGiveFromBalance(PlatformToken balanceGiveFrom) {

        if (balanceGiveFrom == null || balanceGiveFrom.isZero() || multiplier <= 0) {
            return false;
        }

        PlatformToken totalGive = give.clone();
        totalGive.multiply(multiplier);

        if (! totalGive.isValid()) {
            return false;
        }

        return balanceGiveFrom.isGreaterOrEqual(totalGive);
    }

    public boolean canTakeFromBalance(PlatformToken balanceTakeFrom, long multiplier) {

        if (balanceTakeFrom == null || balanceTakeFrom.isZero() || multiplier <= 0) {
            return false;
        }

        PlatformToken totalTake = take.clone();
        totalTake.multiply(multiplier);

        if (! totalTake.isValid()) {
            return false;
        }

        return balanceTakeFrom.isGreaterOrEqual(totalTake);
    }

    public boolean reduce(long multiplier) {

        if (multiplier < 0 || multiplier > this.multiplier) {
            return false;
        }

        this.multiplier -= multiplier;

        return true;
    }

    public boolean increase(long multiplier) {

        if (multiplier <= 0) {
            return false;
        }

        if (! isSafeMultiplier(multiplier)) {
            return false;
        }

        this.multiplier += multiplier;

        return true;
    }
}
