package org.tarasca.mythicalbeings.rgame.omno.service.object;

import concept.utility.JsonFunction;
import org.json.simple.JSONObject;

public class Medium {
    public int id = -1;
    int bonus = 0;

    public Medium(JSONObject jsonObject) {
        define(jsonObject);
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();

        JsonFunction.put(jsonObject, "id", id);
        JsonFunction.put(jsonObject, "bonus", bonus);

        return  jsonObject;
    }

    public void define(JSONObject jsonObject) {
        id = JsonFunction.getInt(jsonObject, "id", -1);

        bonus = JsonFunction.getInt(jsonObject, "bonus", 0);
    }
}
