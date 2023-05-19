package org.tarasca.mythicalbeings.rgame.omno.service.object;

import concept.omno.ApplicationContext;
import concept.omno.object.PlatformToken;
import concept.platform.EconomicCluster;
import concept.utility.JsonFunction;
import org.json.simple.JSONObject;
import org.tarasca.mythicalbeings.rgame.omno.service.ArenaState;
import org.tarasca.mythicalbeings.rgame.omno.service.Definition;

import java.util.*;

public class Army {
    final ApplicationContext applicationContext;

    public long account = 0;
    private boolean isReady = false; // set false on modification
    public int arenaId = -1;
    long itemForBonusAssetId = 0;
    SortedSet<Long> soldierAssetIdsSorted = new TreeSet<>();
    HashMap<Integer, Integer> mapRankSize = new HashMap<>();

    long soldierHeroConceptAsset = 0;
    public PlatformToken cost = new PlatformToken();

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();

        JsonFunction.put(jsonObject, "account", Long.toUnsignedString(account));
        JsonFunction.put(jsonObject, "arenaId", arenaId);

        JsonFunction.put(jsonObject, "itemAsset", Long.toUnsignedString(itemForBonusAssetId));
        JsonFunction.put(jsonObject, "heroAsset", Long.toUnsignedString(soldierHeroConceptAsset));

        JsonFunction.put(jsonObject, "asset", JsonFunction.jsonArrayStringsFromListLongUnsigned(new ArrayList<>(soldierAssetIdsSorted)));
        JsonFunction.put(jsonObject, "rank", JsonFunction.jsonObjectKeyValuePairsFromMapIntInt(mapRankSize));

        if (cost != null) {
            JsonFunction.put(jsonObject, "cost", cost.toJSONObject());
        }

        return  jsonObject;
    }

    public void define(JSONObject jsonObject) {

        account = JsonFunction.getLongFromStringUnsigned(jsonObject, "account", account);
        itemForBonusAssetId = JsonFunction.getLongFromStringUnsigned(jsonObject, "itemAsset", itemForBonusAssetId);
        soldierHeroConceptAsset = JsonFunction.getLongFromStringUnsigned(jsonObject, "heroAsset", soldierHeroConceptAsset);

        arenaId = JsonFunction.getInt(jsonObject, "arenaId", arenaId);

        soldierAssetIdsSorted = new TreeSet<>();

        List<Long> list = JsonFunction.getListLongFromJsonArrayStringUnsigned(jsonObject, "asset", null);

        if (list != null && list.size() > 0) {
            soldierAssetIdsSorted.addAll(list);
        }

        mapRankSize = JsonFunction.getHashMapIntIntFromKeyValuePairs(jsonObject, "rank", mapRankSize);

        cost = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "cost", null));
    }

    public Army(ApplicationContext applicationContext, JSONObject jsonObject) {
        this.applicationContext = applicationContext;
        define(jsonObject);
    }

    public Army(ApplicationContext applicationContext, int arenaId, long account, List<Long> listAsset, PlatformToken cost, boolean validate) {

        this.applicationContext = applicationContext;

        if (arenaId < 0 || account == 0 || listAsset == null || listAsset.size() == 0) {
            return;
        }

        this.account = account;
        this.arenaId = arenaId;

        for (Long asset: listAsset) {
            addAsset(asset, validate);
        }

        if (cost != null) {
            this.cost = cost.clone();
        } else {
            this.cost = new PlatformToken();
        }
    }

    public boolean isValid(boolean isComplete) {

        if (isReady) {
            return true;
        }

        if (account == 0 || arenaId < 0 || soldierAssetIdsSorted == null || soldierAssetIdsSorted.size() == 0 || mapRankSize == null || mapRankSize.size() == 0) {
            return false;
        }

        ArenaState arenaState = applicationContext.state.rgame.arena;
        Arena arena = arenaState.getArena(arenaId);

        if (arena == null || !arena.isValid()) {
            return false;
        }

        for (int id: arena.armyRankMaximum.keySet()) {

            if (!mapRankSize.containsKey(id)) {
                continue;
            }

            if (((int) ((Number) mapRankSize.get(id)).longValue()) > ((int) ((Number) arena.armyRankMaximum.get(id)).longValue())) {
                return false;
            }
        }

        if (isComplete) {
            for (int id: arena.armyRankMinimum.keySet()) {

                if (!mapRankSize.containsKey(id)) {

                    if (((int) ((Number) arena.armyRankMinimum.get(id)).longValue()) == 0) {
                        continue;
                    }

                    return false;
                }

                if (((int) ((Number) mapRankSize.get(id)).longValue()) < ((int) ((Number) arena.armyRankMinimum.get(id)).longValue())) {
                    return false;
                }
            }
        }

        if (isComplete) {
            isReady = true;
        }

        return true;
    }

    public PlatformToken getAssetCost() {
        PlatformToken platformToken = new PlatformToken();

        if (!isValid(true)) {
            return platformToken;
        }

        List<Long> listAssets = new ArrayList<>(soldierAssetIdsSorted);

        for (long asset: listAssets) {
            platformToken.mergeAssetToken(asset, 1, true);
        }

        if (itemForBonusAssetId != 0) {
            platformToken.mergeAssetToken(itemForBonusAssetId, 1, true);
        }

        return platformToken;
    }

    public boolean addAsset(long asset, boolean validate) {

        Definition definition = applicationContext.state.rgame.definition;

        if (validate && !canAddAsset(asset)) {
            return false;
        }

        isReady = false;

        if (definition.itemForBonus.containsKey(asset)) {

            itemForBonusAssetId = asset;

        } else {

            soldierAssetIdsSorted.add(asset);
            Soldier soldier = definition.soldier.getSoldierByAsset(asset);

            if (soldier == null) {
                return false;
            }

            rankAdd(soldier.rank, 1);

            if (soldierHeroConceptAsset == 0) {
                soldierHeroConceptAsset = asset;
            } else {
                Soldier soldierHeroConcept = definition.soldier.getSoldierByAsset(soldierHeroConceptAsset);
                if (soldierHeroConcept.rank < soldier.rank) {
                    soldierHeroConceptAsset = soldier.asset;
                }
            }
        }

        return true;
    }

    public boolean canAddAsset(long asset) {

        Definition definition = applicationContext.state.rgame.definition;
        ArenaState arenaState = applicationContext.state.rgame.arena;

        if (asset == 0) {
            return false;
        }

        Soldier soldier = definition.soldier.getSoldierByAsset(asset);

        if (definition.itemForBonus.containsKey(asset)) {

            return itemForBonusAssetId == 0;
        }

        if (soldier == null) {
            return false;
        }

        if (arenaState.isUniqueSoldierRequired(arenaId) && soldierAssetIdsSorted.contains(asset)) {
            return false;
        }

        return mapRankSize == null || mapRankSize.size() == 0 || !mapRankSize.containsKey(soldier.rank) || mapRankSize.get(soldier.rank) < arenaState.getRankMaximum(arenaId, soldier.rank);
    }

    public int rankAdd(int rank, int delta) {

        int value = delta;

        if (mapRankSize.containsKey(rank)) {
            value += mapRankSize.get(rank);
        }

        mapRankSize.put(rank, value);

        return value;
    }

    public List<Soldier> getSoldiersInFightOrder(int battleId, EconomicCluster economicCluster) {

        List<Long> listSoldierAssets = new ArrayList<>(soldierAssetIdsSorted);

        List<Soldier> listResult = new ArrayList<>();
        Random random = applicationContext.state.getCombinedRandom(battleId, economicCluster);

        int count = listSoldierAssets.size();

        for (int i = 0; i < count; i++) {
            int index = (int) (listSoldierAssets.size() * random.nextDouble());
            long asset = listSoldierAssets.get(index);
            Soldier soldier = applicationContext.state.rgame.definition.soldier.getSoldierByAsset(asset);

            if (soldier == null) {
                continue;
            }

            soldier = soldier.clone();

            listResult.add(soldier);
            listSoldierAssets.remove(index);
        }

        return listResult;
    }

    public List<Long> getSoldierAssetsIds() {

        if (soldierAssetIdsSorted == null || soldierAssetIdsSorted.size() == 0) {
            return null;
        }

        return new ArrayList<>(soldierAssetIdsSorted);
    }
}
