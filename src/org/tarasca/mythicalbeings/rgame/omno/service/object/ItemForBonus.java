package org.tarasca.mythicalbeings.rgame.omno.service.object;

import concept.utility.JsonFunction;
import org.json.simple.JSONObject;

public class ItemForBonus {
    public long asset = 0;
    int bonus = 0;

    ItemForBonus() {}

    public ItemForBonus(JSONObject jsonObject) {
        define(jsonObject);
    }

    public boolean isValid() {
        return asset != 0;
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();

        JsonFunction.put(jsonObject, "asset", Long.toUnsignedString(asset));
        JsonFunction.put(jsonObject, "bonus", bonus);

        return  jsonObject;
    }

    public void define(JSONObject jsonObject) {
        if (jsonObject == null) {
            return;
        }

        asset = JsonFunction.getLongFromStringUnsigned(jsonObject, "asset", 0);
        bonus = JsonFunction.getInt(jsonObject, "bonus", 1);
    }
}
