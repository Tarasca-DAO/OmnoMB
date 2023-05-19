package org.tarasca.mythicalbeings.rgame.omno.service.object;

import concept.omno.ApplicationContext;
import concept.omno.object.PlatformToken;
import concept.utility.JsonFunction;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Arena {
    public int id = -1;
    public int domainId = -1;
    public int mediumId = -1;
    int defenderBonusOnDomainMatch = 1;
    public double rewardChainFraction = 0.5F;
    public double rewardChainFractionIfEqualOrStronger = 0.25F;
    public HashMap<Integer, Integer> armyRankMaximum = new HashMap<>();
    public HashMap<Integer, Integer> armyRankMinimum = new HashMap<>();
    public boolean armyRequireUnique = true;
    public PlatformToken battleCost = new PlatformToken();
    public boolean rewardCostIsChainTokenOnly = true;
    public int cardWinQuantity = 1;
    int diceRollSize = 6;
    public Army defender = null;

    public boolean isValid() {
        return id >= 0 && domainId >= 0 && mediumId >= 0 && armyRankMaximum != null && armyRankMinimum != null && defender != null;
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();

        JsonFunction.put(jsonObject, "id", id);
        JsonFunction.put(jsonObject, "domainId", domainId);
        JsonFunction.put(jsonObject, "mediumId", mediumId);

        JsonFunction.put(jsonObject, "defenderBonusOnDomainMatch", defenderBonusOnDomainMatch);

        JsonFunction.put(jsonObject, "armyRankMaximum", JsonFunction.jsonArrayFromListInteger(new ArrayList<>(armyRankMaximum.values())));
        JsonFunction.put(jsonObject, "armyRankMinimum", JsonFunction.jsonArrayFromListInteger(new ArrayList<>(armyRankMinimum.values())));

        JsonFunction.put(jsonObject, "rewardChainFraction", Double.toString(rewardChainFraction));
        JsonFunction.put(jsonObject, "rewardChainFractionIfEqualOrStronger", Double.toString(rewardChainFractionIfEqualOrStronger));

        JsonFunction.put(jsonObject, "armyRequireUnique", armyRequireUnique);

        JsonFunction.put(jsonObject, "cardWinQuantity", cardWinQuantity);
        JsonFunction.put(jsonObject, "diceRollSize", diceRollSize);

        JsonFunction.put(jsonObject, "battleCost", battleCost.toJSONObject());
        JsonFunction.put(jsonObject, "rewardCostIsChainTokenOnly", rewardCostIsChainTokenOnly);

        if (defender != null) {
            JsonFunction.put(jsonObject, "defender", defender.toJSONObject());
        }

        return  jsonObject;
    }

    public PlatformToken getBattleCost() {
        return battleCost.clone();
    }

    public Arena() {}

    public Arena(ApplicationContext applicationContext, JSONObject jsonObject, boolean isDefault, boolean isInitial) {
        define(applicationContext, jsonObject, null, isDefault, isInitial);
    }

    public void define(ApplicationContext applicationContext, JSONObject jsonObject, Arena arenaDefault, boolean isDefault, boolean isInitial) {

        if (jsonObject == null) {
            return;
        }

        if (arenaDefault == null) {
            arenaDefault = new Arena();
        }

        id = JsonFunction.getInt(jsonObject, "id", -1);
        domainId = JsonFunction.getInt(jsonObject, "domainId", -1);
        mediumId = JsonFunction.getInt(jsonObject, "mediumId", -1);

        defenderBonusOnDomainMatch = JsonFunction.getInt(jsonObject, "defenderBonusOnDomainMatch", arenaDefault.defenderBonusOnDomainMatch);
        rewardChainFraction = JsonFunction.getDoubleFromString(jsonObject, "rewardChainFraction", arenaDefault.rewardChainFraction);
        rewardChainFractionIfEqualOrStronger = JsonFunction.getDoubleFromString(jsonObject, "rewardChainFractionIfEqualOrStronger", arenaDefault.rewardChainFractionIfEqualOrStronger);

        List<Integer> listInteger;

        listInteger = JsonFunction.getListIntegerFromJsonArray(jsonObject, "armyRankMaximum", new ArrayList<>(arenaDefault.armyRankMaximum.values()));

        int i = 0;

        for (Integer integer: listInteger) {
            armyRankMaximum.put(i++, integer);
        }

        listInteger = JsonFunction.getListIntegerFromJsonArray(jsonObject, "armyRankMinimum", new ArrayList<>(arenaDefault.armyRankMinimum.values()));

        i = 0;

        for (Integer integer: listInteger) {
            armyRankMinimum.put(i++, integer);
        }

        armyRequireUnique = JsonFunction.getBoolean(jsonObject, "armyRequireUnique", arenaDefault.armyRequireUnique);

        JSONObject battleCostJson = JsonFunction.getJSONObject(jsonObject, "battleCost", null);

        battleCost = new PlatformToken(battleCostJson);

        if (battleCostJson == null || !battleCost.isValid()) {
            battleCost = arenaDefault.battleCost.clone();
        }

        rewardCostIsChainTokenOnly = JsonFunction.getBoolean(jsonObject, "rewardCostIsChainTokenOnly", true);

        cardWinQuantity = JsonFunction.getInt(jsonObject, "cardWinQuantity", arenaDefault.cardWinQuantity);
        diceRollSize = JsonFunction.getInt(jsonObject, "diceRollSize", arenaDefault.diceRollSize);

        if (defender == null) {

            JSONObject armyObject = JsonFunction.getJSONObject(jsonObject, "defender", null);

            if (armyObject != null) {
                if (isInitial) {
                    long account = JsonFunction.getLongFromStringUnsigned(armyObject, "account", 0);
                    List<Long> listAsset = JsonFunction.getListLongFromJsonArrayStringUnsigned(armyObject, "asset", new ArrayList<>());

                    defender = new Army(applicationContext, id, account, listAsset, battleCost, false);
                } else {
                    defender = new Army(applicationContext, armyObject);
                }
            } else if (!isDefault) {
                // use or create a default?
                throw new RuntimeException();
            }
        }
    }

    public int getRankMaximum(int rank) {
        int value = 0;

        if (armyRankMaximum.containsKey(rank)) {
            value = armyRankMaximum.get(rank);
        }

        return value;
    }

    public int getRankMinimum(int rank) {
        int value = 0;

        if (armyRankMinimum.containsKey(rank)) {
            value = armyRankMinimum.get(rank);
        }

        return value;
    }

    public int getDiceRollSize() {
        return diceRollSize;
    }

    public long getAccountId() {

        if (defender == null) {
            return 0;
        }

        return defender.account;
    }
}
