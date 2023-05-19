package concept.omno;

import concept.omno.object.PlatformToken;
import concept.platform.ArdorApi;
import concept.platform.EconomicCluster;
import concept.utility.JsonFunction;
import concept.utility.NxtCryptography;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OmnoApi extends ArdorApi implements Runnable{

    final JSONArray globalOperationArray = new JSONArray();
    final long contractAccountId;
    final String contractName;
    final NxtCryptography nxtCryptography;
    short deadline = Short.MAX_VALUE;

    EconomicCluster economicCluster;
    private boolean stop = false;
    private boolean stopComplete = true;

    public void stop() {
        broadcast();
        stop = true;
    }

    @Override
    public void run() {

        System.out.println("omnoApi started");

        stopComplete = false;

        while(!stop) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException ignored) { }

            synchronized (this) {

                int generationTimeRemaining = getBlockGenerationTimeRemaining();

                if (generationTimeRemaining > 11) {
                    continue;
                }

                processBlock();
            }
        }

        stopComplete = true;
    }

    public boolean waitStopComplete() {
        System.out.println("omnoApi wait stop");

        if (stop) {

            while (!stopComplete) {
                try {
                    TimeUnit.MILLISECONDS.sleep(50);
                } catch (InterruptedException ignored) {}
            }
        }

        return stop;
    }

    public OmnoApi(String hostProtocolString, String hostNameString, String hostPortString, long contractAccountId, String contractName, byte[] privateKey) {
        super(hostProtocolString, hostNameString, hostPortString);
        this.contractAccountId = contractAccountId;
        this.contractName = contractName;
        this.nxtCryptography = new NxtCryptography(privateKey);

        economicCluster = new EconomicCluster(this, 1);
        setDeadline((short) 1440);
    }

    public OmnoApi(ArdorApi ardorApi, long contractAccountId, String contractName, byte[] privateKey) {
        this(ardorApi.hostProtocolString, ardorApi.hostNameString, ardorApi.hostPortString, contractAccountId, contractName, privateKey);
    }

    public int getPendingTransactionCount() {
        return globalOperationArray.size();
    }

    public void setEconomicCluster(EconomicCluster economicCluster) {
        this.economicCluster = economicCluster.clone();
    }

    public void setDeadline(short deadline) {
        this.deadline = deadline;
    }

    private JSONObject getHeader() {
        JSONObject jsonObject = new JSONObject();
        JsonFunction.put(jsonObject, "contract", contractName);
        return jsonObject;
    }

    public boolean deposit(PlatformToken platformToken) {

        if (platformToken == null || platformToken.isZero() || ! platformToken.isValid()) {
            return false;
        }

        platformToken.removeZeroBalanceAll();

        HashMap<Long, Long> mapChainToken = platformToken.getChainTokenMap();
        JSONObject headerJson = getHeader();

        if (mapChainToken != null && mapChainToken.size() > 0) {

            for (long key: mapChainToken.keySet()) {
                long value = mapChainToken.get(key);
                long feeNQT = getChainToken((int)key).ONE_COIN / 5;

                if (key == 1) {
                    feeNQT *= 10;
                }

                JSONObject response = sendMoney(economicCluster.getHeight(), economicCluster.getBlockId(), 0, deadline, null, nxtCryptography.getPublicKey(), contractAccountId, (int) key, feeNQT, headerJson.toJSONString(), true, value);
                transactionBroadcast(response, nxtCryptography.getPrivateKey());
            }
        }

        HashMap<Long, Long> mapAssetToken = platformToken.getAssetTokenMap();
        long feeNQT = getChainToken(2).ONE_COIN / 5;

        if (mapAssetToken != null && mapAssetToken.size() > 0) {

            for (long key: mapAssetToken.keySet()) {
                long value = mapAssetToken.get(key);
                JSONObject response = transferAsset(economicCluster.getHeight(), economicCluster.getBlockId(), 0, deadline, null, nxtCryptography.getPublicKey(), contractAccountId, 2, feeNQT, headerJson.toJSONString(), true, key, value);
                transactionBroadcast(response, nxtCryptography.getPrivateKey());
            }
        }

        return true;
    }

    public void broadcast() {

        if (globalOperationArray.size() == 0) {
            return;
        }

        System.out.println("omnoApi broadcast");

        JSONObject headerJson = getHeader();

        long feeNQT = getChainToken(2).ONE_COIN / 5;

        synchronized (this) {
            JsonFunction.put(headerJson, "operation", globalOperationArray);
            JSONObject response = sendMessage(economicCluster.getHeight(), economicCluster.getBlockId(), 0, deadline, null, nxtCryptography.getPublicKey(), contractAccountId, 2, feeNQT, headerJson.toJSONString(), true);
            transactionBroadcast(response, nxtCryptography.getPrivateKey());
            globalOperationArray.clear();
        }
    }

    private void processBlock() {
        broadcast();
    }

    private void addOperation(JSONObject jsonObject) {

        if (jsonObject == null) {
            return;
        }

        synchronized (this) {
            JsonFunction.add(globalOperationArray, jsonObject);
        }
    }

    public boolean withdraw(PlatformToken platformToken) {

        if (platformToken == null || platformToken.isZero() || ! platformToken.isValid()) {
            return false;
        }

        platformToken.removeZeroBalanceAll();

        JSONObject operation = new JSONObject();

        JsonFunction.put(operation, "service", "user");
        JsonFunction.put(operation, "request", "withdraw");

        JSONObject parameter = new JSONObject();
        JsonFunction.put(parameter, "value", platformToken.toJSONObject());

        JsonFunction.put(operation, "parameter", parameter);

        addOperation(operation);

        return true;
    }

    public boolean withdrawAll() {

        JSONObject operation = new JSONObject();

        JsonFunction.put(operation, "service", "user");
        JsonFunction.put(operation, "request", "withdrawAll");

        JsonFunction.add(globalOperationArray, operation);

        return true;
    }

    public boolean platformTokenExchangeByIdOperationOffer(PlatformToken give, PlatformToken take, long multiplier) {

        if (give == null || !give.isValid() ||give.isZero() || take == null || !take.isValid() || take.isZero()) {
            return false;
        }

        JSONObject operation = new JSONObject();

        JsonFunction.put(operation, "service", "trade");
        JsonFunction.put(operation, "request", "offer");

        JSONObject parameter = new JSONObject();
        JsonFunction.put(parameter, "give", give.toJSONObject());
        JsonFunction.put(parameter, "take", take.toJSONObject());
        JsonFunction.put(parameter, "multiplier", Long.toUnsignedString(multiplier));

        JsonFunction.put(operation, "parameter", parameter);

        addOperation(operation);

        return true;
    }

    public boolean platformTokenExchangeByIdOperationCancel(long offerId) {

        if (offerId == 0) {
            return false;
        }

        JSONObject operation = new JSONObject();

        JsonFunction.put(operation, "service", "trade");
        JsonFunction.put(operation, "request", "cancel");

        JSONObject parameter = new JSONObject();
        JsonFunction.put(parameter, "id", Long.toUnsignedString(offerId));

        JsonFunction.put(operation, "parameter", parameter);

        addOperation(operation);

        return true;
    }

    public boolean platformTokenExchangeByIdOperationAccept(long offerId, long multiplier) {

        if (offerId == 0 || multiplier <= 0) {
            return false;
        }

        JSONObject operation = new JSONObject();

        JsonFunction.put(operation, "service", "trade");
        JsonFunction.put(operation, "request", "accept");

        JSONObject parameter = new JSONObject();
        JsonFunction.put(parameter, "id", Long.toUnsignedString(offerId));
        JsonFunction.put(parameter, "multiplier", Long.toUnsignedString(multiplier));

        JsonFunction.put(operation, "parameter", parameter);

        addOperation(operation);

        return true;
    }

    // R Game
    public boolean formArmy(int arenaId, List<Long> listAssetTokenIds) {

        if (listAssetTokenIds == null || listAssetTokenIds.size() == 0) {
            return false;
        }

        JSONObject operation = new JSONObject();

        JsonFunction.put(operation, "service", "rgame");
        JsonFunction.put(operation, "request", "formArmy");

        JSONObject parameter = new JSONObject();
        JsonFunction.put(parameter, "arena", arenaId);
        JsonFunction.put(parameter, "asset", JsonFunction.jsonArrayStringsFromListLongUnsigned(listAssetTokenIds));

        JsonFunction.put(operation, "parameter", parameter);

        addOperation(operation);

        return true;
    }
}
