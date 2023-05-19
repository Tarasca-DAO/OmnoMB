package concept.omno;

import concept.utility.JsonFunction;
import concept.utility.RestApi;
import org.json.simple.JSONObject;
import org.tarasca.mythicalbeings.rgame.omno.service.object.Battle;

public class RemoteApi extends RestApi {

    final ApplicationContext applicationContext;

    String password;

    public RemoteApi(ApplicationContext applicationContext, String hostProtocolString, String hostNameString, String hostPortString, String password) {
        super(hostProtocolString, hostNameString, hostPortString, "api");

        this.applicationContext = applicationContext;

        this.password = password;
    }

    public State getState() {
        JSONObject jsonObject = new JSONObject();

        if (password != null) {
            JsonFunction.put(jsonObject, "password", password);
        }

        JsonFunction.put(jsonObject, "service", "platform");
        JsonFunction.put(jsonObject, "request", "state");

        JSONObject response;

        try {
            response = jsonObjectHttpApiRaw(false, jsonObject);
        } catch (Exception e) {
            return null;
        }

        return new State(applicationContext, JsonFunction.getJSONObject(response, "state", null));
    }

    public Battle getBattle(int id) {

        if (id < 1) {
            return null;
        }

        JSONObject jsonObject = new JSONObject();

        if (password != null) {
            JsonFunction.put(jsonObject, "password", password);
        }

        JsonFunction.put(jsonObject, "service", "rgame");
        JsonFunction.put(jsonObject, "request", "battle");

        JSONObject parameter = new JSONObject();
        JsonFunction.put(parameter, "id", id);
        JsonFunction.put(jsonObject, "parameter", parameter);

        JSONObject response;

        try {
            response = jsonObjectHttpApiRaw(false, jsonObject);
        } catch (Exception e) {
            return null;
        }

        return new Battle(applicationContext, JsonFunction.getJSONObject(response, "battle", null));
    }
}
