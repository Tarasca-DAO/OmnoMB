package org.tarasca.mythicalbeings.rgame.omno.service;

import concept.utility.JsonFunction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.tarasca.mythicalbeings.rgame.omno.service.object.Soldier;

import java.util.HashMap;

public class SoldierState {
    HashMap<Long, Soldier> soldierDefinition = new HashMap<>();

    SoldierState() {}

    SoldierState(JSONObject jsonObject, boolean validate) {
        define(jsonObject, validate);
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();

        JSONArray jsonArray = new JSONArray();

        for (Soldier value: soldierDefinition.values()) {
            JsonFunction.add(jsonArray, value.toJSONObject());
        }

        JsonFunction.put(jsonObject, "soldier", jsonArray);

        return  jsonObject;
    }

    public void define(JSONObject jsonObject, boolean validate) {

        if (jsonObject == null) {
            return;
        }

        defineSoldier(JsonFunction.getJSONArray(jsonObject, "soldier", null), validate);
    }

    public void defineSoldier(JSONArray jsonArray, boolean validate) {

        if (jsonArray == null) {
            return;
        }

        for (Object o: jsonArray) {
            defineSoldier((JSONObject) o, validate);
        }
    }

    public void defineSoldier(JSONObject json, boolean validate) {

        if (json == null) {
            return;
        }

        Soldier soldier = new Soldier();

        soldier.define(json);

        if (validate && !soldier.isValid()) {
            return;
        }

        soldierDefinition.put(soldier.asset, soldier);
    }

    public Soldier getSoldierByAsset(long asset) {
        Soldier soldier = null;

        if (soldierDefinition.containsKey(asset)) {
            soldier = soldierDefinition.get(asset);
        }

        return soldier;
    }

    public int getSoldierRank(long asset) {

        Soldier soldier;

        if (soldierDefinition.containsKey(asset)) {
            soldier = soldierDefinition.get(asset);
        } else {
            throw new NullPointerException();
        }

        return soldier.rank;
    }
}
