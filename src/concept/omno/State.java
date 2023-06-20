package concept.omno;

import concept.omno.object.Operation;
import concept.omno.object.PlatformToken;
import concept.omno.object.UserAccount;
import concept.omno.service.*;
import concept.omno.service.voting.Voting;
import concept.platform.ArdorApi;
import concept.platform.EconomicCluster;
import concept.platform.Transaction;
import concept.utility.FileUtility;
import concept.utility.JsonFunction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

public class State {
    final static int saveRotateMaxIndex = 20;
    final static int saveHeightCheckpointInterval = 360;
    int versionFromDefinition = 0;

    final private ApplicationContext applicationContext;
    boolean useLowSecurityRandom = true;

    int[] supportedDepositChainTokens = { 1, 2, 4, 6 };

    final int versionMinimum = 0;
    final int version = 0;

    public EconomicCluster economicCluster;

    public short transactionDeadlineMinimum = 1;
    public int ecBlockHeightDeltaMinimum = 1;

    long incomeAccount = 0;
    PlatformToken operationFee = new PlatformToken();

    StateCache stateCache;

    HashMap<Long, Boolean> failAccountMap = new HashMap<>();
    HashMap<Long, Boolean> skipAccountMap = new HashMap<>();

    public NativeAssetState nativeAssetState;
    public UserAccountState userAccountState;
    public concept.omno.service.PlatformTokenExchangeById.State platformTokenExchangeById;
    public PlatformSwap platformSwap;
    public Voting voting;
    public CollateralizedSwap collateralizedSwap;

    public org.tarasca.mythicalbeings.cardmorph.omno.service.State cardmorph;

    public org.tarasca.mythicalbeings.rgame.omno.service.State rgame;

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();

        if (stateCache != null && stateCache.economicCluster.isEqual(economicCluster)) {
            return stateCache.state;
        }

        JsonFunction.put(jsonObject, "version", version);

        JsonFunction.put(jsonObject, "economicCluster", economicCluster.toJSONObject());

        JsonFunction.put(jsonObject, "transactionDeadlineMinimum", (int) transactionDeadlineMinimum);
        JsonFunction.put(jsonObject, "ecBlockHeightDeltaMinimum", ecBlockHeightDeltaMinimum);

        JsonFunction.put(jsonObject, "incomeAccount", Long.toUnsignedString(incomeAccount));
        JsonFunction.put(jsonObject, "operationFee", operationFee.toJSONObject());

        JsonFunction.put(jsonObject, "nativeAsset", nativeAssetState.toJSONObject());
        JsonFunction.put(jsonObject, "userAccount", userAccountState.toJSONObject());
        JsonFunction.put(jsonObject, "exchange", platformTokenExchangeById.toJSONObject());
        JsonFunction.put(jsonObject, "platformSwap", platformSwap.state.toJSONObject());
        JsonFunction.put(jsonObject, "voting", voting.state.toJSONObject());
        JsonFunction.put(jsonObject, "collateralizedSwap", collateralizedSwap.state.toJSONObject());

        JsonFunction.put(jsonObject, "rgame", rgame.toJSONObject());
        JsonFunction.put(jsonObject, "cardmorph", cardmorph.toJSONObject());

        stateCache = new StateCache(jsonObject, economicCluster);

        return jsonObject;
    }

    public void define(JSONObject jsonObject) {
        if (jsonObject == null) {
            return;
        }

        versionFromDefinition = JsonFunction.getInt(jsonObject, "version", 0);

        if (versionFromDefinition > version) {
            applicationContext.logErrorMessage("ERROR: definition (" + versionFromDefinition
                    + ") is from a future version. This application (" + version + ") may be out-of-date.");
        }

        if (versionFromDefinition < versionMinimum) {
            applicationContext.logErrorMessage("ERROR: definition (" + versionFromDefinition
                    + ") is from an unsupported older version. This application's minimum (" + versionMinimum + ")");
        }

        transactionDeadlineMinimum = (short) JsonFunction.getInt(jsonObject, "transactionDeadlineMinimum", 1);
        ecBlockHeightDeltaMinimum = JsonFunction.getInt(jsonObject, "ecBlockHeightDeltaMinimum", 1);

        incomeAccount = JsonFunction.getLongFromStringUnsigned(jsonObject, "incomeAccount",
                applicationContext.contractAccountId);
        operationFee = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "operationFee", null));

        economicCluster = new EconomicCluster(JsonFunction.getJSONObject(jsonObject, "economicCluster", null));

        nativeAssetState = new NativeAssetState(applicationContext,
                JsonFunction.getJSONObject(jsonObject, "nativeAsset", null), false);

        userAccountState = new UserAccountState(applicationContext,
                JsonFunction.getJSONObject(jsonObject, "userAccount", null));
        platformTokenExchangeById = new concept.omno.service.PlatformTokenExchangeById.State(applicationContext,
                JsonFunction.getJSONObject(jsonObject, "exchange", null), true);

        platformSwap = new PlatformSwap(applicationContext,
                JsonFunction.getJSONObject(jsonObject, "platformSwap", null));

        voting = new Voting(applicationContext, JsonFunction.getJSONObject(jsonObject, "voting", null));

        collateralizedSwap = new CollateralizedSwap(applicationContext,
                JsonFunction.getJSONObject(jsonObject, "collateralizedSwap", null));

        rgame = new org.tarasca.mythicalbeings.rgame.omno.service.State(applicationContext,
                JsonFunction.getJSONObject(jsonObject, "rgame", null));
        cardmorph = new org.tarasca.mythicalbeings.cardmorph.omno.service.State(applicationContext,
                JsonFunction.getJSONObject(jsonObject, "cardmorph", null));
    }

    State(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        economicCluster = new EconomicCluster(0, 0, 0);
        userAccountState = new UserAccountState(applicationContext);
        nativeAssetState = new NativeAssetState(applicationContext);
        platformTokenExchangeById = new concept.omno.service.PlatformTokenExchangeById.State(applicationContext);
        platformSwap = new PlatformSwap(applicationContext);
        voting = new Voting(applicationContext);
        collateralizedSwap = new CollateralizedSwap(applicationContext);

        rgame = new org.tarasca.mythicalbeings.rgame.omno.service.State(applicationContext);
        cardmorph = new org.tarasca.mythicalbeings.cardmorph.omno.service.State(applicationContext);
    }

    State(ApplicationContext applicationContext, JSONObject jsonObject) {
        this(applicationContext);
        define(jsonObject);
    }

    State(ApplicationContext applicationContext, EconomicCluster economicCluster) {
        this(applicationContext);
        this.economicCluster = economicCluster.clone();
    }

    public static class StateCache {
        EconomicCluster economicCluster;
        JSONObject state;

        StateCache(JSONObject jsonObject, EconomicCluster economicCluster) {

            if (economicCluster != null) {
                this.economicCluster = economicCluster.clone();
            }

            this.state = jsonObject;
        }

        public boolean isValid(ArdorApi ardorApi) {
            return (economicCluster.isValid(ardorApi) && state != null);
        }
    }

    public PlatformToken getUserAccountBalance(long accountId) {

        UserAccount userAccount = userAccountState.getUserAccount(accountId);

        if (userAccount == null) {
            return null;
        }

        PlatformToken result = userAccount.balance;

        // if (result == null) {
        // return null;
        // }

        return result;
    }

    public boolean transferToBalanceIncome(PlatformToken platformToken, long account) {
        if (account == incomeAccount) {
            return true;
        }

        if (!applicationContext.state.userAccountState.subtractFromBalance(account, platformToken)) {
            return false;
        }

        applicationContext.state.userAccountState.addToBalance(incomeAccount, platformToken);

        return true;
    }

    public void addToBalanceIncome(PlatformToken platformToken) {
        applicationContext.state.userAccountState.addToBalance(incomeAccount, platformToken);
        applicationContext.logInfoMessage("Income: " + platformToken.toJSONObject().toJSONString());
    }

    private void applyHeightDependentChanges() {

    }

    public void configure(JSONObject jsonObject) {
        if (jsonObject == null) {
            return;
        }

        incomeAccount = JsonFunction.getLongFromStringUnsigned(jsonObject, "incomeAccount",
                applicationContext.contractAccountId);

        JSONObject feeObject = JsonFunction.getJSONObject(jsonObject, "operationFee", null);

        if (feeObject != null) {
            PlatformToken newFee = new PlatformToken(feeObject);

            if (newFee.isValid()) {
                operationFee = newFee;
            }
        }

        transactionDeadlineMinimum = (short) JsonFunction.getInt(jsonObject, "transactionDeadlineMinimum",
                transactionDeadlineMinimum);
        ecBlockHeightDeltaMinimum = JsonFunction.getInt(jsonObject, "ecBlockHeightDeltaMinimum",
                ecBlockHeightDeltaMinimum);
    }

    private boolean operationConfigure(Operation operation) {

        if (operation == null || operation.parameterJson == null) {
            return false;
        }

        if (operation.account != applicationContext.contractAccountId) {
            return false;
        }

        int version = JsonFunction.getInt(operation.parameterJson, "version", -1);

        if (version > 0) {
            if (version > this.version || version < this.versionMinimum) {
                applicationContext.logInfoMessage(
                        "New state version (" + version + ") is unsupported by this application ("
                                + this.versionMinimum + " up-to including " + this.version);
                throw new RuntimeException();
            }
        }

        configure(operation.parameterJson);

        return true;
    }

    private boolean operationSkipIfFail(Operation operation) {

        if (operation == null || operation.parameterJson == null) {
            return false;
        }

        if (isFailOperationSet(operation.account)) {
            skipOperationSet(operation.account);
        }

        return true;
    }

    private boolean operationFailClear(Operation operation) {

        if (operation == null || operation.parameterJson == null) {
            return false;
        }

        failOperationClear(operation.account);

        return true;
    }

    private boolean operationSkipClear(Operation operation) {

        if (operation == null || operation.parameterJson == null) {
            return false;
        }

        skipOperationClear(operation.account);

        return true;
    }

    public boolean isSkipOperation(Operation operation) {

        if (isSkipOperationSet(operation.account)) {
            applicationContext.logDebugMessage("operation skipped : skip set");
            return true;
        }

        JSONObject jsonObject = operation.parameterJson;

        if (JsonFunction.getBoolean(jsonObject, "requireFailSet", false)) {
            if (!isFailOperationSet(operation.account)) {
                applicationContext.logDebugMessage("operation skipped : requireFailSet");
                return true;
            }
        }

        if (JsonFunction.getBoolean(jsonObject, "requireFailClear", false)) {
            if (isFailOperationSet(operation.account)) {
                applicationContext.logDebugMessage("operation skipped : requireFailClear");
                return true;
            }
        }

        return false;
    }

    public boolean isFailOperationSet(long account) {
        if (!failAccountMap.containsKey(account))
            return false;

        return failAccountMap.get(account);
    }

    public boolean failOperationSet(long account) {
        failAccountMap.put(account, true);
        return true;
    }

    public boolean failOperationClear(long account) {
        failAccountMap.put(account, false);
        return true;
    }

    public boolean isSkipOperationSet(long account) {
        if (!skipAccountMap.containsKey(account))
            return false;

        return skipAccountMap.get(account);
    }

    public boolean skipOperationSet(long account) {
        skipAccountMap.put(account, true);
        return true;
    }

    public boolean skipOperationClear(long account) {
        skipAccountMap.put(account, false);
        return true;
    }

    public boolean processOperation(Operation operation) {
        boolean result = false;

        if (operation == null) {
            return false;
        }

        switch (operation.request) {
            default: {
                break;
            }

            case "configure": {
                result = operationConfigure(operation);
                break;
            }

            case "skipIfFail": {
                result = operationSkipIfFail(operation);
                break;
            }

            case "skipClear": {
                result = operationSkipClear(operation);
                break;
            }

            case "failClear": {
                result = operationFailClear(operation);
                break;
            }
        }

        return result;
    }

    public boolean processServiceOperation(Operation operation) {
        boolean result = false;

        if (operation.account != applicationContext.contractAccountId) {
            if (!applicationContext.state.userAccountState.subtractFromBalance(operation.account, operationFee)) {
                return false;
            }

            applicationContext.state.userAccountState.addToBalance(incomeAccount, operationFee);
        }

        applicationContext.logDebugMessage("Service: " + operation.service + " | Request: " + operation.request);
        if (operation.parameterJson != null) {
            applicationContext.logDebugMessage("JSON: " + operation.parameterJson.toJSONString());
        }

        switch (operation.service) {
            default: {
                break;
            }

            case "platform": {
                result = processOperation(operation);
                break;
            }

            case "user": {
                result = userAccountState.processOperation(operation);
                break;
            }

            case "nativeAsset": {
                result = nativeAssetState.processOperation(operation);

                break;
            }

            case "trade": {
                result = platformTokenExchangeById.processOperation(operation);

                break;
            }

            case "platformSwap": {
                result = platformSwap.state.processOperation(operation);
                break;
            }

            case "voting": {
                result = voting.state.processOperation(operation);
                break;
            }

            case "collateralizedSwap": {
                result = collateralizedSwap.state.processOperation(operation);
                break;
            }

            case "cardmorph": {
                result = cardmorph.processOperation(operation);
                break;
            }

            case "rgame": {
                result = rgame.processOperation(operation);
                break;
            }
        }

        return result;
    }

    public void nextBlock() {
        EconomicCluster economicClusterNext = new EconomicCluster(applicationContext.ardorApi,
                economicCluster.getHeight() + 1);

        if (!economicClusterNext.isValid(applicationContext.ardorApi)) {
            applicationContext.logInfoMessage("nextBlock not valid: " + economicClusterNext.getHeight() + ": "
                    + economicCluster.getHeight());
            return;
        }

        failAccountMap = new HashMap<>();
        skipAccountMap = new HashMap<>();

        economicCluster = economicClusterNext;
        applyHeightDependentChanges();

        List<Transaction> listTransactionAll = sortTransactionList(
                getBlockExecutedTransactionsReceived(economicClusterNext.getHeight()));
        List<Transaction> listTransactionControl = getTransactionsControl(listTransactionAll);
        List<Transaction> listTransactionUser = getTransactionsUser(listTransactionAll);

        Operations operations = new Operations(applicationContext.contractName);

        for (Transaction transaction : listTransactionControl) {
            operations.addOperations(transaction);
        }

        for (Transaction transaction : listTransactionUser) {
            operations.addOperations(transaction);
        }

        applyIncomingTransactionEffects(listTransactionUser);

        List<Operation> listOperation = operations.getOperationList();

        for (Operation operation : listOperation) {
            processServiceOperation(operation);
        }

        listOperation = serviceProcessBlock();

        if (listOperation.size() > 0) {
            for (Operation operation : listOperation) {
                processServiceOperation(operation);
            }
        }

    }

    private List<Operation> serviceProcessBlock() {
        List<Operation> operationList = new ArrayList<>();

        platformSwap.state.processBlock();
        voting.state.processBlock(operationList);

        rgame.processBlock();

        return operationList;
    }

    public boolean isValid() {
        return (economicCluster != null && economicCluster.isValid(applicationContext.ardorApi)
                && versionFromDefinition <= version && versionFromDefinition >= versionMinimum);
    }

    public static boolean createFileHash(String pathString) {

        byte[] hash = FileUtility.getHash(pathString, "SHA-256");

        if (hash == null || hash.length == 0) {
            return false;
        }

        return FileUtility.writeFile(pathString + ".hash", hash);
    }

    public static boolean verifyFileHash(String pathString) {
        return FileUtility.verifyHash(pathString, FileUtility.readFile(pathString + ".hash"), "SHA-256");
    }

    public static Path getStatePathRotationCreate(Path rootPath) {
        Path result = Paths.get(rootPath.toString() + "/rotate/");

        try {
            Files.createDirectories(result);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static Path getStateFullPathRotation(String name, Path rootPath, String stringId, int index) {
        return Paths.get(getStatePathRotationCreate(rootPath) + "/" + name + "." + stringId + "." + index + ".state");
    }

    public static Path getStateFullPathAtHeight(String name, Path rootPath, EconomicCluster economicCluster) {
        return Paths.get(rootPath.toString() + "/block/" + name + "." + economicCluster.getHeight() + "."
                + Long.toUnsignedString(economicCluster.getBlockId()) + ".state");
    }

    public static Path getStateFullPathAtHeight(String name, Path rootPath, int height, ArdorApi ardorApi) {
        EconomicCluster economicCluster = new EconomicCluster(ardorApi, height);
        return getStateFullPathAtHeight(name, rootPath, economicCluster);
    }

    public boolean saveState(String fullPath) {
        FileOutputStream fileOutputStream;

        try {
            JSONObject jsonObject = new JSONObject();
            fileOutputStream = new FileOutputStream(fullPath, false);
            ObjectOutputStream objectOutputStream;
            objectOutputStream = new ObjectOutputStream(fileOutputStream);

            applicationContext.signJSONObject(jsonObject, "state", applicationContext.state.toJSONObject());
            objectOutputStream.writeObject(jsonObject);

            objectOutputStream.close();
            fileOutputStream.close();
        } catch (Exception e) {
            applicationContext.logErrorMessage("error: saveState: " + e);
            return false;
        }

        createFileHash(fullPath);

        return true;
    }

    private void saveRotate(String name, Path rootPath, String stringId) {

        for (int i = saveRotateMaxIndex; i > 0; i--) {
            File fileNew = new File(getStateFullPathRotation(name, rootPath, stringId, i - 1).toString());
            File fileOld = new File(getStateFullPathRotation(name, rootPath, stringId, i).toString());

            fileOld.delete();
            fileNew.renameTo(fileOld);

            fileNew = new File(getStateFullPathRotation(name, rootPath, stringId, i - 1) + ".hash");
            fileOld = new File(getStateFullPathRotation(name, rootPath, stringId, i) + ".hash");

            fileOld.delete();
            fileNew.renameTo(fileOld);
        }
    }

    public boolean saveState(String name, Path rootPath) {

        int height = economicCluster.getHeight();

        Path path;

        if (height % saveHeightCheckpointInterval == 0) {
            saveRotate(name, rootPath, "far");
            path = getStateFullPathRotation(name, rootPath, "far", 0);
        } else {
            saveRotate(name, rootPath, "near");
            path = getStateFullPathRotation(name, rootPath, "near", 0);
        }

        saveState(path.toString());

        return true;
    }

    public static State loadState(ApplicationContext applicationContext, String fullPath) {

        if (applicationContext == null || fullPath == null) {
            return null;
        }

        if (!Files.exists(Paths.get(fullPath))) {
            return null;
        }

        if (!verifyFileHash(fullPath)) {
            applicationContext.logInfoMessage("WARNING: state hash verification fail for: " + fullPath);
            return null;
        }

        State result = null;

        try {

            FileInputStream fileInputStream = new FileInputStream(fullPath);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

            JSONObject jsonObject = (JSONObject) objectInputStream.readObject();
            JSONObject jsonObjectState = JsonFunction.getJSONObject(jsonObject, "state", null);

            State state = new State(applicationContext, jsonObjectState);

            if (state.isValid() && state.economicCluster.getHeight() >= applicationContext.heightStart) {
                result = state;
            }

        } catch (Exception e) {
            applicationContext.logErrorMessage(e.toString());
        }

        return result;
    }

    public static State loadLastValidState(ApplicationContext applicationContext, String name, Path rootPath) {
        State state = null;

        int index = 0;

        while (state == null && index <= saveRotateMaxIndex) {
            Path path = getStateFullPathRotation(name, rootPath, "near", index++);
            state = loadState(applicationContext, path.toString());
        }

        index = 0;

        while (state == null && index <= saveRotateMaxIndex) {
            Path path = getStateFullPathRotation(name, rootPath, "far", index++);
            state = loadState(applicationContext, path.toString());
        }

        if (applicationContext.reloadAndRescan) {
            return null;
        }

        return state;
    }

    public Random getCombinedRandom(long value, EconomicCluster economicClusterOverride) {
        Random random;

        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            applicationContext.logErrorMessage(e.toString());
            throw new IllegalArgumentException(e);
        }

        MessageDigest digest;

        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            applicationContext.logErrorMessage(e.toString());
            throw new IllegalArgumentException(e);
        }

        // NOTE this can be mixed with a secret to prevent the block generator
        // manipulating the results
        // and with regular publishing of the block's seed to allow verification of the
        // results
        // With option "useLowSecurityRandom" the verifier can follow the results
        // in-sync and this is convenient for initial release.

        if (!useLowSecurityRandom) {
            if (applicationContext.secretForRandom == null) {
                applicationContext
                        .logErrorMessage("secretForRandom must be configured if useLowSecurityRandom is false");
                throw new NullPointerException();
            }

            digest.update(applicationContext.secretForRandom);
        }

        EconomicCluster economicClusterForSeed = economicCluster;

        if (economicClusterOverride != null) {
            economicClusterForSeed = economicClusterOverride;
        }

        digest.update(ByteBuffer.allocate(Long.BYTES).putLong(value).array());
        digest.update(ByteBuffer.allocate(Long.BYTES).putLong(economicClusterForSeed.getBlockId()).array());
        long seed = ByteBuffer.wrap(digest.digest(), 0, 8).getLong();
        random.setSeed(seed);

        return random;
    }

    /*
     * private List<Transaction> getBlockExecutedTransactionsSent(int height, int
     * chain) {
     * List<Transaction> listTransaction = new ArrayList<>();
     * 
     * JSONObject response;
     * 
     * try {
     * response =
     * applicationContext.ardorApi.getExecutedTransactions(applicationContext.
     * adminPasswordString, chain, height, 0, applicationContext.contractAccountId);
     * } catch (IOException e) {
     * applicationContext.logErrorMessage(e.toString());
     * return listTransaction;
     * }
     * 
     * JSONArray transactionsJSONArray = JsonFunction.getJSONArray(response,
     * "transactions", null);
     * 
     * if (transactionsJSONArray == null || transactionsJSONArray.size() == 0) {
     * return listTransaction;
     * }
     * 
     * for (Object o: transactionsJSONArray) {
     * listTransaction.add(new Transaction((JSONObject) o));
     * }
     * 
     * return listTransaction;
     * }
     */

    private List<Transaction> getBlockExecutedTransactionsReceived(int height, int chain) {
        List<Transaction> listTransaction = new ArrayList<>();

        JSONObject response;

        try {
            response = applicationContext.ardorApi.getExecutedTransactions(applicationContext.adminPasswordString,
                    chain, height, applicationContext.contractAccountId, 0);
        } catch (IOException e) {
            applicationContext.logErrorMessage(e.toString());
            return listTransaction;
        }

        JSONArray transactionsJSONArray = JsonFunction.getJSONArray(response, "transactions", null);

        if (transactionsJSONArray == null || transactionsJSONArray.size() == 0) {
            return listTransaction;
        }

        for (Object o : transactionsJSONArray) {
            JSONObject transactionObject = (JSONObject) o;

            Transaction transaction = new Transaction(transactionObject);

            {
                boolean isValidForPlatformSwap = true;

                long transactionDeadlineRemaining = (transaction.timestampDeadline - economicCluster.getTimestamp())
                        / 60;

                if (transactionDeadlineMinimum > transactionDeadlineRemaining) {
                    applicationContext.logDebugMessage("Transaction ignored: deadline too short: "
                            + transactionDeadlineRemaining + ": " + Long.toUnsignedString(transaction.sender));
                    isValidForPlatformSwap = false;
                }

                int ecBlockHeightDelta = economicCluster.getHeight() - transaction.ecBlockHeight;

                if (ecBlockHeightDeltaMinimum > ecBlockHeightDelta) {
                    applicationContext.logDebugMessage("Transaction ignored: ecBlockHeight too recent: "
                            + transaction.ecBlockHeight + ": " + Long.toUnsignedString(transaction.sender));
                    isValidForPlatformSwap = false;
                }

                if (!isValidForPlatformSwap) {
                    continue;
                }
            }

            listTransaction.add(transaction);
        }

        return listTransaction;
    }

    private List<Transaction> getBlockExecutedTransactionsReceived(int height) {
        List<Transaction> listTransaction = new ArrayList<>();

        if (supportedDepositChainTokens == null || supportedDepositChainTokens.length == 0) {
            return listTransaction;
        }

        for (int i = 0; i < supportedDepositChainTokens.length; i++) {
            listTransaction.addAll(getBlockExecutedTransactionsReceived(height, supportedDepositChainTokens[i]));
        }

        return listTransaction;
    }

    private List<Transaction> sortTransactionList(List<Transaction> listTransaction) {

        if (listTransaction == null || listTransaction.size() == 0) {
            return null;
        }

        SortedMap<String, Transaction> sortedMap = new TreeMap<>();

        try {

            MessageDigest digestBlockId;
            digestBlockId = MessageDigest.getInstance("SHA-256");
            digestBlockId.update(ByteBuffer.allocate(Long.BYTES).putLong(economicCluster.getBlockId()).array());

            for (Transaction transaction : listTransaction) {
                MessageDigest digestFullHash = (MessageDigest) digestBlockId.clone();
                sortedMap.put(JsonFunction.hexStringFromBytes(digestFullHash.digest(transaction.fullHash)),
                        transaction);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new ArrayList<>(sortedMap.values());
    }

    private List<Transaction> getTransactionsControl(List<Transaction> listTransaction) {
        List<Transaction> result = new ArrayList<>();

        if (listTransaction == null) {
            return result;
        }

        for (Transaction transaction : listTransaction) {
            if (transaction.recipient == transaction.sender
                    && transaction.sender == applicationContext.contractAccountId) {
                result.add(transaction);
            }
        }

        return result;
    }

    private List<Transaction> getTransactionsUser(List<Transaction> listTransaction) {
        List<Transaction> result = new ArrayList<>();

        if (listTransaction == null) {
            return result;
        }

        for (Transaction transaction : listTransaction) {
            if (transaction.recipient != transaction.sender
                    && transaction.recipient == applicationContext.contractAccountId) {
                result.add(transaction);
            }
        }

        return result;
    }

    private List<Transaction> applyIncomingTransactionEffects(List<Transaction> listTransaction) {
        List<Transaction> listTransactionFiltered = new ArrayList<>();

        if (listTransaction == null) {
            return listTransactionFiltered;
        }

        for (Transaction transaction : listTransaction) {
            if (transaction.sender == applicationContext.contractAccountId) {
                continue;
            }

            if (transaction.messageJson == null) {
                continue;
            }

            if (!transaction.messageIsPrunable) { // perhaps avoid state out-of-sync if node mismatched some not
                                                  // archival
                continue;
            }

            if (!transaction.messageJson.containsKey("contract")) {
                continue;
            }

            if (!transaction.messageJson.get("contract").equals(applicationContext.contractName)) {
                continue;
            }

            if ((transaction.type == 0 || transaction.type == -2) && transaction.subtype == 0) {
                PlatformToken platformToken = new PlatformToken();
                platformToken.mergeChainToken(transaction.chain, transaction.amountNQT, true);
                userAccountState.addToBalance(transaction.sender, platformToken);

                applicationContext
                        .logDebugMessage(applicationContext.state.economicCluster.toJSONObject().toJSONString());

                applicationContext.logDebugMessage("Deposit account: " + transaction.senderRS + " | "
                        + Long.toUnsignedString(transaction.sender));
                applicationContext.logDebugMessage("Chain: " + Long.toUnsignedString(transaction.chain)
                        + " | Amount: " + Long.toUnsignedString(transaction.amountNQT));
                continue;
            }

            if (transaction.type == 2 && transaction.subtype == 1) {
                PlatformToken platformToken = new PlatformToken();
                platformToken.mergeAssetToken(transaction.attachmentId, transaction.amountNQT, true);
                userAccountState.addToBalance(transaction.sender, platformToken);

                applicationContext
                        .logDebugMessage(applicationContext.state.economicCluster.toJSONObject().toJSONString());

                applicationContext.logDebugMessage("Deposit account: " + transaction.senderRS + " | "
                        + Long.toUnsignedString(transaction.sender));
                applicationContext.logDebugMessage("Asset: " + Long.toUnsignedString(transaction.attachmentId)
                        + " | Amount: " + Long.toUnsignedString(transaction.amountNQT));
                continue;
            }

            listTransactionFiltered.add(transaction);
        }

        return listTransactionFiltered;
    }

    public JSONObject apiProcessRequestPlatform(JSONObject jsonObject) {

        if (jsonObject == null) {
            return toJSONObject();
        }

        JSONObject result = new JSONObject();

        EconomicCluster economicCluster = new EconomicCluster(
                JsonFunction.getJSONObject(jsonObject, "economicCluster", null));

        if (this.economicCluster.isEqual(economicCluster)) {
            JsonFunction.put(result, "isEqual", true);
            return result;
        }

        return toJSONObject();
    }
}
