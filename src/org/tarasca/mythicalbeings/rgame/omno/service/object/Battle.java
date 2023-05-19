package org.tarasca.mythicalbeings.rgame.omno.service.object;

import concept.omno.ApplicationContext;
import concept.omno.object.PlatformToken;
import concept.platform.EconomicCluster;
import concept.utility.FileUtility;
import concept.utility.JsonFunction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Battle {

    final ApplicationContext applicationContext;
    public int id = -1;
    public EconomicCluster economicCluster = null;

    public int version = 0;

    public int arenaId = -1;
    public List<Soldier> attacker = null;
    public List<Soldier> defender = null;

    public Army attackerArmy, defenderArmy;

    int attackerHeroIndex = 0;
    int defenderHeroIndex = 0;

    int attackerItemForBonusSoldierIndex = 0;
    long attackerItemForBonusAssetId = 0;

//        long seedForRandom; //hash(block+armySeedForRandom*2)

    public List<Integer> listDiceRolls = new ArrayList<>(); //defender,attacker pairs

    public boolean isDefenderWin = false;
    boolean isComplete = false;

    public PlatformToken tokensCaptured = new PlatformToken();
    public PlatformToken tokensRefunded = new PlatformToken();

    public JSONObject toJSONObject() {

        JSONObject jsonObject = new JSONObject();

        if (economicCluster != null) {
            JsonFunction.put(jsonObject, "economicCluster", economicCluster.toJSONObject());
        }

        JsonFunction.put(jsonObject, "id", id);
        JsonFunction.put(jsonObject, "arenaId", arenaId);

        if (version != 0) {
            JsonFunction.put(jsonObject, "version", version);
        }

        JsonFunction.put(jsonObject, "isDefenderWin", isDefenderWin);

        JSONArray attackerJsonArray = new JSONArray();

        if (attacker != null && attacker.size() != 0) {
            for (Soldier soldier: attacker) {
                if (soldier != null) {
                    JsonFunction.add(attackerJsonArray, soldier.toJSONObject());
                }
            }
        }

        JSONArray defenderJsonArray = new JSONArray();

        if (defender != null && defender.size() != 0) {
            for (Soldier soldier: defender) {
                if (soldier != null) {
                    JsonFunction.add(defenderJsonArray, soldier.toJSONObject());
                }
            }
        }

        JsonFunction.put(jsonObject, "attacker", attackerJsonArray);
        JsonFunction.put(jsonObject, "defender", defenderJsonArray);

        JsonFunction.put(jsonObject, "roll", JsonFunction.jsonArrayFromListInteger(listDiceRolls));

        JsonFunction.put(jsonObject, "captured", tokensCaptured.toJSONObject());

        if (attackerArmy != null) {
            JsonFunction.put(jsonObject, "attackerArmy", attackerArmy.toJSONObject());
        }

        if (defenderArmy != null) {
            JsonFunction.put(jsonObject, "defenderArmy", defenderArmy.toJSONObject());
        }

        return  jsonObject;
    }

    Battle(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public Battle(ApplicationContext applicationContext, JSONObject jsonObject) {

        this(applicationContext);

        if (jsonObject == null) {
            return;
        }

//        int version = JsonFunction.getInt(jsonObject, "version", 1);

        int id = JsonFunction.getInt(jsonObject, "id", -1);
        int arenaId = JsonFunction.getInt(jsonObject, "arenaId", -1);
        EconomicCluster economicCluster = new EconomicCluster(JsonFunction.getJSONObject(jsonObject, "economicCluster", null));

        Army attackerArmy = new Army(applicationContext, JsonFunction.getJSONObject(jsonObject, "attackerArmy", null));
        Army defenderArmy = new Army(applicationContext, JsonFunction.getJSONObject(jsonObject, "defenderArmy", null));

        define(id, arenaId, economicCluster, defenderArmy, attackerArmy);
    }

    public Battle(ApplicationContext applicationContext, int id, int arenaId, EconomicCluster economicCluster, Army defender, Army attacker) {

        this(applicationContext);

        define(id, arenaId, economicCluster, defender, attacker);
    }

    private void define(int id, int arenaId, EconomicCluster economicCluster, Army defender, Army attacker) {

        if (id < 0 || arenaId < 0 || economicCluster == null || ! economicCluster.isValid(applicationContext.ardorApi)) {
            return;
        }

        if (defender == null || !defender.isValid(true) || attacker == null || !attacker.isValid(true)) {
            return;
        }

        // for narrative
        attackerArmy = attacker;
        defenderArmy = defender;
        //

        this.economicCluster = economicCluster.clone();
        this.id = id;
        this.arenaId = arenaId;
        this.attacker = attacker.getSoldiersInFightOrder(id, economicCluster);
        this.defender = defender.getSoldiersInFightOrder(id, economicCluster);

        // ItemForBonus soldier pick
        attackerItemForBonusAssetId = attacker.itemForBonusAssetId;
        Random random = applicationContext.state.getCombinedRandom((long) id + attackerItemForBonusAssetId, economicCluster);
        attackerItemForBonusSoldierIndex = (int) (random.nextDouble() * this.attacker.size());
        //

        for (Soldier soldier: this.attacker) {

            if (soldier.asset == attacker.soldierHeroConceptAsset) {
                break;
            }

            attackerHeroIndex++;
        }

        for (Soldier soldier: this.defender) {

            if (soldier.asset == defender.soldierHeroConceptAsset) {
                break;
            }

            defenderHeroIndex++;
        }
    }

    public boolean isValidPreBattle(boolean isComplete) {
        return id >= 0 && arenaId >= 0 && defender != null && defender.size() != 0 && attacker != null && attacker.size() != 0 && attackerArmy != null && attackerArmy.isValid(isComplete);
    }

    public boolean isValidPostBattle() {
        return isValidPreBattle(true) && listDiceRolls != null && listDiceRolls.size() % 2 != 1 && listDiceRolls.size() != 0;
    }

    public void applyConceptHeroAura(Soldier hero, boolean isHeroAttacker) {

        Domain domain = applicationContext.state.rgame.definition.domain.get(hero.domainId);
        Medium medium = applicationContext.state.rgame.definition.medium.get(hero.mediumId);


        for (Soldier soldier: attacker) {
            if (isHeroAttacker && soldier.asset == hero.asset) {
                continue;
            }

            if (soldier.domainId == hero.domainId) {
                soldier.power += domain.bonus;
            }

            if (soldier.mediumId == hero.mediumId) {
                soldier.power += medium.bonus;
            }
        }

        for (Soldier soldier: defender) {
            if (!isHeroAttacker && soldier.asset == hero.asset) {
                continue;
            }

            if (soldier.domainId == hero.domainId) {
                soldier.power += domain.bonus;
            }

            if (soldier.mediumId == hero.mediumId) {
                soldier.power += medium.bonus;
            }
        }
    }

    public void applyConceptHeroAura() {

        applyConceptHeroAura(attacker.get(attackerHeroIndex), true);
        applyConceptHeroAura(defender.get(defenderHeroIndex), false);
    }

    public void applyConceptDefenderDomain() {

        Arena arena = applicationContext.state.rgame.arena.getArena(arenaId);
        Domain domain = applicationContext.state.rgame.definition.domain.get(arena.domainId);

        for (Soldier soldier: defender) {
            if (soldier.domainId == domain.id) {
                soldier.power += domain.bonus;
            }
        }
    }

    public void applyConceptItemForBonus() {

        Soldier soldier = null;

        if (defender.size() > attackerItemForBonusSoldierIndex) {
            soldier = defender.get(attackerItemForBonusSoldierIndex);
        }

        if (soldier == null) {
            return;
        }

        ItemForBonus itemForBonus = new ItemForBonus();

        if (applicationContext.state.rgame.definition.itemForBonus.containsKey(attackerItemForBonusAssetId)) {
            itemForBonus = applicationContext.state.rgame.definition.itemForBonus.get(attackerItemForBonusAssetId);
        }

        soldier.power += itemForBonus.bonus;
    }

    private void applyConceptEffects() {

        applyConceptHeroAura();
        applyConceptDefenderDomain();
        applyConceptItemForBonus();
    }

    public boolean isComplete() {
        return isComplete;
    }

    public void calculateBattle(boolean isMonteCarlo) {

        if (!isMonteCarlo && isComplete()) {
            return;
        }

        if (!isMonteCarlo) {
            applyConceptEffects();
        }

        List<Soldier> attacker = new ArrayList<>(this.attacker);
        List<Soldier> defender = new ArrayList<>(this.defender);

        Random random = applicationContext.state.getCombinedRandom(id, economicCluster);
        int diceRollSize = applicationContext.state.rgame.arena.getDiceRollSize(arenaId);

        isDefenderWin = false;

        while(!isComplete) {
            int attackerValue = attacker.get(0).power;
            int defenderValue = defender.get(0).power;

            int attackerRoll = 1 + (int) (random.nextDouble() * diceRollSize);
            int defenderRoll = 1 + (int) (random.nextDouble() * diceRollSize);

            if (!isMonteCarlo) {
                listDiceRolls.add(attackerRoll);
                listDiceRolls.add(defenderRoll);
            }

            attackerValue += attackerRoll;
            defenderValue += defenderRoll;

            if (!isMonteCarlo) {
                applicationContext.logDebugMessage("attacker (" + attacker.size() + "): " + attackerValue);
                applicationContext.logDebugMessage("defender (" + defender.size() + "): " + defenderValue);
            }

            if (attackerValue <= defenderValue) {
                if (!isMonteCarlo) {
                    applicationContext.logDebugMessage("removed: attacker");
                }
                attacker.remove(0);
            }

            if (attackerValue >= defenderValue) {
                if (!isMonteCarlo) {
                    applicationContext.logDebugMessage("removed: defender");
                }
                defender.remove(0);
            }

            if (attacker.size() == 0) {
                isDefenderWin = true;
                isComplete = true;
            }

            if (defender.size() == 0) {
                isComplete = true;
            }
        }

        if (!isMonteCarlo) {
            int cardWinQuantity = applicationContext.state.rgame.arena.getItemLossCount(arenaId);
            List<Soldier> listDefeated;

            if (isDefenderWin) {
                listDefeated = new ArrayList<>(this.attacker);
            } else {
                listDefeated = new ArrayList<>(this.defender);
            }

            for (int i = 0; i < cardWinQuantity && listDefeated.size() > 0; i++) {
                int captureIndex = (int) (random.nextDouble() * listDefeated.size());
                tokensCaptured.mergeAssetToken(listDefeated.get(captureIndex).asset, 1, true);
            }

            for (Soldier soldier: listDefeated) {
                tokensRefunded.mergeAssetToken(soldier.asset, 1, true);
            }

            tokensRefunded.merge(tokensCaptured, false);
        }
    }

    public double monteCarlo(int count) {

        if (count == 0) {
            return 0;
        }

        int result = 0;

        for (int i = 0; i < count; i++) {
            id = i;
            isComplete = false;
            calculateBattle(true);

            if (!isDefenderWin) {
                result++;
            }
        }

        return ((double) result) / count;
    }

    private int getTotalValueOfListSoldiers(List<Soldier> list) {

        int result = 0;

        for(Soldier soldier: list) {
            result += soldier.power;
        }

        return result;
    }

    public int getTotalValueOfAttacker() {
        return getTotalValueOfListSoldiers(attacker);
    }

    public int getTotalValueOfDefender() {
        return getTotalValueOfListSoldiers(defender);
    }

    public boolean isWinnerLowerPower() {

        int attackerPower = getTotalValueOfAttacker();
        int defenderPower = getTotalValueOfDefender();

        return (isDefenderWin && attackerPower > defenderPower) || (attackerPower < defenderPower);
    }

    public boolean save() {

        Path pathDirectory = getBattleSaveDirectory(applicationContext, id);

        try {
            Files.createDirectories(pathDirectory);
        } catch (IOException e) {
            applicationContext.logErrorMessage("WARNING: could not create directories for save battle: " + pathDirectory);
            return false;
        }

        Path path = getBattleSaveName(applicationContext, id);

        FileOutputStream fileOutputStream;

        try {
            fileOutputStream = new FileOutputStream(path.toString());
            fileOutputStream.write(toJSONObject().toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            applicationContext.logErrorMessage("WARNING: could not save battle: " + path);
            applicationContext.logErrorMessage(e.toString());
            return false;
        }

        return true;
    }

    public static Battle loadBattle(ApplicationContext applicationContext, int id) {

        if (applicationContext == null || !applicationContext.isConfigured || id < 1) {
            return null;
        }

        Path path = getBattleSaveName(applicationContext, id);


        JSONObject jsonObject = FileUtility.jsonObjectReadFile(path.toString());

        if (jsonObject == null) {
            return null;
        }

        return new Battle(applicationContext, jsonObject);
    }

    public static Path getBattleSaveDirectory(ApplicationContext applicationContext, int id) {

        int z = 100;
        int a = id % z;
        int b = (id / z) % z;
        return Paths.get(applicationContext.stateRootDirectory.toString() + "/battle/" + a + "/" + b);
    }

    public static Path getBattleSaveName(ApplicationContext applicationContext, int id) {
        return Paths.get(getBattleSaveDirectory(applicationContext, id) + "/rgame.battle." + id + ".json");
    }

    public static JSONObject battleLoad(ApplicationContext applicationContext, int id) {

        Path path = getBattleSaveName(applicationContext, id);

        try {
            InputStream inputStream = Files.newInputStream(Paths.get(path.toString()));
            JSONParser jsonParser = new JSONParser();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); // Java 8

            byte[] buffer = new byte[1024];
            int length;

            while ((length = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }

            String inputString = byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());

            return (JSONObject) jsonParser.parse(inputString); // re-interpret to verify, only called once per block

        } catch (Exception e) {
            applicationContext.logErrorMessage("WARNING: could not load battle: " + path);
            applicationContext.logErrorMessage(e.toString());
            return null;
        }
    }

    public int getPower(List<Soldier> soldierList) {

        if (soldierList == null || soldierList.size() == 0) {
            return 0;
        }

        int result = 0;

        for (Soldier item: soldierList) {
            result += item.power;
        }

        return result;
    }

    public int getPowerDefender() {
        return getPower(defender);
    }

    public int getPowerAttacker() {
        return getPower(attacker);
    }
}

