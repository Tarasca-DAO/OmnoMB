package org.tarasca.mythicalbeings.cardmorph.omno.service;

import concept.omno.ApplicationContext;
import concept.omno.object.Operation;
import concept.omno.object.PlatformToken;
import concept.platform.EconomicCluster;
import concept.utility.JsonFunction;
import org.json.simple.JSONObject;
import org.tarasca.mythicalbeings.cardmorph.omno.service.object.Peer;

import java.util.*;

public class State {
    private ApplicationContext applicationContext;
    private Definition definition = new Definition();
    private boolean removeDuplicate = false;
    private long incomeAccount;
    private PlatformToken operationFee = new PlatformToken();
    private boolean contractPaysWithdrawFee = false;

    private HashMap<Long, Integer> rankCache = new HashMap<>();

    private EconomicCluster economicCluster = null;
    private long seedCount = 0;

    private PlatformToken tokenTotalIn = new PlatformToken();
    private PlatformToken tokenTotalOut = new PlatformToken();

    public boolean isValid() {
        return true;
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();

        JsonFunction.put(jsonObject, "definition", definition.toJSONObject());

        JsonFunction.put(jsonObject, "incomeAccount", Long.toUnsignedString(incomeAccount));
        JsonFunction.put(jsonObject, "operationFee", operationFee.toJSONObject());
        JsonFunction.put(jsonObject, "contractPaysWithdrawFee", contractPaysWithdrawFee);

        JsonFunction.put(jsonObject, "tokenTotalIn", tokenTotalIn.toJSONObject());
        JsonFunction.put(jsonObject, "tokenTotalOut", tokenTotalOut.toJSONObject());

        return jsonObject;
    }

    public void define(JSONObject jsonObject) {

        if (jsonObject == null) {
            return;
        }

        definition = new Definition(JsonFunction.getJSONObject(jsonObject, "definition", null));
        incomeAccount = JsonFunction.getLongFromStringUnsigned(jsonObject, "incomeAccount",
                applicationContext.contractAccountId);
        operationFee = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "operationFee", null));
        contractPaysWithdrawFee = JsonFunction.getBoolean(jsonObject, "contractPaysWithdrawFee",
                contractPaysWithdrawFee);

        tokenTotalIn = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "tokenTotalIn", null));
        tokenTotalOut = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "tokenTotalOut", null));

        rankCache.clear();
    }

    public State(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public State(ApplicationContext applicationContext, JSONObject jsonObject) {
        this(applicationContext);
        define(jsonObject);
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
                result = operationDefinition(operation);
                break;
            }

            case "morph": {
                result = operationMorph(operation);
                break;
            }
        }

        return result;
    }

    private boolean operationDefinition(Operation operation) {

        if (operation == null || operation.parameterJson == null) {
            return false;
        }

        if (operation.account != applicationContext.contractAccountId) {
            return false;
        }

        define(operation.parameterJson);

        return true;
    }

    private boolean operationMorph(Operation operation) {

        if (operation == null || operation.parameterJson == null) {
            return false;
        }

        if (operation.account == applicationContext.contractAccountId) {
            return false;
        }

        JSONObject parameterJson = operation.parameterJson;

        long count = JsonFunction.getLongFromStringUnsigned(parameterJson, "count", 1);

        if (count <= 0) {
            return false;
        }

        long assetId = JsonFunction.getLongFromStringUnsigned(parameterJson, "asset", 0);

        if (assetId == 0) {
            return false;
        }

        int rank = morphGetAssetRank(assetId);

        if (rank == 0) {
            return false;
        }

        if (definition.peers == null || definition.peers.isEmpty() || !definition.peers.containsKey(rank)) {
            return false;
        }

        Peer peer = definition.peers.get(rank);

        if (peer == null) {
            return false;
        }

        if (peer.peer.getAssetTokenValue(assetId) == 0) {
            return false;
        }

        PlatformToken cost = peer.cost.clone();

        PlatformToken platformTokenAsset = new PlatformToken();
        platformTokenAsset.mergeAssetToken(assetId, 1, true);

        PlatformToken costTotal = new PlatformToken();
        costTotal.merge(platformTokenAsset, true);
        costTotal.merge(cost, true);

        if (count > 1) {
            costTotal.multiply(count);
            cost.multiply(count);
        }

        if (!costTotal.isValid() || !cost.isValid()) {
            return false;
        }

        if (!applicationContext.state.userAccountState.hasRequiredBalance(operation.account, costTotal)) {
            return false;
        }

        PlatformToken platformTokenAssetTotal = platformTokenAsset.clone();
        platformTokenAssetTotal.multiply(count);

        if (!platformTokenAssetTotal.isValid()) {
            return false;
        }

        PlatformToken platformTokenAssetPickTotal = new PlatformToken();

        long seed = getSeed();
        Random random = applicationContext.state.getCombinedRandom(seed, null);

        for (int i = 0; i < count; i++) {

            List<Long> listAssetId = new ArrayList<>(peer.peer.getAssetTokenMap().keySet());

            Collections.sort(listAssetId);

            if (removeDuplicate) {
                listAssetId.removeIf(n -> n == assetId);
            }

            int index = (int) (random.nextDouble() * listAssetId.size());
            long assetIdPick = listAssetId.get(index);
            long assetIdPickCount = 1;

            boolean changed = assetId != assetIdPick;

            double rollForDuplicate = 1.0;

            if (!changed) {
                rollForDuplicate = random.nextDouble();

                if (rollForDuplicate < peer.chanceDuplicateIfNotMorphed) {
                    assetIdPickCount *= 2;
                }
            }

            platformTokenAssetPickTotal.mergeAssetToken(assetIdPick, assetIdPickCount, true);

            {
                int log_limit = 200;

                if (count <= log_limit) {

                    applicationContext.logInfoMessage("Morph: " + Long.toUnsignedString(operation.account) + ": seed: "
                            + seed + ": rank " + rank + ": index: " + index + ": size: " + listAssetId.size() + ": "
                            + Long.toUnsignedString(assetId) + " -> " + Long.toUnsignedString(assetIdPickCount) + " * "
                            + Long.toUnsignedString(assetIdPick) + ": changed: " + changed + " (" + rollForDuplicate
                            + " < " + peer.chanceDuplicateIfNotMorphed + ")");

                    if (count == log_limit) {
                        applicationContext.logInfoMessage(Long.toUnsignedString(operation.account)
                                + ": information: last log entry for this operation because too many: " + count);
                    }
                }
            }
        }

        if (!platformTokenAssetPickTotal.isValid()) {
            return false;
        }

        applicationContext.state.userAccountState.subtractFromBalance(operation.account, platformTokenAssetTotal);
        applicationContext.state.userAccountState.addToBalance(applicationContext.contractAccountId,
                platformTokenAssetTotal);

        applicationContext.state.userAccountState.subtractFromBalance(operation.account, cost);
        applicationContext.state.userAccountState.addToBalance(incomeAccount, cost);

        applicationContext.state.userAccountState.subtractFromBalance(applicationContext.contractAccountId,
                platformTokenAssetPickTotal); // fail ignored
        applicationContext.state.userAccountState.addToBalance(operation.account, platformTokenAssetPickTotal);

        // Statistics

        // tokenTotalIn.merge(platformTokenAssetTotal, true);
        tokenTotalIn.merge(costTotal, true);
        tokenTotalOut.merge(platformTokenAssetPickTotal, true);
        applicationContext.logInfoMessage("tokenTotalIn : " + tokenTotalIn.toJSONObject().toJSONString());
        applicationContext.logInfoMessage("tokenTotalOut : " + tokenTotalOut.toJSONObject().toJSONString());

        //

        boolean withdraw = JsonFunction.getBoolean(parameterJson, "withdraw", false);

        if (withdraw) {
            applicationContext.logInfoMessage("Withdraw | Account: " + Long.toUnsignedString(operation.account) + " | PlatformToken: " + platformTokenAssetPickTotal.toJSONObject().toJSONString());
            applicationContext.state.userAccountState.withdraw(operation.account, platformTokenAssetPickTotal,
                    operation.account, "cardmorph", null, null, contractPaysWithdrawFee);
        }

        return true;
    }

    private int morphGetAssetRank(Long assetId) {

        if (assetId == 0) {
            return 0;
        }

        if (rankCache != null && rankCache.isEmpty() && rankCache.containsKey(assetId)) {
            return rankCache.get(assetId);
        }

        if (definition.peers == null || definition.peers.isEmpty()) {
            return 0;
        }

        for (int rank : definition.peers.keySet()) {
            Peer peer = definition.peers.get(rank);

            if (peer.peer.getAssetTokenValue(assetId) != 0) {

                if (rankCache == null) {
                    rankCache = new HashMap<>();
                }

                rankCache.put(assetId, rank);

                return rank;
            }
        }

        return 0;
    }

    private long getSeed() {

        if (economicCluster == null || !economicCluster.isEqual(applicationContext.state.economicCluster)) {
            economicCluster = applicationContext.state.economicCluster.clone();
            seedCount = 0;
        }

        seedCount++;

        return seedCount;
    }
}
