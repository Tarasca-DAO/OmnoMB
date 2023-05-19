package org.tarasca.mythicalbeings.rgame.omno.service;

import concept.utility.JsonFunction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.tarasca.mythicalbeings.rgame.omno.service.object.Domain;
import org.tarasca.mythicalbeings.rgame.omno.service.object.ItemForBonus;
import org.tarasca.mythicalbeings.rgame.omno.service.object.Medium;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Definition {
    public SoldierState soldier = new SoldierState();
    public HashMap<Integer, Domain> domain = new HashMap<>();
    public HashMap<Integer, Medium> medium = new HashMap<>();
    public HashMap<Long, ItemForBonus> itemForBonus = new HashMap<>();

    Definition() {}

    Definition(JSONObject jsonObject) {
        defineWithoutValidation(jsonObject);
    }

    public void defineWithoutValidation(JSONObject jsonObject) {

        if (jsonObject == null) {
            return;
        }

        JSONObject soldierObject = JsonFunction.getJSONObject(jsonObject, "soldier", null);

        soldier = new SoldierState(soldierObject, false);

        List<JSONObject> list = JsonFunction.getListJSONObject(jsonObject, "domain", null);

        if (list != null) {
            domain = new HashMap<>();

            for(JSONObject object: list) {
                Domain item = new Domain(object);

                if (item.id >= 0) {
                    domain.put(item.id, item);
                }
            }
        }

        list = JsonFunction.getListJSONObject(jsonObject, "medium", null);

        if (list != null) {
            medium = new HashMap<>();

            for(JSONObject object: list) {
                Medium item = new Medium(object);

                if (item.id >= 0) {
                    medium.put(item.id, item);
                }
            }
        }

        list = JsonFunction.getListJSONObject(jsonObject, "itemForBonus", null);

        if (list != null) {
            itemForBonus = new HashMap<>();

            for(JSONObject object: list) {
                ItemForBonus item = new ItemForBonus(object);

                if (item.asset != 0) {
                    itemForBonus.put(item.asset, item);
                }
            }
        }
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();

        JsonFunction.put(jsonObject, "soldier", soldier.toJSONObject());

        JSONArray domainJsonArray = new JSONArray();
        List<Domain> listDomain = new ArrayList<>(domain.values());

        for (Domain value: listDomain) {
            JsonFunction.add(domainJsonArray, value.toJSONObject());
        }

        JsonFunction.put(jsonObject, "domain", domainJsonArray);


        JSONArray mediumJsonArray = new JSONArray();
        List<Medium> listMedium = new ArrayList<>(medium.values());

        for (Medium value: listMedium) {
            JsonFunction.add(mediumJsonArray, value.toJSONObject());
        }

        JsonFunction.put(jsonObject, "medium", mediumJsonArray);


        JSONArray itemJsonArray = new JSONArray();
        List<ItemForBonus> listItems = new ArrayList<>(itemForBonus.values());

        for (ItemForBonus value: listItems) {
            JsonFunction.add(itemJsonArray, value.toJSONObject());
        }

        JsonFunction.put(jsonObject, "itemForBonus", itemJsonArray);

        return  jsonObject;
    }
}
