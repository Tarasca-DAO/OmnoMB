package concept.omno.object;

import concept.utility.JsonFunction;
import org.json.simple.JSONObject;

public class UserAccount {
    public long id;
    public PlatformToken balance = new PlatformToken();
    public PlatformToken balanceLocked = new PlatformToken();

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();

        JsonFunction.put(jsonObject, "id", Long.toUnsignedString(id));

        if (balance != null && !balance.isZero()) {
            JsonFunction.put(jsonObject, "balance", balance.toJSONObject());
        }

        if (balanceLocked != null && !balanceLocked.isZero()) {
            JsonFunction.put(jsonObject, "balanceLocked", balanceLocked.toJSONObject());
        }

        return  jsonObject;
    }

    public void define(JSONObject jsonObject) {
        if (jsonObject == null) {
            return;
        }

        id = JsonFunction.getLongFromStringUnsigned(jsonObject, "id", 0);

        balance = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "balance", null));
        balanceLocked = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "balanceLocked", null));
    }

    public UserAccount(long account, PlatformToken platformToken) {
        this.id = account;

        if (platformToken != null && platformToken.isValid()) {
            this.balance = platformToken.clone();
        }
    }

    public UserAccount(JSONObject jsonObject) {
        define(jsonObject);
    }

    public long getBalanceNativeAsset(long id) {
        long value = 0;

        if (balance != null) {
            value = balance.getNativeAssetTokenValue(id);
        }

        return value;
    }


    public boolean hasRequiredBalance(PlatformToken platformToken) {

        if (platformToken == null || platformToken.isZero()) {
            return true;
        }

        if (! platformToken.isValid()) {
            return false;
        }

        return balance.isGreaterOrEqual(platformToken);
    }

    public boolean hasRequiredBalanceLocked(PlatformToken platformToken) {

        if (platformToken == null || platformToken.isZero()) {
            return true;
        }

        if (! platformToken.isValid()) {
            return false;
        }

        return balanceLocked.isGreaterOrEqual(platformToken);
    }

    public void addToBalance(PlatformToken platformToken) {

        if (platformToken == null || ! platformToken.isValid()) {
            return;
        }

        balance.merge(platformToken, true);
    }

    public void subtractFromBalance(PlatformToken platformToken) {

        if (platformToken == null || ! platformToken.isValid()) {
            return;
        }

        balance.merge(platformToken, false);
    }

    public void addToBalanceLocked(PlatformToken platformToken) {

        if (platformToken == null || ! platformToken.isValid()) {
            return;
        }

        balanceLocked.merge(platformToken, true);
    }

    public void subtractFromBalanceLocked(PlatformToken platformToken) {

        if (platformToken == null || ! platformToken.isValid()) {
            return;
        }

        balanceLocked.merge(platformToken, false);
    }

    public boolean lockBalance(PlatformToken platformToken) {

        if (! platformToken.isValid() || ! balance.isGreaterOrEqual(platformToken)) {
            return false;
        }

        subtractFromBalance(platformToken);
        addToBalanceLocked(platformToken);

        return true;
    }

    public boolean unlockBalance(PlatformToken platformToken) {

        if (! platformToken.isValid() || ! balanceLocked.isGreaterOrEqual(platformToken)) {
            return false;
        }

        subtractFromBalanceLocked(platformToken);
        addToBalance(platformToken);

        return true;
    }
}
