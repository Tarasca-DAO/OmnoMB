package org.tarasca.mythicalbeings.rgame.omno.service.object;

import concept.utility.JsonFunction;
import org.json.simple.JSONObject;

public class Soldier implements Cloneable {
    public long asset = 0;
    public int power = -1;
    public int rank = -1;
    public int domainId = -1;
    public int mediumId = -1;
    int arenaId = -1;

    public boolean isValid() {
        return asset != 0 && domainId >= 0 && mediumId >= 0 && arenaId >= 0 && power >= 0 && rank >= 0;
    }

    public void define(JSONObject jsonObject) {

        if (jsonObject == null) {
            return;
        }

        asset = JsonFunction.getLongFromStringUnsigned(jsonObject, "asset", 0);
        domainId = JsonFunction.getInt(jsonObject, "domainId", -1);
        mediumId = JsonFunction.getInt(jsonObject, "mediumId", -1);
        arenaId = JsonFunction.getInt(jsonObject, "arenaId", -1);

        power = JsonFunction.getInt(jsonObject, "power", 1);
        rank = JsonFunction.getInt(jsonObject, "rank", 1);
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();

        JsonFunction.put(jsonObject, "asset", Long.toUnsignedString(asset));
        JsonFunction.put(jsonObject, "domainId", domainId);
        JsonFunction.put(jsonObject, "mediumId", mediumId);
        JsonFunction.put(jsonObject, "arenaId", arenaId);

        JsonFunction.put(jsonObject, "power", power);
        JsonFunction.put(jsonObject, "rank", rank);

        return  jsonObject;
    }

    public Soldier clone() {
        final Soldier clone;

        try {
            clone = (Soldier) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException();
        }

        clone.asset = this.asset;
        clone.power = this.power;
        clone.rank = this.rank;
        clone.domainId = this.domainId;
        clone.mediumId = this.mediumId;
        clone.arenaId = this.arenaId;

        return clone;
    }
}
