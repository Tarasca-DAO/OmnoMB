package concept.omno.object;

import org.json.simple.JSONObject;

public class Operation {

    public long account;
    public String service;
    public String request;
    public JSONObject parameterJson;

    public Operation(long account, String service, String request, JSONObject parameterJson) {

        this.account = account;
        this.service = service;
        this.request = request;

        /*
            WARNING: This may be from untrusted source and needs to be hardened against malformed user-defined input
         */

        this.parameterJson = parameterJson;
        //
    }

    public boolean isValid() {
        return (account != 0 && service != null && !service.equals("") && request != null && !request.equals(""));
    }
}
