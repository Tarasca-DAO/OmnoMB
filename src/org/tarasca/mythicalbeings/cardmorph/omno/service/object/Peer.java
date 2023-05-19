package org.tarasca.mythicalbeings.cardmorph.omno.service.object;

import concept.omno.object.PlatformToken;
import concept.utility.JsonFunction;
import org.json.simple.JSONObject;

public class Peer {
    public int rank;

    public double chanceDuplicateIfNotMorphed;
    public PlatformToken cost = new PlatformToken();
    public PlatformToken peer = new PlatformToken();

    public Peer(JSONObject jsonObject) {
        define(jsonObject);
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();

        JsonFunction.put(jsonObject, "rank", rank);
        JsonFunction.put(jsonObject, "chanceDuplicateIfNotMorphed", Double.toString(chanceDuplicateIfNotMorphed));

        if (cost != null) {
            JsonFunction.put(jsonObject, "cost", cost.toJSONObject());
        }

        if (peer != null) {
            JsonFunction.put(jsonObject, "peer", peer.toJSONObject());
        }

        return  jsonObject;
    }

    public void define(JSONObject jsonObject) {
        rank = JsonFunction.getInt(jsonObject, "rank", rank);
        chanceDuplicateIfNotMorphed = JsonFunction.getDoubleFromString(jsonObject, "chanceDuplicateIfNotMorphed", 0.0);
        cost = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "cost", null));
        peer = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "peer", null));
    }
}
