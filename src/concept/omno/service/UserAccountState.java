package concept.omno.service;

import concept.omno.ApplicationContext;
import concept.omno.object.Operation;
import concept.omno.object.PlatformToken;
import concept.omno.object.UserAccount;
import concept.omno.service.PlatformTokenExchangeById.Offer;
import concept.platform.EconomicCluster;
import concept.utility.JsonFunction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UserAccountState {
    final private ApplicationContext applicationContext;

    HashMap<Long, UserAccount> mapAccount = new HashMap<>();

    long incomeAccount;
    public PlatformToken withdrawFeeNQT = new PlatformToken();
    PlatformToken withdrawMinimumNQT = new PlatformToken();
    public PlatformToken operationFee = new PlatformToken();

    short transactionDeadline = Short.MAX_VALUE;

    int withdrawIndex;
    EconomicCluster economicCluster = null;

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();

        JSONArray jsonArray = new JSONArray();

        for (UserAccount value: mapAccount.values()) {
            JsonFunction.add(jsonArray, value.toJSONObject());
        }

        JsonFunction.put(jsonObject, "account", jsonArray);

        JsonFunction.put(jsonObject, "incomeAccount", Long.toUnsignedString(incomeAccount));
        JsonFunction.put(jsonObject, "withdrawFeeNQT", withdrawFeeNQT.toJSONObject());
        JsonFunction.put(jsonObject, "withdrawMinimumNQT", withdrawMinimumNQT.toJSONObject());
        JsonFunction.put(jsonObject, "operationFee", operationFee.toJSONObject());

        JsonFunction.put(jsonObject, "transactionDeadline", (int) transactionDeadline);

        return jsonObject;
    }

    public void define(JSONObject jsonObject) {
        if (jsonObject == null) {
            return;
        }

        incomeAccount = JsonFunction.getLongFromStringUnsigned(jsonObject, "incomeAccount", applicationContext.contractAccountId);
        withdrawFeeNQT = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "withdrawFeeNQT", null));
        withdrawMinimumNQT = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "withdrawMinimumNQT", null));
        operationFee = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "operationFee", null));

        transactionDeadline = (short) JsonFunction.getInt(jsonObject, "transactionDeadline", 0);

        List<JSONObject> list = JsonFunction.getListJSONObject(jsonObject, "account", null);

        mapAccount = new HashMap<>();

        if (list != null) {
            for(JSONObject object: list) {
                UserAccount item = new UserAccount(object);

                mapAccount.put(item.id, item);
            }
        }
    }

    public UserAccountState(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        incomeAccount = applicationContext.contractAccountId;
    }

    public UserAccountState(ApplicationContext applicationContext, JSONObject jsonObject) {
        this.applicationContext = applicationContext;
        define(jsonObject);
    }

    public void configure(JSONObject jsonObject) {
        if (jsonObject == null) {
            return;
        }

        incomeAccount = JsonFunction.getLongFromStringUnsigned(jsonObject, "incomeAccount", applicationContext.contractAccountId);
        withdrawFeeNQT.define(JsonFunction.getJSONObject(jsonObject, "withdrawTransactionFeeNQT", null));
        withdrawMinimumNQT.define(JsonFunction.getJSONObject(jsonObject, "withdrawMinimumNQT", null));

        JSONObject feeObject = JsonFunction.getJSONObject(jsonObject, "operationFee", null);

        if (feeObject != null) {
            PlatformToken newFee = new PlatformToken(feeObject);

            if (newFee.isValid()) {
                operationFee = newFee;
            }
        }
    }

    public boolean processOperation(Operation operation) {
        boolean result = false;

        if (operation == null || !operation.service.equals("user")) {
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

            case "withdraw": {
                result = operationWithdraw(operation);
                break;
            }

            case "withdrawAll": {
                result = operationWithdrawAll(operation);
                break;
            }

            case "transfer": {
                result = operationPlatformTokenTransfer(operation);
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

    public UserAccount getUserAccount(long id) {
        UserAccount result = null;

        if (mapAccount.containsKey(id)) {
            result = mapAccount.get(id);
        }

        return result;
    }

    public long getBalanceNativeAsset(long id) {
        long value = 0;

        if (mapAccount.containsKey(id)) {
            UserAccount userAccount = mapAccount.get(id);
            value = userAccount.getBalanceNativeAsset(id);
        }

        return value;
    }


    public long getBalanceByUniqueAsset(long id, PlatformToken platformToken) {
        if (platformToken == null || !platformToken.isValid() || platformToken.isZero()) {
            return 0;
        }

        if (!mapAccount.containsKey(id)) {
            return 0;
        }

        UserAccount userAccount = mapAccount.get(id);
        PlatformToken balance = userAccount.balance;

        long result = 0;

        if (balance != null) {
            result = balance.getValueByUniqueId(platformToken);
        }

        return result;
    }

    private boolean operationPlatformTokenTransfer(Operation operation) {

        if (operation == null || operation.parameterJson == null) {
            return false;
        }

        JSONObject jsonObject = operation.parameterJson;

        long recipient = JsonFunction.getLongFromStringUnsigned(jsonObject , "account", 0);

        if (recipient == 0) {
            return false;
        }

        PlatformToken give = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "give", null));

        if (!give.isValid() || !applicationContext.state.nativeAssetState.canTransfer(give)) {
            return false;
        }

        if (!subtractFromBalance(operation.account, give)) {
            return false;
        }

        addToBalance(recipient, give);

        return true;
    }

    private boolean operationWithdraw(Operation operation) {

        if (operation == null || operation.parameterJson == null) {
            return false;
        }

        JSONObject jsonObject = JsonFunction.getJSONObject(operation.parameterJson, "value", null);

        if (jsonObject == null) {
            return false;
        }

        PlatformToken platformToken = new PlatformToken(jsonObject);

        if (!platformToken.isValid() && !platformToken.isZero()) {
            return false;
        }

        long recipient = JsonFunction.getLongFromStringUnsigned(operation.parameterJson, "account", 0);
        String message = JsonFunction.getString(operation.parameterJson, "message", null);

        String contract = null;
        JSONObject contractOperation = null;

        if (operation.account == applicationContext.contractAccountId) {
            contract = JsonFunction.getString(operation.parameterJson, "contract", null);
            contractOperation = JsonFunction.getJSONObject(operation.parameterJson, "contractOperation", null);
        }

        return withdraw(operation.account, platformToken, recipient, message, contract, contractOperation, false);
    }

    private boolean operationWithdrawAll(Operation operation) {

        if (operation == null) {
            return false;
        }

        UserAccount userAccount = applicationContext.state.userAccountState.getUserAccount(operation.account);

        if (userAccount == null || userAccount.balance.isZero()) {
            return false;
        }

        PlatformToken balance = userAccount.balance.clone();
        balance.applyTransactionFee(withdrawFeeNQT, applicationContext.transactionChain, false);

        long recipient = JsonFunction.getLongFromStringUnsigned(operation.parameterJson, "account", 0);
        String message = JsonFunction.getString(operation.parameterJson, "message", null);

        String contract = null;
        JSONObject contractOperation = null;

        if (operation.account == applicationContext.contractAccountId) {
            contract = JsonFunction.getString(operation.parameterJson, "contract", null);
            contractOperation = JsonFunction.getJSONObject(operation.parameterJson, "contractOperation", null);
        }

        return withdraw(operation.account, balance, recipient, message, contract, contractOperation, false);
    }

    public boolean withdraw(long account, PlatformToken platformToken, long recipient, String messageExtraData, String contract, JSONObject contractOperation, boolean contractPaysWithdrawFee) {

        if (account == 0 || !platformToken.isValid()) {
            return false;
        }

        if (recipient == 0) {
            recipient = account;
        }

        platformToken.removeZeroBalanceAll();

        PlatformToken costTotal = platformToken.clone();

        costTotal.applyTransactionFee(withdrawFeeNQT, applicationContext.transactionChain, true);

        PlatformToken feeTotal = costTotal.clone();
        feeTotal.merge(platformToken, false);

        if (contractPaysWithdrawFee) {
            costTotal = platformToken.clone();
        }

        if (! hasRequiredBalance(account, costTotal)) {
            return false;
        }

        if (contractPaysWithdrawFee) {
            if (! hasRequiredBalance(applicationContext.contractAccountId, feeTotal)) {
                return false;
            }
        }

        subtractFromBalance(account, costTotal);
        subtractFromBalance((applicationContext.contractAccountId), withdrawFeeNQT);

        JSONObject messageForAttachment = new JSONObject();
        JsonFunction.put(messageForAttachment, "submittedBy", applicationContext.contractName);

        if (messageExtraData != null && messageExtraData.length() != 0) {
            JsonFunction.put(messageForAttachment, "message", messageExtraData);
        }

        JsonFunction.put(messageForAttachment, "withdrawIndex", getWithdrawIndex());

        if (contract != null && contract.length() != 0) {
            JsonFunction.put(messageForAttachment, "contract", contract);
        }

        if (contractOperation != null) {
            JsonFunction.put(messageForAttachment, "operation", contractOperation);
        }

        String message = messageForAttachment.toString();

        if (platformToken.getChainTokenMap() != null && platformToken.getChainTokenMap().size() != 0) {

            List<Long> listChainId = new ArrayList<>(platformToken.getChainTokenMap().keySet());
            List<Long> listChainAmountNQT = new ArrayList<>(platformToken.getChainTokenMap().values());
            int countChainTokens = listChainId.size();


            for (int i = 0; i < countChainTokens; i++) {
                long chainId = listChainId.get(i);
                long amountNQT = listChainAmountNQT.get(i);
                long feeNQT = withdrawFeeNQT.getChainTokenValue(chainId);

                JSONObject jsonObject;

                jsonObject = applicationContext.ardorApi.sendMoney(applicationContext.state.economicCluster.height, applicationContext.state.economicCluster.blockId, applicationContext.state.economicCluster.timestamp, transactionDeadline, null, applicationContext.nxtCryptography.getPublicKey(), recipient, (int) chainId, feeNQT, message, false, amountNQT);

                if (applicationContext.nxtCryptography != null && applicationContext.nxtCryptography.hasPrivateKey() && jsonObject == null) {
                    applicationContext.logErrorMessage("error: sendMoney: " + contractOperation);
                    continue;
                }

                applicationContext.broadcastTransaction(jsonObject);
            }
        }

        if (platformToken.getAssetTokenMap() != null && platformToken.getAssetTokenMap().size() != 0) {

            List<Long> listAssetId = new ArrayList<>(platformToken.getAssetTokenMap().keySet());
            List<Long> listAssetAmountNQT = new ArrayList<>(platformToken.getAssetTokenMap().values());
            int countAssetTokens = listAssetId.size();

            long feeNQT = withdrawFeeNQT.getChainTokenValue(applicationContext.transactionChain);

            for (int i = 0; i < countAssetTokens; i++) {
                long assetId = listAssetId.get(i);
                long amountNQT = listAssetAmountNQT.get(i);

                JSONObject jsonObject;

                jsonObject = applicationContext.ardorApi.transferAsset(applicationContext.state.economicCluster.height, applicationContext.state.economicCluster.blockId, applicationContext.state.economicCluster.timestamp, transactionDeadline, null, applicationContext.nxtCryptography.getPublicKey(), account, applicationContext.transactionChain, feeNQT, message, false, assetId, amountNQT);

                if (applicationContext.nxtCryptography != null && applicationContext.nxtCryptography.hasPrivateKey() && jsonObject == null) {
                    applicationContext.logErrorMessage("error: transferAsset: " + contractOperation);
                    continue;
                }

                applicationContext.broadcastTransaction(jsonObject);
            }
        }

        return true;
    }

    public boolean hasRequiredBalance(Offer offer) {

        if (offer == null || ! offer.isValid(applicationContext.state.nativeAssetState)) {
            return false;
        }

        PlatformToken giveTotal = offer.give.clone();
        giveTotal.multiply(offer.multiplier);

        if (!giveTotal.isValid()) {
            return false;
        }

        return hasRequiredBalance(offer.account, giveTotal);
    }

    public boolean hasRequiredBalance(long account, PlatformToken platformToken) {

        if (account == 0 || platformToken == null || ((!mapAccount.containsKey(account)) && platformToken.isZero())) {
            return true;
        }

        if (! mapAccount.containsKey(account)) {
            return false;
        }

        UserAccount userAccount = mapAccount.get(account);

        return userAccount.hasRequiredBalance(platformToken);
    }

    public boolean hasRequiredBalanceLocked(long account, PlatformToken platformToken) {

        if (account == 0 || platformToken == null || ((!mapAccount.containsKey(account)) && platformToken.isZero())) {
            return true;
        }

        if (! platformToken.isValid() || ! mapAccount.containsKey(account)) {
            return false;
        }

        UserAccount userAccount = mapAccount.get(account);

        return userAccount.hasRequiredBalanceLocked(platformToken);
    }

    public boolean subtractFromBalance(long account, PlatformToken platformToken) {

        if (account != 0 && platformToken != null && platformToken.isZero()) {
            return true;
        }

        if (account == 0 || platformToken == null || ! platformToken.isValid() || ! mapAccount.containsKey(account)) {
            return false;
        }

        UserAccount userAccount = mapAccount.get(account);

        if (!userAccount.hasRequiredBalance(platformToken)) {
            return false;
        }

        userAccount.subtractFromBalance(platformToken);

        return true;
    }

    public void subtractFromBalanceLocked(long account, PlatformToken platformToken) {

        if (platformToken == null || ! platformToken.isValid() || ! mapAccount.containsKey(account)) {
            return;
        }

        UserAccount userAccount = mapAccount.get(account);

        userAccount.subtractFromBalanceLocked(platformToken);
    }

    public void addToBalance(long account, PlatformToken platformToken) {

        if (account == 0 || platformToken == null || !platformToken.isValid()) {
            return;
        }

        UserAccount userAccount;

        if (! mapAccount.containsKey(account)) {
            userAccount = new UserAccount(account, null);
            mapAccount.put(account, userAccount);
        } else {
            userAccount = mapAccount.get(account);
        }

        userAccount.addToBalance(platformToken);
    }

    public void addToBalanceLocked(long account, PlatformToken platformToken) {

        if (account == 0 || platformToken == null || !platformToken.isValid()) {
            return;
        }

        UserAccount userAccount;

        if (! mapAccount.containsKey(account)) {
            userAccount = new UserAccount(account, platformToken);
        } else {
            userAccount = mapAccount.get(account);
            userAccount.addToBalanceLocked(platformToken);
        }

        mapAccount.put(account, userAccount);
    }

    public boolean lockBalance(long account, PlatformToken platformToken) {

        if (account == 0 || platformToken == null || !platformToken.isValid() || !hasRequiredBalance(account, platformToken)) {
            return false;
        }

        UserAccount userAccount = mapAccount.get(account);

        userAccount.subtractFromBalance(platformToken);
        userAccount.addToBalanceLocked(platformToken);

        return true;
    }

    public boolean unlockBalance(long account, PlatformToken platformToken) {

        if (account == 0 || platformToken == null || !platformToken.isValid() || !hasRequiredBalanceLocked(account, platformToken)) {
            return false;
        }

        UserAccount userAccount = mapAccount.get(account);

        userAccount.subtractFromBalanceLocked(platformToken);
        userAccount.addToBalance(platformToken);

        return true;
    }

    public long getChainTokenSum(long id) {

        if (mapAccount == null || mapAccount.size() == 0) {
            return 0;
        }

        long result = 0;

        for (UserAccount userAccount: mapAccount.values()) {
            if (userAccount == null) {
                continue;
            }

            HashMap<Long, Long> map = userAccount.balance.getChainTokenMap();

            if (map == null || map.size() == 0 || !map.containsKey(id)) {
                continue;
            }

            result += map.get(id);
        }

        return result;
    }

    public long totalTokenBalance(PlatformToken token) {

        if (token == null || !token.isValid() || token.countUniqueTokensAll() != 1 || mapAccount == null || mapAccount.size() == 0) {
            return 0;
        }

        long result = 0;

        for (UserAccount userAccount: mapAccount.values()) {
            PlatformToken balance = userAccount.balance;

            if (balance == null) {
                continue;
            }

            result += userAccount.balance.getValueByUniqueId(token);
        }

        return result;
    }

    public PlatformToken distributeValueByTokenBalance(PlatformToken shareToken, PlatformToken amount) {

        if (shareToken == null || !shareToken.isValid() || shareToken.countUniqueTokensAll() != 1 || amount == null || !amount.isValid() || amount.isZero()) {
            return amount;
        }

        long totalShareValue = totalTokenBalance(shareToken);

        for (UserAccount userAccount: mapAccount.values()) {
            PlatformToken balance = userAccount.balance;

            if (balance == null) {
                continue;
            }

            PlatformToken accountBalance = userAccount.balance;

            long accountShareTokenValue = accountBalance.getValueByUniqueId(shareToken);

            if (accountShareTokenValue <= 0) {
                continue;
            }

            double multiplier = BigInteger.valueOf(accountShareTokenValue).multiply(BigInteger.valueOf(Long.MAX_VALUE)).divide(BigInteger.valueOf(totalShareValue)).doubleValue() / Long.MAX_VALUE;
            totalShareValue -= accountShareTokenValue;

            PlatformToken dividend = amount.clone();

            dividend.multiply(multiplier);


            if (!dividend.isValid()) {
                break;
            }

            applicationContext.logDebugMessage("Amount: " + amount.toJSONObject().toJSONString());


            if (! amount.isGreaterOrEqual(dividend)) {
                dividend = amount.clone();
            }

            amount.merge(dividend, false);
            accountBalance.merge(dividend, true);

            applicationContext.logDebugMessage("dividend: " + Long.toUnsignedString(userAccount.id) + ": " + dividend.toJSONObject().toJSONString());
        }

        return amount;
    }

    private int getWithdrawIndex() {

        if (economicCluster == null || !economicCluster.isEqual(applicationContext.state.economicCluster)) {
            economicCluster = applicationContext.state.economicCluster.clone();
            withdrawIndex = 0;
        }

        return withdrawIndex++;
    }
}
