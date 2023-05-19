package org.tarasca.mythicalbeings.rgame.omno.service;

import concept.omno.ApplicationContext;
import concept.omno.object.Operation;
import concept.omno.object.PlatformToken;
import concept.platform.EconomicCluster;
import concept.utility.JsonFunction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.tarasca.mythicalbeings.rgame.omno.service.object.*;

import java.util.ArrayList;
import java.util.List;

public class State {
    ApplicationContext applicationContext;

    public Definition definition = new Definition();

    List<Operation> listOperationPending = new ArrayList<>();
    List<Operation> listOperationPendingDelayed = new ArrayList<>();

    public ArenaState arena;

    long incomeAccount;
    PlatformToken operationFee = new PlatformToken();

    public boolean isValid() {
        return true;
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();

        JsonFunction.put(jsonObject, "definition", definition.toJSONObject());
        JsonFunction.put(jsonObject, "arena", arena.toJSONObject());

        JsonFunction.put(jsonObject, "incomeAccount", Long.toUnsignedString(incomeAccount));
        JsonFunction.put(jsonObject, "operationFee", operationFee.toJSONObject());

        return  jsonObject;
    }

    public void define(JSONObject jsonObject, boolean isInitial) {

        if (jsonObject == null) {
            return;
        }

        definition = new Definition(JsonFunction.getJSONObject(jsonObject, "definition", null));
        arena = new ArenaState(applicationContext, JsonFunction.getJSONObject(jsonObject, "arena", null), isInitial);
        incomeAccount = JsonFunction.getLongFromStringUnsigned(jsonObject, "incomeAccount", applicationContext.contractAccountId);
        operationFee = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "operationFee", null));
    }

    public State(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        arena = new ArenaState(applicationContext);
    }

    public State(ApplicationContext applicationContext, JSONObject jsonObject) {
        this(applicationContext);
        define(jsonObject, false);
    }

    public boolean processBlock() {

        if (listOperationPending != null) {

            for (Operation operation: listOperationPending) {
                processOperation(operation);
            }

            listOperationPending.clear();
        }

        listOperationPending = listOperationPendingDelayed;
        listOperationPendingDelayed = new ArrayList<>();

        arena.processPendingArmies();

        return true;
    }

    public void queueOperation(Operation operation) {
        if (listOperationPending == null) {
            listOperationPending = new ArrayList<>();
        }

        listOperationPending.add(operation);
    }

    public JSONObject apiProcessBattleJsonRequest(JSONObject jsonObject) {
        JSONObject result = new JSONObject();

        if (jsonObject == null) {
            JsonFunction.put(result, "error", "missing parameter");
            return result;
        }

        int id = JsonFunction.getInt(jsonObject, "id", -1);

        if (id < 0) {
            JsonFunction.put(result, "error", "invalid id");
            return result;
        }

        JSONObject jsonBattle = Battle.battleLoad(applicationContext, id);

        if (jsonBattle != null) {
            result = jsonBattle;
        } else {
            JsonFunction.put(result, "error", "not found");
        }

        return result;
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
            case "configureAsMerge": {
                result = operationDefinitionMerge(operation);
                break;
            }

            case "formArmy": {
                result = operationFormArmy(operation);
                break;
            }
        }

        return result;
    }

    private boolean operationDefinitionMerge(Operation operation) {

        if (operation == null || operation.parameterJson == null) {
            return false;
        }

        if (operation.account != applicationContext.contractAccountId) {
            return false;
        }


        JSONObject parameters = operation.parameterJson;

        incomeAccount = JsonFunction.getLongFromStringUnsigned(parameters, "incomeAccount", applicationContext.contractAccountId);

        JSONObject feeObject = JsonFunction.getJSONObject(parameters, "operationFee", null);

        if (feeObject != null) {
            PlatformToken newFee = new PlatformToken(feeObject);

            if (newFee.isValid()) {
                operationFee = newFee;
            }
        }

        JSONObject objectDefinitionArenaDefault = JsonFunction.getJSONObject(parameters, "arenaDefault", null);
        JSONArray arrayDefinitionSoldier = JsonFunction.getJSONArray(parameters, "soldier", null);
        JSONArray arrayDefinitionMedium = JsonFunction.getJSONArray(parameters, "medium", null);
        JSONArray arrayDefinitionDomain = JsonFunction.getJSONArray(parameters, "domain", null);
        JSONArray arrayDefinitionArena = JsonFunction.getJSONArray(parameters, "arena", null);
        JSONArray arrayDefinitionItemForBonus = JsonFunction.getJSONArray(parameters, "itemForBonus", null);

        if (arrayDefinitionItemForBonus != null) {

            for (Object definitionItemForBonus: arrayDefinitionItemForBonus) {
                ItemForBonus itemForBonus = new ItemForBonus((JSONObject) definitionItemForBonus);

                definition.itemForBonus.put(itemForBonus.asset, itemForBonus);
            }
        }

        if (arrayDefinitionMedium != null) {

            for (Object o: arrayDefinitionMedium) {
                Medium medium = new Medium((JSONObject) o);

                definition.medium.put(medium.id, medium);
            }
        }

        if (arrayDefinitionDomain != null) {

            for (Object o: arrayDefinitionDomain) {
                Domain domain = new Domain((JSONObject) o);

                definition.domain.put(domain.id, domain);
            }
        }

        definition.soldier.defineSoldier(arrayDefinitionSoldier, true);

        arena.setArenaDefault(objectDefinitionArenaDefault, true);
        arena.defineArena(arrayDefinitionArena, true);

        return true;
    }

    private boolean operationFormArmy(Operation operation) {

        if (operation == null || operation.parameterJson == null) {
            applicationContext.logDebugMessage("formArmy: missing parameter");
            return false;
        }

        JSONObject parameters = operation.parameterJson;
        List<Long> listAsset = JsonFunction.getListLongFromJsonArrayStringUnsigned(parameters, "asset", null);

        if (listAsset == null || listAsset.size() == 0) {
            applicationContext.logDebugMessage("formArmy: invalid asset list");
            return false;
        }

        int arenaId = JsonFunction.getInt(parameters, "arena", -1);

        if (arenaId < 0) {
            applicationContext.logDebugMessage("formArmy: invalid arena id");
            return false;
        }

        PlatformToken costBattle = applicationContext.state.rgame.arena.getBattleCost(arenaId);

        Army army = new Army(applicationContext, arenaId, operation.account, listAsset, costBattle, true);

        boolean isValid = false;

        if (army.isValid(true)) {
            isValid = arena.addPendingArmy(army, true, true);
        } else {
            applicationContext.logDebugMessage("formArmy: invalid army");
        }

        return isValid;
    }

    public boolean isIncompleteArmyValid(int arenaId, long account, List<Long> listAsset) {

        if (listAsset == null || listAsset.size() == 0) {
            return false;
        }

        Army army = createArmy(arenaId, account, listAsset);

        if (army == null) {
            return false;
        }

        List<Long> listAccepted = army.getSoldierAssetsIds();

        if (listAccepted == null || listAccepted.size() != listAsset.size()) {
            return false;
        }

        return army.isValid(false);
    }

    public Army createArmy(int arenaId, long account, List<Long> listAsset) {

        if (listAsset == null || listAsset.size() == 0) {
            return null;
        }

        return new Army(applicationContext, arenaId, account, listAsset, null, true);
    }

    public Battle createBattle(int arenaId, EconomicCluster economicCluster, Army defender, Army attacker) {

        if (arenaId <= 0 || economicCluster == null || defender == null || !defender.isValid(true) || attacker == null || !attacker.isValid(true)) {
            return null;
        }

        return new Battle(applicationContext, 1, arenaId, economicCluster, defender, attacker);
    }

    public List<Integer> getArenaIds() {

        if (arena == null || arena.mapArena == null || arena.mapArena.size() == 0) {
            return null;
        }

        List<Integer> result = new ArrayList<>();

        for (Arena item: arena.mapArena.values()) {

            if (item.isValid() && item.id != 0) {
                result.add(item.id);
            }
        }

        return result;
    }

    public List<Arena> getArenaList() {

        if (arena == null || arena.mapArena == null || arena.mapArena.size() == 0) {
            return null;
        }

        List<Arena> result = new ArrayList<>();

        for (Arena item: arena.mapArena.values()) {

            if (item.isValid() && item.id != 0) {
                result.add(new Arena(applicationContext, item.toJSONObject(), false, false));
            }
        }

        return result;
    }

    public Arena getArenaClone(int id) {

        if (id <= 0 || arena == null || arena.mapArena == null || arena.mapArena.size() == 0 || !arena.mapArena.containsKey(id)) {
            return null;
        }

        return new Arena(applicationContext, arena.mapArena.get(id).toJSONObject(), false, false);
    }

    public PlatformToken getOperationFeeIds() {

        PlatformToken result = operationFee;

        if (result == null) {
            result = new PlatformToken();
        }

        if (applicationContext.state.userAccountState.operationFee != null) {
            result.mergeAsId(applicationContext.state.userAccountState.operationFee);
        }

        return result;
    }

    public PlatformToken getSoldierAssetTokenIds() {

        if (definition == null || definition.soldier == null || definition.soldier.soldierDefinition == null || definition.soldier.soldierDefinition.size() == 0) {
            return null;
        }

        PlatformToken result = new PlatformToken();

        for (Soldier item: definition.soldier.soldierDefinition.values()) {

            if (!item.isValid()) {
                continue;
            }

            result.mergeAssetToken(item.asset, 1, true);
        }

        result.setValues(1);

        return result;
    }

    public PlatformToken getItemForBonusAssetTokenIds() {

        if (definition == null || definition.itemForBonus == null || definition.itemForBonus.size() == 0) {
            return null;
        }

        PlatformToken result = new PlatformToken();

        for (ItemForBonus item: definition.itemForBonus.values()) {

            if (!item.isValid()) {
                continue;
            }

            result.mergeAssetToken(item.asset, 1, true);
        }

        result.setValues(1);

        return result;
    }

    public PlatformToken getPaymentTokenIds() {

        if (arena == null || arena.mapArena == null || arena.mapArena.size() == 0) {
            return null;
        }

        PlatformToken result = new PlatformToken();

        for (Arena item: arena.mapArena.values()) {

            if (!item.isValid()) {
                continue;
            }

            PlatformToken cost = item.battleCost;

            if (cost == null || cost.isZero()) {
                continue;
            }

            cost = cost.clone();

            result.mergeAsId(cost);
        }

        return result;
    }

    public PlatformToken getTokenIds() {
        PlatformToken result = new PlatformToken();

        result.merge(getSoldierAssetTokenIds(), true);
        result.merge(getItemForBonusAssetTokenIds(), true);
        result.merge(getPaymentTokenIds(), true);

        result.mergeAsId(getOperationFeeIds());

        return result;
    }

    public Soldier getSoldierCloneByAssetId(long assetId) {

        if (assetId == 0 || !isValid() || definition == null || definition.soldier == null || definition.soldier.soldierDefinition == null || definition.soldier.soldierDefinition.size() == 0) {
            return null;
        }

        for (Soldier item: definition.soldier.soldierDefinition.values()) {

            if (!item.isValid()) {
                continue;
            }

            if (item.asset == assetId) {
                return item.clone();
            }
        }

        return null;
    }

    public List<Soldier> listSoldierFromListAssetToken(List<Long> listAssetTokenIds) {

        if (listAssetTokenIds == null || listAssetTokenIds.size() == 0) {
            return null;
        }

        List<Soldier> result = new ArrayList<>();

        for (long id: listAssetTokenIds) {
            Soldier item = getSoldierCloneByAssetId(id);

            if (item != null && item.isValid()) {
                result.add(item);
            }
        }

        return result;
    }

    public boolean transferToBalanceIncome(PlatformToken platformToken, long account, boolean log) {
        if (!applicationContext.state.userAccountState.subtractFromBalance(account, platformToken)){
            return false;
        }

        addToBalanceIncome(platformToken, log);

        return true;
    }

    public void addToBalanceIncome(PlatformToken platformToken, boolean log) {
        applicationContext.state.userAccountState.addToBalance(incomeAccount, platformToken);

        if (log) {
            applicationContext.logInfoMessage("income: " + Long.toUnsignedString(incomeAccount) + ": + " + platformToken.toJSONObject().toJSONString());
        }
    }
}
