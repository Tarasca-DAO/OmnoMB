package concept.omno.service;

import concept.omno.ApplicationContext;
import concept.omno.object.Operation;
import concept.omno.object.PlatformToken;
import concept.utility.JsonFunction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PlatformSwap {
    public State state;

    public PlatformSwap(ApplicationContext applicationContext) {
        this.state = new State(applicationContext);
    }

    public PlatformSwap(ApplicationContext applicationContext, JSONObject jsonObject) {
        this.state = new State(applicationContext, jsonObject);
    }

    public static class Platform {
        int id;
        int accountParameterSize;
        String name;
        PlatformToken depositMinimum;
        List<Deposit> deposit = new ArrayList<>();
        PlatformToken depositTotal = new PlatformToken();

        Platform(int id, int accountParameterSize, String name, PlatformToken depositMinimum) {
            this.id = id;
            this.accountParameterSize = accountParameterSize;
            this.name = name;

            if (depositMinimum == null || !depositMinimum.isValid()) {
                this.depositMinimum = new PlatformToken();
            } else {
                this.depositMinimum = depositMinimum;
            }
        }

        Platform(JSONObject jsonObject) {
            define(jsonObject);
        }

        public JSONObject toJSONObject() {
            JSONObject jsonObject = new JSONObject();

            JsonFunction.put(jsonObject, "id", id);

            JsonFunction.put(jsonObject, "accountParameterSize", accountParameterSize);

            if (name != null && !name.equals("")) {
                JsonFunction.put(jsonObject, "name", name);
            }

            if (depositMinimum != null && depositMinimum.isValid()) {
                JsonFunction.put(jsonObject, "depositMinimum", depositMinimum.toJSONObject());
            }

            JsonFunction.put(jsonObject, "depositTotal", depositTotal.toJSONObject());

            if (deposit != null && deposit.size() != 0) {

                JSONArray jsonArray = new JSONArray();

                for (Deposit item: deposit) {
                    if (item.isValid()) {
                        JsonFunction.add(jsonArray, item.toJSONObject());
                    }
                }

                JsonFunction.put(jsonObject, "deposit", jsonArray);
            }

            return  jsonObject;
        }

        public boolean define(JSONObject jsonObject) {
            if (jsonObject == null) {
                return false;
            }

            id = JsonFunction.getInt(jsonObject, "id", -1);

            if (id < 1) {
                return false;
            }

            accountParameterSize = JsonFunction.getInt(jsonObject, "accountParameterSize", -1);

            if (accountParameterSize < 0) {
                return false;
            }

            name = JsonFunction.getString(jsonObject, "name", null);

            depositMinimum = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "depositMinimum", null));

            depositTotal = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "depositTotal", null));

            JSONArray jsonArray = JsonFunction.getJSONArray(jsonObject, "deposit", null);

            if (jsonArray != null) {
                for (Object object: jsonArray) {
                    Deposit item = new Deposit((JSONObject) object);

                    if (item.isValid()) {
                        deposit.add(item);
                    }
                }
            }

            return true;
        }

        public boolean isValid() {
            return (id > 0 && accountParameterSize >= 0 && depositMinimum != null && depositMinimum.isValid());
        }

        public boolean addDeposit(PlatformToken amount, long account, String recipient, int height, boolean simulate) {
            if (amount == null || !amount.isValid() || amount.isZero() || height <= 0) {
                return false;
            }

            if (!amount.isGreaterOrEqual(depositMinimum)) {
                return false;
            }

            if (accountParameterSize > 0) {
                if (recipient == null || recipient.length() != accountParameterSize) {
                    return false;
                }
            }

            if (simulate) {
                return true;
            }

            Deposit item = new Deposit(account, recipient, amount, height);

            deposit.add(item);

            depositTotal.merge(item.amount, true);

            return true;
        }

        public void removeDepositExpired(int height, int confirmationsMaximum) {

            if (this.deposit == null || deposit.size() == 0) {
                return;
            }

            boolean complete = false;

            while (deposit.size() > 0 && !complete) {
                int confirmations = height - deposit.get(0).height;

                if (confirmations > confirmationsMaximum) {
                    deposit.remove(0);
                } else {
                    complete = true;
                }
            }
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

            return depositMinimumMasked.isGreaterOrEqual(platformToken);
        }
    }

    public static class Deposit {
        long account;
        String recipient;
        PlatformToken amount;
        int height;

        Deposit(long account, String recipient, PlatformToken amount, int height) {
            this.account = account;
            this.recipient = recipient;
            this.amount = amount;
            this.height = height;
        }

        public Deposit(JSONObject jsonObject) {
            define(jsonObject);
        }

        public boolean isValid() {
            return (account != 0 && amount.isValid() && !amount.isZero() && height > 0);
        }

        public JSONObject toJSONObject() {
            JSONObject jsonObject = new JSONObject();

            JsonFunction.put(jsonObject, "account", Long.toUnsignedString(account));

            if (recipient != null && recipient.length() != 0) {
                JsonFunction.put(jsonObject, "recipient", recipient);
            }

            if (amount != null && amount.isValid() && !amount.isZero()) {
                JsonFunction.put(jsonObject, "amount", amount.toJSONObject());
            }

            JsonFunction.put(jsonObject, "height", height);

            return  jsonObject;
        }

        public boolean define(JSONObject jsonObject) {

            if (jsonObject == null) {
                return false;
            }

            height = JsonFunction.getInt(jsonObject, "height", 0);

            if (height <= 0) {
                return false;
            }

            account = JsonFunction.getLongFromStringUnsigned(jsonObject, "account", 0);

            if (account == 0) {
                return false;
            }

            recipient = JsonFunction.getString(jsonObject, "recipient", null);

            amount = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "amount", null));

            if (!amount.isValid() || amount.isZero()) {
                return false;
            }

            return true;
        }
    }

    public static class State {
        HashMap<Integer, Platform> platform = new HashMap<>();

        int confirmationsMinimum = 0;
        int confirmationsMaximum = 14400;

        long incomeAccount = 0;
        PlatformToken operationFee = new PlatformToken();

        ApplicationContext applicationContext;

        State(ApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
        }

        State(ApplicationContext applicationContext, JSONObject jsonObject) {
            this(applicationContext);
            define(jsonObject);
        }

        public JSONObject toJSONObject() {
            JSONObject jsonObject = new JSONObject();

            JSONArray jsonArray = new JSONArray();

            for (Platform platform: platform.values()) {
                if (platform.isValid()) {
                    JsonFunction.add(jsonArray, platform.toJSONObject());
                }
            }

            JsonFunction.put(jsonObject, "platform", jsonArray);

            JsonFunction.put(jsonObject, "incomeAccount", Long.toUnsignedString(incomeAccount));
            JsonFunction.put(jsonObject, "operationFee", operationFee.toJSONObject());

            JsonFunction.put(jsonObject, "confirmationsMinimum", confirmationsMinimum);
            JsonFunction.put(jsonObject, "confirmationsMaximum", confirmationsMaximum);

            return jsonObject;
        }

        public boolean define(JSONObject jsonObject) {
            if (jsonObject == null) {
                return false;
            }

            JSONArray jsonArray = JsonFunction.getJSONArray(jsonObject, "platform", null);

            if (jsonArray == null || jsonArray.size() == 0) {
                return false;
            }

            if (platform == null) {
                platform = new HashMap<>();
            }

            for (Object object: jsonArray) {
                Platform item = new Platform((JSONObject) object);

                if (item.isValid()) {
                   platform.put(item.id, item);
                }
            }

            incomeAccount = JsonFunction.getLongFromStringUnsigned(jsonObject, "incomeAccount", applicationContext.contractAccountId);
            operationFee = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "operationFee", null));

            confirmationsMinimum = JsonFunction.getInt(jsonObject, "confirmationsMinimum", 0);
            confirmationsMaximum = JsonFunction.getInt(jsonObject, "confirmationsMaximum", 14400);

            return platform.size() != 0;
        }

        public void configure(JSONObject jsonObject) {
            if (jsonObject == null) {
                return;
            }

            confirmationsMinimum = JsonFunction.getInt(jsonObject, "confirmationsMinimum", confirmationsMinimum);
            confirmationsMaximum = JsonFunction.getInt(jsonObject, "confirmationsMaximum", confirmationsMaximum);

            incomeAccount = JsonFunction.getLongFromStringUnsigned(jsonObject, "incomeAccount", incomeAccount);
            JSONObject feeObject = JsonFunction.getJSONObject(jsonObject, "operationFee", null);

            if (feeObject != null) {
                PlatformToken newFee = new PlatformToken(feeObject);

                if (newFee.isValid()) {
                    operationFee = newFee;
                }
            }
        }

        public void configurePlatform(JSONObject jsonObject) {
            if (jsonObject == null) {
                return;
            }

            Platform platform = new Platform(jsonObject);

            if (!platform.isValid()) {
                return;
            }

            Platform platformOld = getPlatform(platform.id);

            if (platformOld != null && platformOld.isValid() && platformOld.deposit != null && platformOld.deposit.size() != 0) {
                platform.deposit = platformOld.deposit;
                platform.depositTotal = platformOld.depositTotal;
            }

            this.platform.put(platform.id, platform);
        }

        public boolean processOperation(Operation operation) {
            boolean result = false;

            if (operation == null || !operation.service.equals("platformSwap")) {
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

                case "configurePlatform": {
                    result = operationConfigurePlatform(operation);
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

        private boolean operationConfigurePlatform(Operation operation) {

            if (operation == null || operation.parameterJson == null) {
                return false;
            }

            if (operation.account != applicationContext.contractAccountId) {
                return false;
            }

            configurePlatform(operation.parameterJson);

            return true;
        }

        private boolean operationDeposit(Operation operation) {

            if (operation == null || operation.parameterJson == null) {
                return false;
            }

            JSONObject jsonObject = operation.parameterJson;

            int id = JsonFunction.getInt(jsonObject , "platformId", -1);

            if (id <= 0) {
                return false;
            }

            String recipient = JsonFunction.getString(jsonObject, "recipient", null);

            PlatformToken amount = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "amount", null));

            return addDeposit(id, amount, operation.account, recipient);
        }

        private boolean operationWithdraw(Operation operation) {
            if (operation == null || operation.parameterJson == null || operation.account != incomeAccount) {
                return false;
            }

            int platformId = JsonFunction.getInt(operation.parameterJson, "platformId", -1);

            Platform platform = getPlatform(platformId);

            if (platform == null) {
                return false;
            }

            JSONObject jsonObject = JsonFunction.getJSONObject(operation.parameterJson, "value", null);

            if (jsonObject == null) {
                return false;
            }

            PlatformToken platformToken = new PlatformToken(jsonObject);

            if (!platformToken.isValid()) {
                return false;
            }

            PlatformToken depositTotal = platform.depositTotal.clone();
            depositTotal.merge(platformToken, false);

            if (!depositTotal.isValid()) {
                return false;
            }

            long recipient = JsonFunction.getLongFromStringUnsigned(operation.parameterJson, "account", 0);
            String message = JsonFunction.getString(operation.parameterJson, "message", null);
            String contract = JsonFunction.getString(operation.parameterJson, "contract", null);
            JSONObject contractOperation = JsonFunction.getJSONObject(operation.parameterJson, "contractOperation", null);

            if (!applicationContext.state.userAccountState.withdraw(operation.account, platformToken, recipient, message, contract, contractOperation, false)) {
                return false;
            }

            platform.depositTotal = depositTotal;

            return true;
        }

        public boolean addDeposit(int id, PlatformToken amount, long account, String recipient) {
            if (amount == null || !amount.isValid() || amount.isZero()) {
                return false;
            }

            Platform platform = getPlatform(id);

            if (platform == null) {
                return false;
            }

            if (!amount.isValid() || !applicationContext.state.nativeAssetState.canTransfer(amount)) {
                return false;
            }

            if (!applicationContext.state.userAccountState.hasRequiredBalance(account, amount)) {
                return false;
            }

            if (platform.accountParameterSize > 0) {
                if (recipient == null || recipient.length() != platform.accountParameterSize) {
                    return false;
                }
            }

            if (!platform.addDeposit(amount, account, recipient, applicationContext.state.economicCluster.getHeight(), true)) {
                return false;
            }

            if (!applicationContext.state.userAccountState.subtractFromBalance(account, amount)) {
                return false;
            }

            platform.addDeposit(amount, account, recipient, applicationContext.state.economicCluster.getHeight(), false);

            applicationContext.state.userAccountState.addToBalance(incomeAccount, amount);

            return true;
        }

        public boolean processBlock() {

            if (platform == null || platform.size() == 0) {
                return false;
            }

            int height = applicationContext.state.economicCluster.getHeight();

            for (Platform platform: platform.values()) {
                platform.removeDepositExpired(height, confirmationsMaximum);
            }

            return true;
        }

        public Platform getPlatform(int id) {

            if (id <= 0 || platform.size() == 0 || !platform.containsKey(id)) {
                return null;
            }

            Platform result = platform.get(id);

            if (!result.isValid()) {
                return null;
            }

            return result;
        }

        public JSONObject apiProcessRequestPlatform(JSONObject jsonObject) {
            JSONObject result = new JSONObject();

            if (jsonObject == null) {
                JsonFunction.put(result, "error", "missing parameter");
                return result;
            }

            int id = JsonFunction.getInt(jsonObject, "platformId", -1);

            if (id <= 0) {
                JsonFunction.put(result, "error", "invalid id");
                return result;
            }

            Platform platform = getPlatform(id);

            if (platform == null) {
                JsonFunction.put(result, "error", "invalid id");
                return result;
            }

            result = platform.toJSONObject();

            return result;
        }
    }
}
