package org.tarasca.mythicalbeings.rgame.omno.service;

import concept.omno.ApplicationContext;
import concept.omno.object.PlatformToken;
import concept.utility.JsonFunction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.tarasca.mythicalbeings.rgame.omno.service.object.Arena;
import org.tarasca.mythicalbeings.rgame.omno.service.object.Army;
import org.tarasca.mythicalbeings.rgame.omno.service.object.Battle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ArenaState {
    final ApplicationContext applicationContext;

    public HashMap<Integer, Arena> mapArena = new HashMap<>();
    Arena arenaDefault = new Arena();
    List<Army> listPendingArmy = new ArrayList<>();

    public int battleCount = 0;

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();

        JSONArray arenaJsonArray = new JSONArray();

        List<Arena> listArena = new ArrayList<>(mapArena.values());

        for (Arena arena: listArena) {
            JsonFunction.add(arenaJsonArray, arena.toJSONObject());
        }

        JsonFunction.put(jsonObject, "arena", arenaJsonArray);
        JsonFunction.put(jsonObject, "arenaDefault", arenaDefault.toJSONObject());
        JsonFunction.put(jsonObject, "battleCount", battleCount);

        JSONArray jsonArray = new JSONArray();

        for(Army army: listPendingArmy) {
            JsonFunction.add(jsonArray, army.toJSONObject());
        }

        JsonFunction.put(jsonObject, "pendingArmy", jsonArray);

        return  jsonObject;
    }

    public void defineWithoutValidation(JSONObject jsonObject, boolean isInitial) {

        battleCount = JsonFunction.getInt(jsonObject, "battleCount", 0);

        arenaDefault = new Arena(applicationContext, JsonFunction.getJSONObject(jsonObject, "arenaDefault", null), true, isInitial);

        JSONArray mapArenaJsonArray = JsonFunction.getJSONArray(jsonObject, "arena", null);

        if (mapArenaJsonArray != null) {
            mapArena = new HashMap<>();

            for(Object object: mapArenaJsonArray) {
                JSONObject arenaJson = (JSONObject) object;
                Arena arena = new Arena(applicationContext, arenaJson, false, isInitial); // must not depend on arenaDefault to re-sync state

                if (arena.id >= 0) {
                    mapArena.put(arena.id, arena);
                }
            }
        }

        List<JSONObject> listArmyJson = JsonFunction.getListJSONObject(jsonObject, "pendingArmy", null);

        if (listArmyJson != null) {
            for(JSONObject object: listArmyJson) {
                Army army = new Army(applicationContext, object);
                listPendingArmy.add(army);
            }
        }
    }

    public ArenaState(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    ArenaState(ApplicationContext applicationContext, JSONObject jsonObject, boolean isInitial) {
        this(applicationContext);
        defineWithoutValidation(jsonObject, isInitial);
    }

    public void processPendingArmies() {

        if (listPendingArmy == null || listPendingArmy.size() == 0) {
            return;
        }

        for (int i = 0; i < listPendingArmy.size(); i++) {
            Army attacker = listPendingArmy.get(i);

            if (!attacker.isValid(true)) {
                listPendingArmy.remove(i--);
                continue;
            }

            Arena arena = getArena(attacker.arenaId);

            if (arena == null || !arena.isValid()) {
                // wait for configuration update

                if (arena != null) {
                    applicationContext.logDebugMessage("invalid arena: " + arena.toJSONObject());
                } else {
                    applicationContext.logDebugMessage("invalid arena: " + arena);
                }

                continue;
            }

            if (arena.defender.account == attacker.account) {
                continue; // stacked defender army
            }

            applicationContext.logDebugMessage("creating battle: " + arena.id);
            applicationContext.logDebugMessage(applicationContext.state.economicCluster.toJSONObject().toJSONString());
            applicationContext.logDebugMessage(arena.toJSONObject().toJSONString());
            applicationContext.logDebugMessage(attacker.toJSONObject().toJSONString());
//                applicationContext.logDebugMessage("rank: " + attacker.mapRankSize);

            Battle battle = new Battle(applicationContext, battleCount + 1, arena.id, applicationContext.state.economicCluster, arena.defender, attacker);
            applicationContext.logDebugMessage("battle: " + battle.toJSONObject().toJSONString());

            if (!battle.isValidPreBattle(true)) {
                applicationContext.logDebugMessage("invalid pre-battle: " + battle.toJSONObject().toJSONString() + ": " + Long.toUnsignedString(attacker.account) + ": " + attacker.toJSONObject());
                applicationContext.logDebugMessage("arena: " + arena.toJSONObject());
                continue;
            }

            battle.calculateBattle(false);

            applicationContext.logInfoMessage("Omno | Battle: " + battle.toJSONObject().toJSONString());

            Army defeated = attacker;
            Army defender = arena.defender;

            double defenderRewardFraction = battle.isWinnerLowerPower() ? arena.rewardChainFraction: arena.rewardChainFractionIfEqualOrStronger;

            if (! battle.isDefenderWin) {
                defeated = arena.defender;
                defender = attacker;
                applicationContext.logInfoMessage("Omno | New defender: " + Long.toUnsignedString(arena.defender.account) + ": " + arena.toJSONObject().toJSONString());
            }

            PlatformToken bountyForDefender = attacker.cost.clone();
            PlatformToken bountyForIncome = bountyForDefender.clone();

            if (arena.rewardCostIsChainTokenOnly) {
                bountyForDefender.keepChainTokenOnly();
            }

            bountyForDefender.multiply(defenderRewardFraction);
            bountyForIncome.merge(bountyForDefender, false);

            { // state update

                // update account state
                applicationContext.state.rgame.addToBalanceIncome(bountyForIncome, true);
                applicationContext.state.userAccountState.addToBalance(defender.account, bountyForDefender);

                applicationContext.state.userAccountState.addToBalance(defender.account, battle.tokensCaptured);
                applicationContext.state.userAccountState.addToBalance(defeated.account, battle.tokensRefunded);

                // save details for front-end before discard
                battle.save();

                listPendingArmy.remove(i--);
                battleCount++;

                // sync arena with battle result
                if (!battle.isDefenderWin) {
                    arena.defender = attacker;
                }
            }
        }
    }

    public boolean addPendingArmy(Army army, boolean payBattleCost, boolean payArmyAssets) {

        if (army == null || ! army.isValid(true)) {
            applicationContext.logDebugMessage("addPendingArmy: army not valid " + army);
            return false;
        }

        PlatformToken platformToken = null;

        if (payBattleCost) {
            platformToken = army.cost.clone();
        }

        if (payArmyAssets) {
            if (platformToken == null) {
                platformToken = new PlatformToken();
            }

            PlatformToken costAssets = army.getAssetCost();

            if (costAssets != null) {
                platformToken.merge(costAssets, true);
            }
        }

        if (platformToken != null) {
            if (!applicationContext.state.userAccountState.subtractFromBalance(army.account, platformToken)) {
                applicationContext.logDebugMessage("addPendingArmy: not enough balance: " + platformToken.toJSONObject().toJSONString());
                return false;
            }
        }

        listPendingArmy.add(army);

        return true;
    }

    public void setArenaDefault(JSONObject jsonObject, boolean isInitial) {
        if (jsonObject == null) {
            return;
        }

        arenaDefault.define(applicationContext, jsonObject, null, true, isInitial);
    }

    public void setArenaDefault(Arena arena) {
        if (arena == null) {
            return;
        }

        arenaDefault = arena;
    }

    public void defineArena(JSONArray jsonArray, boolean isInitial) {
        if (jsonArray == null) {
            return;
        }

        for (Object o: jsonArray) {
            defineArena((JSONObject) o, isInitial);
        }
    }

    public void defineArena(JSONObject json, boolean isInitial) {
        if (json == null) {
            return;
        }

        Arena arena = new Arena();

        arena.define(applicationContext, json, arenaDefault, false, isInitial);

        if (arena.isValid()) {
            mapArena.put(arena.id, arena);
        } else {
            applicationContext.logErrorMessage("arena not valid: " + arena.toJSONObject());
        }
    }

    public Arena getArena(int id) {
        Arena arena = null;

        if (mapArena.containsKey(id)) {
            arena = mapArena.get(id);
        }

        return arena;
    }

    public boolean isUniqueSoldierRequired(int id) {
        boolean isRequired;

        Arena arena = null;

        if (mapArena.containsKey(id)) {
            arena = mapArena.get(id);
        }

        if (arena == null) {
            throw (new NullPointerException());
        }

        isRequired = arena.armyRequireUnique;

        return isRequired;
    }

    public PlatformToken getBattleCost(int id) {
        PlatformToken platformToken = null;

        Arena arena = getArena(id);

        if (arena != null) {
            platformToken = arena.getBattleCost();
        }

        return platformToken;
    }

    public boolean isRewardCostChainTokenOnly(int id) {
        boolean result = true;

        Arena arena = getArena(id);

        if (arena != null) {
            result = arena.rewardCostIsChainTokenOnly;
        }

        return result;
    }

    public boolean isDefender(int id, long account) {

        Arena arena = getArena(id);

        if (arena == null || arena.defender == null) {
            return false;
        }

        return arena.defender.account == account;
    }

    public int getRankMaximum(int id, int rank) {
        int value = 0;

        Arena arena;

        if (mapArena.containsKey(id)) {
            arena = mapArena.get(id);
        } else {
            return value;
        }

        value = arena.getRankMaximum(rank);

        return value;
    }

    public int getRankMinimum(int id, int rank) {
        int value = 0;

        Arena arena;

        if (mapArena.containsKey(id)) {
            arena = mapArena.get(id);
        } else {
            return value;
        }

        value = arena.getRankMinimum(rank);

        return value;
    }

    public int getDiceRollSize(int id) {
        int value = 0;

        Arena arena;

        if (mapArena.containsKey(id)) {
            arena = mapArena.get(id);
        } else {
            return value;
        }

        value = arena.getDiceRollSize();

        return value;
    }

    public int getItemLossCount(int id) {
        int value = 0;

        Arena arena;

        if (mapArena.containsKey(id)) {
            arena = mapArena.get(id);
        } else {
            return value;
        }

        value = arena.cardWinQuantity;

        return value;
    }
}
