package concept.omno;

import concept.omno.object.Operation;
import concept.platform.Transaction;
import concept.utility.JsonFunction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.ArrayList;
import java.util.List;

public class Operations {
    String contractName;
    List<Operation> listOperation = new ArrayList<>();

    Operations(String contractName) {
        this.contractName = contractName;
    }

    public List<Operation> getOperationList() {
        return listOperation;
    }

    public boolean addOperation(Operation operation) {
        boolean isValid = false;

        if (operation != null) {
            isValid = true;
            listOperation.add(operation);
        }

        return isValid;
    }

    public boolean addOperations(Transaction transaction) {

        if (transaction.messageJson == null) {
            return false;
        }

        return addOperations(transaction.sender, transaction.messageJson);

    }

    public boolean addOperations(long account, JSONObject json) {

        boolean isValid = false;

        if (json == null) {
            return false;
        }

        if (contractName != null) {
            if (!JsonFunction.getString(json, "contract", "").equals(contractName)) {
                return false;
            }
        }

        if (!json.containsKey("operation")) {
            return false;
        }

        JSONArray arrayOperation = (JSONArray) json.get("operation");

        for (Object o: arrayOperation) {
            JSONObject operationJson = (JSONObject) o;

            if (!operationJson.containsKey("service") || !operationJson.containsKey("request")) {
                continue;
            }

            JSONObject parameter = JsonFunction.getJSONObject(operationJson, "parameter", null);
            String service = JsonFunction.getString(operationJson, "service", null);
            String request = JsonFunction.getString(operationJson, "request", null);

            Operation operation = new Operation(account, service, request, parameter);

            if (operation.isValid()) {
                listOperation.add(operation);
            }
        }

        if (listOperation.size() != 0) {
            isValid = true;
        }

        return isValid;
    }

    public boolean addOperations(long account, String jsonString) {

        if (jsonString == null) {
            return false;
        }

        JSONParser jsonParser = new JSONParser();
        JSONObject json;

        try {
            json = (JSONObject) jsonParser.parse(jsonString);
        } catch (org.json.simple.parser.ParseException e) {
            return false;
        }

        return addOperations(account, json);
    }

    public static List<Operation> createListOfOperations(long account, JSONArray jsonArray) {
        if (account == 0 || jsonArray == null || jsonArray.size() == 0) {
            return null;
        }

        List<Operation> result = new ArrayList<>();

        for (Object object: jsonArray) {
            if (! (object instanceof JSONObject)) {
                continue;
            }

            JSONObject jsonObject = (JSONObject) object;

            String service = JsonFunction.getString(jsonObject, "service", null);
            String request = JsonFunction.getString(jsonObject, "request", null);
            JSONObject parameter = JsonFunction.getJSONObject(jsonObject, "parameter", null);

            Operation operation = new Operation(account, service, request, parameter);

            if (operation.isValid()) {
                result.add(operation);
            }
        }

        return result;
    }
}
