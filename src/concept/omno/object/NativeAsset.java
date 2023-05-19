package concept.omno.object;

import concept.utility.JsonFunction;
import org.json.simple.JSONObject;

public class NativeAsset {
    public long id;
    public long quantity;
    public long quantityMaximum;
    public long account;
    public long royaltyAccount;
    public double royaltyFraction;
    public boolean royaltyIsTradeOnly = true;

    public String name;
    public String description;

    public void define(JSONObject jsonObject) {
        if (jsonObject == null) {
            return;
        }

        id = JsonFunction.getLongFromStringUnsigned(jsonObject, "id", -1);
        quantity = JsonFunction.getLongFromStringUnsigned(jsonObject, "quantity", 0);
        quantityMaximum = JsonFunction.getLongFromStringUnsigned(jsonObject, "quantityMaximum", 0);
        account = JsonFunction.getLongFromStringUnsigned(jsonObject, "account", 0);

        royaltyFraction = JsonFunction.getDoubleFromString(jsonObject, "royaltyFraction", 0);
        royaltyAccount = JsonFunction.getLongFromStringUnsigned(jsonObject, "royaltyAccount", 0);
        royaltyIsTradeOnly = JsonFunction.getBoolean(jsonObject, "royaltyIsTradeOnly", true);

        name = JsonFunction.getString(jsonObject, "name", "");
        description = JsonFunction.getString(jsonObject, "description", "");
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();

        JsonFunction.put(jsonObject, "id", Long.toUnsignedString(id));
        JsonFunction.put(jsonObject, "quantity", Long.toUnsignedString(quantity));
        JsonFunction.put(jsonObject, "quantityMaximum", Long.toUnsignedString(quantityMaximum));
        JsonFunction.put(jsonObject, "account", Long.toUnsignedString(account));

        if (royaltyFraction > 0) {
            JsonFunction.put(jsonObject, "royaltyAccount", Long.toUnsignedString(royaltyAccount));
            JsonFunction.put(jsonObject, "royaltyFraction", Double.toString(royaltyFraction));
            JsonFunction.put(jsonObject, "royaltyIsTradeOnly", royaltyIsTradeOnly);
        }

        JsonFunction.put(jsonObject, "name", name);
        JsonFunction.put(jsonObject, "description", description);

        return  jsonObject;
    }

    public NativeAsset() {}

    public NativeAsset(JSONObject jsonObject) {
        define(jsonObject);
    }

    public boolean isValid() {
        return name != null && name.length() <= 10 && name.length() >= 3 && description != null && description.length() <= 1000 && description.length() > 0 && quantity >= 0 && quantity <= quantityMaximum && (royaltyFraction >= 0.0F) && (royaltyFraction <= 100F) && !(royaltyFraction != 0 && royaltyAccount == 0);
    }

}
