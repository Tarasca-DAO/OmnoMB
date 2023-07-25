package org.tarasca.mythicalbeings.cardcraft.omno.service;

import concept.utility.JsonFunction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.tarasca.mythicalbeings.cardcraft.omno.service.object.Peer;

import java.util.HashMap;
import java.util.List;

public class Definition {

    HashMap<Integer, Peer> peers = null;

    Definition() {
    }

    Definition(JSONObject jsonObject) {
        defineWithoutValidation(jsonObject);
    }

    public void defineWithoutValidation(JSONObject jsonObject) {

        if (jsonObject == null) {
            return;
        }

        peers = new HashMap<>();

        List<JSONObject> list = JsonFunction.getListJSONObject(jsonObject, "rank", null);

        if (list != null && !list.isEmpty()) {
            for (JSONObject object : list) {
                Peer peer = new Peer(object);
                peers.put(peer.rank, peer);
            }
        }
    }

    public JSONObject toJSONObject() {

        JSONObject jsonObject = new JSONObject();

        if (peers != null && !peers.isEmpty()) {

            JSONArray jsonArray = new JSONArray();

            for (int key : peers.keySet()) {
                Peer peer = peers.get(key);
                JsonFunction.add(jsonArray, peer.toJSONObject());
            }

            JsonFunction.put(jsonObject, "rank", jsonArray);
        }

        return jsonObject;
    }
}
