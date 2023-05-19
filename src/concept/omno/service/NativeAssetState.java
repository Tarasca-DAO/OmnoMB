package concept.omno.service;

import concept.omno.ApplicationContext;
import concept.omno.object.NativeAsset;
import concept.omno.object.Operation;
import concept.omno.object.PlatformToken;
import concept.utility.JsonFunction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NativeAssetState {
    final private ApplicationContext applicationContext;

    HashMap<Long, NativeAsset> asset = new HashMap<>();
    long issueCounter = 0;

    long incomeAccount;
    PlatformToken feeForIssue = new PlatformToken();
    PlatformToken operationFee = new PlatformToken();

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();

        JSONArray jsonArray = new JSONArray();

        for (NativeAsset value: asset.values()) {
            JsonFunction.add(jsonArray, value.toJSONObject());
        }

        JsonFunction.put(jsonObject, "asset", jsonArray);

        JsonFunction.put(jsonObject, "issueCounter", Long.toUnsignedString(issueCounter));

        JsonFunction.put(jsonObject, "incomeAccount", Long.toUnsignedString(incomeAccount));
        JsonFunction.put(jsonObject, "feeForIssue", feeForIssue.toJSONObject());
        JsonFunction.put(jsonObject, "operationFee", operationFee.toJSONObject());

        return  jsonObject;
    }

    public void define(JSONObject jsonObject, boolean validate) {
        if (jsonObject == null) {
            return;
        }

        defineNativeAsset(JsonFunction.getJSONArray(jsonObject, "asset", null), validate);

        issueCounter = JsonFunction.getLongFromStringUnsigned(jsonObject, "issueCounter", 0);

        incomeAccount = JsonFunction.getLongFromStringUnsigned(jsonObject, "incomeAccount", applicationContext.contractAccountId);
        feeForIssue = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "feeForIssue", null));
        operationFee = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "operationFee", null));
    }

    public void defineNativeAsset(JSONArray jsonArray, boolean validate) {
        if (jsonArray == null) {
            return;
        }

        for (Object o: jsonArray) {
            defineNativeAsset((JSONObject) o, validate);
        }
    }

    public void defineNativeAsset(JSONObject jsonObject, boolean validate) {
        if (jsonObject == null) {
            return;
        }

        NativeAsset item = new NativeAsset(jsonObject);

        if (validate && !item.isValid()) {
            return;
        }

        asset.put(item.id, item);
    }

    public NativeAssetState(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        incomeAccount = applicationContext.contractAccountId;
    }

    public NativeAssetState(ApplicationContext applicationContext, JSONObject jsonObject, boolean validate) {
        this(applicationContext);
        define(jsonObject, validate);
    }

    public NativeAsset issueAsset(long account, String name, String description, long quantity, long quantityMaximum, double royaltyFraction, boolean royaltyIsTradeOnly) {
        NativeAsset nativeAsset = new NativeAsset();

        nativeAsset.id = 1 + issueCounter;
        nativeAsset.quantity = quantity;
        nativeAsset.quantityMaximum = quantityMaximum;
        nativeAsset.account = account;
        nativeAsset.royaltyAccount = account;
        nativeAsset.royaltyFraction = royaltyFraction;
        nativeAsset.royaltyIsTradeOnly = royaltyIsTradeOnly;

        nativeAsset.name = name;
        nativeAsset.description = description;

        if (! nativeAsset.isValid()) {
            return null;
        }

        issueCounter++;

        asset.put(nativeAsset.id, nativeAsset);

        return nativeAsset;
    }

    public boolean setAssetRoyaltyFraction(long asset, long account, double royaltyFraction) {

        if (! this.asset.containsKey(asset)) {
            return false;
        }

        if (royaltyFraction < 0 || royaltyFraction > 100) {
            return false;
        }

        NativeAsset nativeAsset = this.asset.get(asset);

        if (nativeAsset.royaltyAccount != account) {
            return false;
        }

        if (royaltyFraction > nativeAsset.royaltyFraction) {
            long balance = applicationContext.state.userAccountState.getBalanceNativeAsset(asset);

            if (nativeAsset.quantity != balance) {
                    return false;
            }
        }

        nativeAsset.royaltyFraction = royaltyFraction;

        return true;
    }

    public boolean setAssetRoyaltyIsTradeOnly(long asset, long account, boolean requestedBooleanForRoyaltyIsTradeOnly) {

        if (! this.asset.containsKey(asset)) {
            return false;
        }

        NativeAsset nativeAsset = this.asset.get(asset);

        if (nativeAsset.royaltyAccount != account) {
            return false;
        }

        if (requestedBooleanForRoyaltyIsTradeOnly) {
            long balance = applicationContext.state.userAccountState.getBalanceNativeAsset(asset);

            if (nativeAsset.quantity != balance) {
                return false;
            }
        }

        nativeAsset.royaltyIsTradeOnly = requestedBooleanForRoyaltyIsTradeOnly;

        return true;
    }

    public boolean setAssetRoyaltyAccount(long asset, long account, long royaltyAccount) {

        if (! this.asset.containsKey(asset)) {
            return false;
        }

        NativeAsset nativeAsset = this.asset.get(asset);

        if (nativeAsset.royaltyAccount != account || royaltyAccount == 0) {
            return false;
        }

        nativeAsset.royaltyAccount = royaltyAccount;

        return true;
    }

    public boolean processOperation(Operation operation) {
        boolean result = false;

        if (operation == null) {
            return false;
        }

        if (operation.account != applicationContext.contractAccountId) {
            if (!applicationContext.state.userAccountState.subtractFromBalance(operation.account, operationFee)) {
                return false;
            }

            applicationContext.state.userAccountState.addToBalance(incomeAccount, operationFee);
        }

        switch (operation.request) {
            case "configure": {
                result = operationConfigure(operation);
                break;
            }

            case "issue": {
                result = operationIssue(operation);
                break;
            }

            case "issueIncrease": {
                result = operationIssueIncrease(operation);
                break;
            }

            case "decrease": {
                result = operationDecrease(operation);
                break;
            }

            case "setRoyaltyFraction": {
                result = operationSetRoyaltyFraction(operation);
                break;
            }

            case "setRoyaltyTradeOnlyFlag": {
                result = operationSetRoyaltyTradeOnlyFlag(operation);
                break;
            }

            case "setRoyaltyAccount": {
                result = operationSetRoyaltyAccount(operation);
                break;
            }
        }

        return result;
    }

    private boolean operationIssue(Operation operation) {

        if (operation == null || operation.parameterJson == null) {
            return false;
        }

        if (operation.account != incomeAccount && !applicationContext.state.userAccountState.hasRequiredBalance(operation.account, feeForIssue)) {
            return false;
        }

        String name = JsonFunction.getString(operation.parameterJson, "name", null);
        String description = JsonFunction.getString(operation.parameterJson, "description", null);
        long quantity = JsonFunction.getLongFromStringUnsigned(operation.parameterJson, "quantity", 0);
        long quantityMaximum = JsonFunction.getLongFromStringUnsigned(operation.parameterJson, "quantityMaximum", 0);

        double royaltyFraction = JsonFunction.getDoubleFromString(operation.parameterJson, "royaltyFraction", 0F);
        boolean royaltyIsTradeOnly = JsonFunction.getBoolean(operation.parameterJson, "royaltyIsTradeOnly", true);

        NativeAsset nativeAsset = issueAsset(operation.account, name, description, quantity, quantityMaximum, royaltyFraction, royaltyIsTradeOnly);

        if (nativeAsset == null) {
            return false;
        }

        applicationContext.state.transferToBalanceIncome(feeForIssue, operation.account);

        PlatformToken platformToken = new PlatformToken();

        platformToken.mergeNativeAssetToken(nativeAsset.id, nativeAsset.quantity, true);

        applicationContext.state.userAccountState.addToBalance(nativeAsset.account, platformToken);

        return true;
    }

    public boolean issueIncrease(long id, long amount, long account, boolean simulate) {

        NativeAsset nativeAsset = getAsset(id);

        if (nativeAsset == null) {
            return false;
        }

        if (!nativeAsset.isValid() || nativeAsset.account != account) {
            return false;
        }

        long newQuantity = nativeAsset.quantity + amount;

        if (amount <= 0 || newQuantity <= nativeAsset.quantity || newQuantity > nativeAsset.quantityMaximum) {
            return false;
        }

        if (simulate) {
            return true;
        }

        nativeAsset.quantity = newQuantity;

        PlatformToken platformToken = new PlatformToken();
        platformToken.mergeNativeAssetToken(nativeAsset.id, amount, true);

        applicationContext.state.userAccountState.addToBalance(nativeAsset.account, platformToken);

        return true;
    }

    private boolean operationIssueIncrease(Operation operation) {

        if (operation == null || operation.parameterJson == null) {
            return false;
        }

        JSONObject jsonObject = operation.parameterJson;

        long id = JsonFunction.getLongFromStringUnsigned(jsonObject, "id", 0);
        long increase = JsonFunction.getLongFromStringUnsigned(jsonObject, "amount", 0);

        return issueIncrease(id, increase, operation.account, false);
    }

    public boolean decrease(long id, long amount, long account) {

        NativeAsset nativeAsset = getAsset(id);

        if (nativeAsset == null) {
            return false;
        }

        if (!nativeAsset.isValid()) {
            return false;
        }

        long newQuantity = nativeAsset.quantity - amount;

        if (amount <= 0 || newQuantity >= nativeAsset.quantity) {
            return false;
        }

        PlatformToken platformToken = new PlatformToken();
        platformToken.mergeNativeAssetToken(nativeAsset.id, amount, true);

        if (!applicationContext.state.userAccountState.subtractFromBalance(account, platformToken)) {
            return false;
        }

        nativeAsset.quantity = newQuantity;

        return true;
    }

    private boolean operationDecrease(Operation operation) {

        if (operation == null || operation.parameterJson == null) {
            return false;
        }

        JSONObject jsonObject = operation.parameterJson;

        long id = JsonFunction.getLongFromStringUnsigned(jsonObject, "id", 0);


        long decrease = JsonFunction.getLongFromStringUnsigned(jsonObject, "amount", 0);

        return decrease(id, decrease, operation.account);
    }

    private boolean operationSetRoyaltyFraction(Operation operation) {

        if (operation == null || operation.parameterJson == null) {
            return false;
        }

        long asset = JsonFunction.getLongFromStringUnsigned(operation.parameterJson, "asset", 0);
        double royaltyFraction = JsonFunction.getDoubleFromString(operation.parameterJson, "royaltyFraction", 0F);

        setAssetRoyaltyFraction(asset, operation.account, royaltyFraction);

        return true;
    }

    private boolean operationSetRoyaltyAccount(Operation operation) {

        if (operation == null || operation.parameterJson == null) {
            return false;
        }

        long asset = JsonFunction.getLongFromStringUnsigned(operation.parameterJson, "asset", 0);
        long account = JsonFunction.getLongFromStringUnsigned(operation.parameterJson, "account", 0);

        setAssetRoyaltyAccount(asset, operation.account, account);

        return true;
    }

    private boolean operationSetRoyaltyTradeOnlyFlag(Operation operation) {

        if (operation == null || operation.parameterJson == null) {
            return false;
        }

        long asset = JsonFunction.getLongFromStringUnsigned(operation.parameterJson, "asset", 0);
        boolean royaltyIsTradeOnly = JsonFunction.getBoolean(operation.parameterJson, "royaltyIsTradeOnly", false);

        setAssetRoyaltyIsTradeOnly(asset, operation.account, royaltyIsTradeOnly);

        return true;
    }

    private boolean operationConfigure(Operation operation) {

        if (operation == null || operation.parameterJson == null) {
            return false;
        }

        if (operation.account != applicationContext.contractAccountId) {
            return false;
        }

        JSONObject jsonObject = JsonFunction.getJSONObject(operation.parameterJson, "issueCost", null);

        if (jsonObject == null) {
            return false;
        }

        PlatformToken newFeetForIssue = new PlatformToken(jsonObject);

        if (!newFeetForIssue.isValid()) {
            return false;
        }

        feeForIssue = newFeetForIssue;

        return true;
    }

    public int countUniqueRoyaltyAsset(PlatformToken platformToken) {
        int count = 0;

        if (platformToken == null || platformToken.getNativeAssetTokenMap() == null || platformToken.getNativeAssetTokenMap().size() == 0) {
            return  count;
        }

        HashMap<Long, Long> mapNativeAssets = platformToken.getNativeAssetTokenMap();

        List<Long> listNativeAssets = new ArrayList<>(mapNativeAssets.keySet());

        for (long asset: listNativeAssets) {
            NativeAsset nativeAsset = this.asset.get(asset);

            if (nativeAsset == null || !nativeAsset.isValid()) {
                continue;
            }

            if (nativeAsset.royaltyFraction != 0 && nativeAsset.quantity != 0) {
                count++;
            }
        }

        return count;
    }

    public boolean canTransfer(PlatformToken platformToken) {

        if (platformToken == null || platformToken.getNativeAssetTokenMap() == null || platformToken.getNativeAssetTokenMap().size() == 0 || asset == null || asset.size() == 0) {
            return true;
        }

        HashMap<Long, Long> mapNativeAssets = platformToken.getNativeAssetTokenMap();

        List<Long> listNativeAssets = new ArrayList<>(mapNativeAssets.keySet());

        for (long asset: listNativeAssets) {

            NativeAsset nativeAsset = this.asset.get(asset);

            if (nativeAsset == null || !nativeAsset.isValid()) {
                continue;
            }

            if (!nativeAsset.isValid()) {
                return  false;
            }

            if (nativeAsset.royaltyFraction != 0 && nativeAsset.royaltyIsTradeOnly) {
                return false;
            }
        }

        return true;
    }

    public NativeAsset getRoyaltyNativeAsset(PlatformToken platformToken) {

        if (platformToken == null || platformToken.getNativeAssetTokenMap() == null || platformToken.getNativeAssetTokenMap().size() == 0) {
            return null;
        }

        HashMap<Long, Long> mapNativeAssets = platformToken.getNativeAssetTokenMap();

        List<Long> listNativeAssets = new ArrayList<>(mapNativeAssets.keySet());

        for (long asset: listNativeAssets) {
            NativeAsset nativeAsset = this.asset.get(asset);

            if (nativeAsset.royaltyFraction != 0 && nativeAsset.quantity != 0) {
                return nativeAsset; // only one for valid offer
            }
        }

        return null;
    }

    public NativeAsset getAsset(long id) {
        if (id == 0 || asset == null || asset.size() == 0 || !asset.containsKey(id)) {
            return null;
        }

        return asset.get(id);
    }
}

