package concept.omno.service;

import concept.omno.ApplicationContext;
import concept.omno.object.Operation;
import concept.omno.object.PlatformToken;
import concept.omno.object.PlatformTokenRate;
import concept.utility.JsonFunction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashMap;

public class CollateralizedSwap {

    public State state;

    public CollateralizedSwap(ApplicationContext applicationContext) {
        state = new State(applicationContext);
    }

    public CollateralizedSwap(ApplicationContext applicationContext, JSONObject jsonObject) {
        state = new State(applicationContext, jsonObject);
    }

    public static class Instance {
        int id;
        long asset;
        PlatformToken assetForShare;
        long incomeAccount;
        PlatformTokenRate rate;
        HashMap<Long, Deposit> deposit = new HashMap<>();
        long depositCounter = 0;
        PlatformToken depositMinimum;
        double interestPercentage;
        long interestPeriodSeconds = (long) (365.2425 * 24 * 60 * 60);
        double interestPercentageMinimum;
        double collateralRatio = 1.5;
        PlatformToken depositTotal;

        Instance(JSONObject jsonObject) {
            define(jsonObject);
        }

        public boolean isValid() {
            return (id > 0 && asset != 0 && rate != null && rate.isValid() && depositCounter > 0 && interestPercentage >= 0 && interestPeriodSeconds > 0 || collateralRatio >= 1 && (assetForShare == null || (assetForShare.isValid() && assetForShare.countUniqueTokensAll() == 1 && assetForShare.getValueSum() == 1)));
        }

        public JSONObject toJSONObject() {
            JSONObject jsonObject = new JSONObject();

            JsonFunction.put(jsonObject, "id", id);

            JsonFunction.put(jsonObject, "asset", Long.toUnsignedString(asset));
            JsonFunction.put(jsonObject, "assetForShare", assetForShare.toJSONObject());
            JsonFunction.put(jsonObject, "incomeAccount", Long.toUnsignedString(incomeAccount));

            if (rate != null && rate.isValid()) {
                JsonFunction.put(jsonObject, "rate", rate.toJSONObject());
            }

            JsonFunction.put(jsonObject, "depositCounter", Long.toUnsignedString(depositCounter));

            if (depositMinimum != null && depositMinimum.isValid()) {
                JsonFunction.put(jsonObject, "depositMinimum", depositMinimum.toJSONObject());
            }

            JsonFunction.put(jsonObject, "interestPercentage", Double.toString(interestPercentage));
            JsonFunction.put(jsonObject, "interestPeriodSeconds", Long.toUnsignedString(interestPeriodSeconds));

            if (interestPercentageMinimum > 0) {
                JsonFunction.put(jsonObject, "interestPercentageMinimum", Double.toString(interestPercentageMinimum));
            }

            JsonFunction.put(jsonObject, "collateralRatio", Double.toString(collateralRatio));

            if (depositTotal != null && depositTotal.isValid()) {
                JsonFunction.put(jsonObject, "depositTotal", depositTotal.toJSONObject());
            }

            if (deposit != null && deposit.size() > 0) {
                JSONArray jsonArray = new JSONArray();

                for (Deposit item: deposit.values()) {
                    if (item.isValid()) {
                        JsonFunction.add(jsonArray, item.toJSONObject());
                    }
                }

                JsonFunction.put(jsonObject, "deposit", jsonArray);
            }

            return  jsonObject;
        }

        private boolean define(JSONObject jsonObject) {
            if (jsonObject == null) {
                return false;
            }

            id = JsonFunction.getInt(jsonObject, "id", -1);
            asset = JsonFunction.getLongFromStringUnsigned(jsonObject, "asset", 0);

            assetForShare = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "assetForShare", null));

            if (!assetForShare.isValid() || assetForShare.countUniqueTokensAll() != 1 || assetForShare.getValueSum() != 1) {
                assetForShare = null;
            }

            incomeAccount = JsonFunction.getLongFromStringUnsigned(jsonObject, "incomeAccount", 0);

            rate = new PlatformTokenRate(JsonFunction.getJSONObject(jsonObject, "rate", null));

            depositMinimum = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "depositMinimum", null));
            depositTotal = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "depositTotal", null));

            depositCounter = JsonFunction.getLongFromStringUnsigned(jsonObject, "depositCounter", 0);

            interestPercentage = JsonFunction.getDoubleFromString(jsonObject, "interestPercentage", 0);
            interestPeriodSeconds = JsonFunction.getLongFromStringUnsigned(jsonObject, "interestPeriodSeconds", interestPeriodSeconds);

            interestPercentageMinimum = JsonFunction.getDoubleFromString(jsonObject, "interestPercentageMinimum", 0);

            collateralRatio = JsonFunction.getDoubleFromString(jsonObject, "collateralRatio", 1.5);

            JSONArray jsonArray = JsonFunction.getJSONArray(jsonObject, "deposit", null);

            if (jsonArray != null && jsonArray.size() > 0) {
                for (Object object: jsonArray) {
                    if (! (object instanceof JSONObject)) {
                        continue;
                    }

                    JSONObject itemJson = (JSONObject) object;

                    Deposit item = new Deposit(itemJson);

                    if (item.isValid()) {
                        deposit.put(item.id, item);
                    }
                }
            }

            return true;
        }

        private boolean isMinimumDeposit(PlatformToken platformToken) {

            if (platformToken == null || !platformToken.isValid() || platformToken.isZero() || !depositMinimum.isValid()) {
                return false;
            }

            if (depositMinimum == null || depositMinimum.isZero()) {
                return true;
            }

            PlatformToken depositMinimumMasked = depositMinimum.clone();
            depositMinimumMasked.mask(platformToken);

            return platformToken.isGreaterOrEqual(depositMinimumMasked);
        }

        public boolean addDeposit(ApplicationContext applicationContext, PlatformToken amount, long account, int timestamp, boolean simulate) {
            if (!isValid() || amount == null || !amount.isValid() || amount.isZero() || timestamp <= 0) {
                return false;
            }

            if (!isMinimumDeposit(amount)) {
                return false;
            }

            PlatformToken amountForSwapValue = amount.clone();
            amountForSwapValue.mask(rate);

            PlatformToken amountLock = amountForSwapValue.clone();
            amountForSwapValue.multiply(rate);
            amountForSwapValue.multiply(1 / collateralRatio);

            long assetValue = amountForSwapValue.getValueSum();

            if (assetValue <= 0) {
                return false;
            }

            if (!applicationContext.state.userAccountState.hasRequiredBalance(account, amount)) {
                return false;
            }

            if (simulate) {
                return true;
            }

            applicationContext.state.userAccountState.subtractFromBalance(account, amount);

            applicationContext.state.nativeAssetState.issueIncrease(asset, assetValue, applicationContext.contractAccountId, false);

            Deposit item = new Deposit(++depositCounter, timestamp, account, assetValue, interestPercentage, interestPeriodSeconds, interestPercentageMinimum, amountLock);

            applicationContext.logDebugMessage("Omno | CollateralizedSwap deposit: " + item.toJSONObject().toJSONString());

            PlatformToken give = new PlatformToken();
            give.mergeNativeAssetToken(asset, assetValue, true);
            applicationContext.state.userAccountState.addToBalance(account, give);

            deposit.put(item.id, item);

            depositTotal.merge(item.locked, true);

            return true;
        }

        public boolean withdraw(ApplicationContext applicationContext, long id, long account) {

            if (!isValid() || deposit == null || deposit.size() == 0 || !deposit.containsKey(id)) {
                return false;
            }

            Deposit item = deposit.get(id);

            if (!item.isValid()) {
                return false;
            }

            int timeSeconds = applicationContext.state.economicCluster.getTimestamp() - item.timestamp;
            double interestPercentage = item.interestPercentage * timeSeconds / item.interestPeriodSeconds;

            if (interestPercentage < item.interestPercentageMinimum) {
                interestPercentage = item.interestPercentageMinimum;
            }

            long totalRepay = item.amount + (long) (item.amount * interestPercentage);

            if (totalRepay < item.amount) {
                // negative interest not implemented
                totalRepay = Long.MAX_VALUE;
            }

            PlatformToken amountRepay = new PlatformToken();
            amountRepay.mergeNativeAssetToken(asset, totalRepay, true);

            if (!applicationContext.state.userAccountState.hasRequiredBalance(account, amountRepay)) {
                return false;
            }

            PlatformToken amount = new PlatformToken();
            amount.mergeNativeAssetToken(asset, item.amount, true);

            PlatformToken income = amountRepay.clone();
            income.merge(amount, false);

            if (!income.isZero()) {
                applicationContext.logDebugMessage("Omno | CollateralizedSwap income: " + income.toJSONObject().toJSONString());
            }

            if (incomeAccount == 0) {
                incomeAccount = applicationContext.contractAccountId;
            }

            if (assetForShare != null) {
                PlatformToken remainder = applicationContext.state.userAccountState.distributeValueByTokenBalance(assetForShare, income);
                applicationContext.state.userAccountState.addToBalance(incomeAccount, remainder);
            } else {
                applicationContext.state.userAccountState.addToBalance(incomeAccount, income);
            }

            applicationContext.state.userAccountState.subtractFromBalance(account, amountRepay);
            applicationContext.state.nativeAssetState.decrease(asset, item.amount, applicationContext.contractAccountId);

            applicationContext.state.userAccountState.addToBalance(account, item.locked);
            depositTotal.merge(item.locked, false);

            deposit.remove(id);

            return true;
        }
    }

    public static class Deposit {
        long id;
        int timestamp;
        long account;
        long amount;
        double interestPercentage;
        long interestPeriodSeconds;
        double interestPercentageMinimum;
        PlatformToken locked;

        Deposit(long id, int timestamp, long account, long amount, double interestPercentage, long interestPeriodSeconds, double interestPercentageMinimum, PlatformToken locked) {
            this.id = id;
            this.timestamp = timestamp;
            this.account = account;
            this.amount = amount;
            this.interestPercentage = interestPercentage;
            this.interestPeriodSeconds = interestPeriodSeconds;
            this.interestPercentageMinimum = interestPercentageMinimum;

            if (locked != null && locked.isValid()) {
                this.locked = locked.clone();
            }
        }

        Deposit(JSONObject jsonObject) {
            define(jsonObject);
        }

        public boolean isValid() {
            return (id > 0 && timestamp > 0 && account != 0 && amount > 0 && interestPeriodSeconds > 0 && locked != null && locked.isValid() && !locked.isZero());
        }

        public JSONObject toJSONObject() {

            JSONObject jsonObject = new JSONObject();

            JsonFunction.put(jsonObject, "id", Long.toUnsignedString(id));
            JsonFunction.put(jsonObject, "timestamp", timestamp);
            JsonFunction.put(jsonObject, "account", Long.toUnsignedString(account));
            JsonFunction.put(jsonObject, "amount", Long.toUnsignedString(amount));
            JsonFunction.put(jsonObject, "interestPercentage", Double.toString(interestPercentage));
            JsonFunction.put(jsonObject, "interestPeriodSeconds", Long.toUnsignedString(interestPeriodSeconds));

            if (interestPercentageMinimum > 0) {
                JsonFunction.put(jsonObject, "interestPercentageMinimum", Double.toString(interestPercentageMinimum));
            }

            if (locked != null && locked.isValid()) {
                JsonFunction.put(jsonObject, "locked", locked.toJSONObject());
            }

            return  jsonObject;
        }

        private boolean define(JSONObject jsonObject) {

            if (jsonObject == null) {
                return false;
            }

            id = JsonFunction.getLongFromStringUnsigned(jsonObject, "id", 0);
            timestamp = JsonFunction.getInt(jsonObject, "timestamp", -1);
            account = JsonFunction.getLongFromStringUnsigned(jsonObject, "account", 0);
            amount = JsonFunction.getLongFromStringUnsigned(jsonObject, "amount", 0);

            interestPercentage = JsonFunction.getDoubleFromString(jsonObject, "interestPercentage", 0);
            interestPeriodSeconds = JsonFunction.getLongFromStringUnsigned(jsonObject, "interestPeriodSeconds", interestPeriodSeconds);

            interestPercentageMinimum = JsonFunction.getDoubleFromString(jsonObject, "interestPercentageMinimum", 0);

            locked = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "locked", null));

            return true;
        }
    }

    public static class State {

        final ApplicationContext applicationContext;

        HashMap<Integer, Instance> instance = new HashMap<>();

        long incomeAccount = 0;
        PlatformToken operationFee = new PlatformToken();

        State(ApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
            incomeAccount = applicationContext.contractAccountId;
        }

        State(ApplicationContext applicationContext, JSONObject jsonObject) {
            this(applicationContext);
            define(jsonObject);
        }

        public boolean isValid() {
            return true;
        }

        public JSONObject toJSONObject() {

            JSONObject jsonObject = new JSONObject();

            if (instance != null && instance.size() > 0) {
                JSONArray jsonArray = new JSONArray();

                for (Instance item: instance.values()) {
                    if (!item.isValid()) {
                        continue;
                    }

                    JsonFunction.add(jsonArray, item.toJSONObject());
                }

                JsonFunction.put(jsonObject, "instance", jsonArray);
            }

            JsonFunction.put(jsonObject, "incomeAccount", Long.toUnsignedString(incomeAccount));
            JsonFunction.put(jsonObject, "operationFee", operationFee.toJSONObject());

            return  jsonObject;
        }

        private boolean define(JSONObject jsonObject) {

            if (jsonObject == null) {
                return false;
            }

            JSONArray jsonArray = JsonFunction.getJSONArray(jsonObject, "instance", null);

            if (jsonArray != null && jsonArray.size() > 0) {

                for (Object object: jsonArray) {

                    if (! (object instanceof JSONObject)) {
                        continue;
                    }

                    Instance item = new Instance((JSONObject) object);

                    if (item.isValid()) {
                        instance.put(item.id, item);
                    }
                }
            }

            incomeAccount = JsonFunction.getLongFromStringUnsigned(jsonObject, "incomeAccount", applicationContext.contractAccountId);
            operationFee = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "operationFee", null));

            return true;
        }

        public void configure(JSONObject jsonObject) {

            if (jsonObject == null) {
                return;
            }

            incomeAccount = JsonFunction.getLongFromStringUnsigned(jsonObject, "incomeAccount", incomeAccount);
            JSONObject feeObject = JsonFunction.getJSONObject(jsonObject, "operationFee", null);

            if (feeObject != null) {
                PlatformToken newFee = new PlatformToken(feeObject);

                if (newFee.isValid()) {
                    operationFee = newFee;
                }
            }
        }

        public void configureInstance(JSONObject jsonObject) {

            if (jsonObject == null) {
                return;
            }

            Instance instance = new Instance(jsonObject);

            if (!instance.isValid()) {
                applicationContext.logDebugMessage("Omno | configureInstance invalid: " + instance.toJSONObject().toJSONString());
                return;
            }

            Instance instanceOld = getInstance(instance.id);

            if (instanceOld != null && instanceOld.isValid() && instanceOld.deposit != null && instanceOld.deposit.size() != 0) {
                instance.deposit = instanceOld.deposit;
                instance.depositTotal = instanceOld.depositTotal;

                instance.rate = instanceOld.rate;
            }

            this.instance.put(instance.id, instance);
        }

        public void configureInstanceRate(JSONObject jsonObject) {

            if (jsonObject == null) {
                return;
            }

            int instanceId = JsonFunction.getInt(jsonObject, "id", -1);

            Instance instance = getInstance(instanceId);

            if (instance == null || !instance.isValid()) {
                return;
            }

            JSONObject rateJson = JsonFunction.getJSONObject(jsonObject, "rate", null);

            if (rateJson == null) {
                return;
            }

            PlatformTokenRate rate = new PlatformTokenRate(rateJson);

            if (!rate.isValid()) {
                return;
            }

            instance.rate = rate;
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

                case "configureInstance": {
                    result = operationConfigureInstance(operation);
                    break;
                }

                case "configureInstanceRate": {
                    result = operationConfigureInstanceRate(operation);
                    break;
                }

                case "deposit": {
                    result = operationDeposit(operation);
                    break;
                }

                case "withdraw": {
                    result = operationWithdraw(operation);
                    break;
                }
            }

            return result;
        }

        private boolean operationConfigure(Operation operation) {

            if (operation == null || operation.parameterJson == null) {
                return false;
            }

            if (operation.account != applicationContext.contractAccountId) {
                return false;
            }

            configure(operation.parameterJson);

            return true;
        }

        private boolean operationConfigureInstance(Operation operation) {

            if (operation == null || operation.parameterJson == null) {
                return false;
            }

            if (operation.account != applicationContext.contractAccountId) {
                return false;
            }

            configureInstance(operation.parameterJson);

            return true;
        }

        private boolean operationConfigureInstanceRate(Operation operation) {

            if (operation == null || operation.parameterJson == null) {
                return false;
            }

            if (operation.account != applicationContext.contractAccountId) {
                return false;
            }

            configureInstanceRate(operation.parameterJson);

            return true;
        }

        private boolean operationDeposit(Operation operation) {

            if (operation == null || operation.parameterJson == null) {
                return false;
            }

            JSONObject jsonObject = operation.parameterJson;

            int id = JsonFunction.getInt(jsonObject , "id", -1);

            if (id <= 0) {
                return false;
            }

            PlatformToken amount = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "amount", null));

            return addDeposit(id, amount, operation.account);
        }

        private boolean operationWithdraw(Operation operation) {

            if (operation == null || operation.parameterJson == null) {
                return false;
            }

            int instanceId = JsonFunction.getInt(operation.parameterJson, "id", -1);

            Instance instance = getInstance(instanceId);

            if (instance == null) {
                return false;
            }

            return instance.withdraw(applicationContext, instanceId, operation.account);
        }

        public boolean addDeposit(int id, PlatformToken amount, long account) {

            if (amount == null || !amount.isValid() || amount.isZero()) {
                return false;
            }

            Instance instance = getInstance(id);

            if (instance == null) {
                return false;
            }

            if (!applicationContext.state.nativeAssetState.canTransfer(amount)) {
                return false;
            }

            if (!applicationContext.state.userAccountState.hasRequiredBalance(account, amount)) {
                return false;
            }

            if (!instance.isMinimumDeposit(amount)) {
                return false;
            }

            if (!instance.addDeposit(applicationContext, amount, account, applicationContext.state.economicCluster.getTimestamp(), true)) {
                return false;
            }

            instance.addDeposit(applicationContext, amount, account, applicationContext.state.economicCluster.getTimestamp(), false);

            return true;
        }

        public boolean processBlock() {
            return true;
        }

        public Instance getInstance(int id) {

            if (id <= 0 || instance.size() == 0 || !instance.containsKey(id)) {
                return null;
            }

            Instance result = instance.get(id);

            if (!result.isValid()) {
                return null;
            }

            return result;
        }

        public JSONObject apiProcessRequestInstance(JSONObject jsonObject) {

            JSONObject result = new JSONObject();

            if (jsonObject == null) {
                JsonFunction.put(result, "error", "missing parameter");
                return result;
            }

            int id = JsonFunction.getInt(jsonObject, "id", -1);

            if (id <= 0) {
                JsonFunction.put(result, "error", "invalid id");
                return result;
            }

            Instance item = getInstance(id);

            if (item == null) {
                JsonFunction.put(result, "error", "invalid id");
                return result;
            }

            result = item.toJSONObject();

            return result;
        }
    }
}
