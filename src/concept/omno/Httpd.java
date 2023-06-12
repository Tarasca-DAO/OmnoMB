package concept.omno;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import concept.omno.object.Operation;
import concept.platform.Transaction;
import concept.utility.JsonFunction;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Httpd implements Runnable {
    final ApplicationContext applicationContext;

    String hostname;
    int port;

    boolean quit = false;

    Httpd(ApplicationContext applicationContext, String hostname, int port) {
        this.applicationContext = applicationContext;
        this.hostname = hostname;
        this.port = port;
    }

    public void stop() {
        quit = true;
    }

    public void run() {

        HttpServer server;

        try {
            server = HttpServer.create(new InetSocketAddress(hostname, port), 0);
        } catch (IOException e) {
            synchronized (applicationContext) {
                applicationContext.logErrorMessage(e.toString());
                applicationContext.logErrorMessage("HTTPD failed to start");
            }
            return;
        }

        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

        server.createContext("/", new HttpHandlerLocal());
        server.setExecutor(threadPoolExecutor);
        server.start();

        synchronized (applicationContext) {
            applicationContext.logInfoMessage("REST API server started: " + hostname + ":" + port);
            applicationContext.isHttpdRunning = true;
        }

        while (!quit) {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }

        synchronized (applicationContext) {
            applicationContext.isHttpdRunning = false;
        }

        server.stop(0);
    }

    private class HttpHandlerLocal implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            JSONObject requestParameter = new JSONObject();

            switch (httpExchange.getRequestMethod()) {
                default: {
                    break;
                }

                case "POST": {
                    applicationContext.logInfoMessage("POST not allowed. Use GET instead.");
                    // requestParameter = processPostRequest(httpExchange);
                    break;
                }

                case "GET": {
                    requestParameter = processGetRequest(httpExchange);
                    break;
                }

            }

            respond(httpExchange, requestParameter);
        }

        private JSONObject processGetRequest(HttpExchange httpExchange) {
            JSONObject response = new JSONObject();

            String requestString = null;

            try {
                String encoded = httpExchange.getRequestURI().toString();

                if (encoded == null || encoded.length() > 1024) {
                    return response;
                }

                requestString = URLDecoder.decode(encoded, "UTF-8");
            } catch (Exception e) {
                applicationContext.logErrorMessage(e.toString());
            }

            if (requestString == null || requestString.length() > 512) {
                return response;
            }

            int index = 0;

            while (index < requestString.length() && requestString.charAt(index) != '?') {
                index++;
            }

            index++;

            if ((index + 2) >= requestString.length()) {
                return response;
            }

            String subString = requestString.substring(index);

            JSONParser jsonParser = new JSONParser();

            try {
                response = (JSONObject) jsonParser.parse(subString);
            } catch (Exception e) {
                return response;
            }

            return response;
        }

        private void respond(HttpExchange httpExchange, JSONObject requestParameter) throws IOException {

            JSONObject responseJson = processRequest(requestParameter);
            String responseString = responseJson.toJSONString();

            // Allow CORS request
            httpExchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            httpExchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            httpExchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            // Set the response content type to JSON
            httpExchange.getResponseHeaders().set("Content-Type", "application/json");

            byte[] responseBytes = responseString.getBytes();
            httpExchange.sendResponseHeaders(200, responseBytes.length);

            OutputStream outputStream = httpExchange.getResponseBody();
            outputStream.write(responseBytes);
            outputStream.flush();
            outputStream.close();
        }

        private JSONObject getTradeState() {

            JSONObject copyState = applicationContext.state.platformTokenExchangeById.toJSONObject();

            // Check for unconfirmed transactions
            List<Transaction> unconfirmedTxs = applicationContext.ardorApi.getUnconfirmedTransactions(2,
                    applicationContext.contractAccountId);

            if (unconfirmedTxs == null || unconfirmedTxs.isEmpty()) {
                return copyState;
            }

            Operations operations;

            // Iterate through the unconfirmed transactions
            // If the transaction is a token exchange, update the state
            for (int i = unconfirmedTxs.size() - 1; i >= 0; i--) {
                Transaction transaction = unconfirmedTxs.get(i);
                JSONObject message = transaction.messageJson;

                if (message != null) {
                    operations = new Operations(applicationContext.contractName);
                    operations.addOperations(transaction);
                    List<Operation> listOperation = operations.getOperationList();

                    if (listOperation != null && !listOperation.isEmpty()) {
                        for (Operation operation : listOperation) {
                            String operationService = operation.service;
                            String operationRequest = operation.request;

                            if (operationService.equals("trade") && operationRequest.equals("accept")) {
                                JSONObject operationParameters = operation.parameterJson;
                                String id = operationParameters.get("id").toString();
                                JSONArray originalOffers = (JSONArray) copyState.get("offer");
                                JSONArray offers = (JSONArray) copyState.get("offer");

                                if (originalOffers != null) {
                                    for (int k = originalOffers.size() - 1; k >= 0; k--) {
                                        JSONObject offer = (JSONObject) originalOffers.get(k);

                                        if (offer.get("id").toString().equals(id)) {
                                            long multiplier = Long.parseLong(offer.get("multiplier").toString());
                                            long transactionMultiplier = Long
                                                    .parseLong(operationParameters.get("multiplier").toString());
                                            multiplier -= transactionMultiplier;

                                            if (multiplier <= 0) {
                                                offers.remove(k);
                                                applicationContext.logDebugMessage("Offer deleted -> " + id);
                                            } else {
                                                // Convert multiplier to string
                                                String multiplierString = Long.toString(multiplier);
                                                JsonFunction.put(offer, "multiplier", multiplierString);
                                                applicationContext
                                                        .logDebugMessage("Offer updated -> " + offer.toJSONString());
                                            }
                                        }
                                    }
                                    JsonFunction.put(copyState, "offer", offers);
                                }
                            }
                        }
                    }
                }
            }
            return copyState;
        }

        private JSONObject processRequest(JSONObject jsonObject) {
            JSONObject response = new JSONObject();

            synchronized (applicationContext) {
                if (!applicationContext.isConfigured) {
                    JsonFunction.put(response, "error", "Application not yet configured");
                    return response;
                }

                String apiPassword = JsonFunction.getString(jsonObject, "password", "");

                if (apiPassword == null || !apiPassword.equals(applicationContext.apiPassword)) {
                    JsonFunction.put(response, "error", "Incorrect API password attempt");
                    return response;
                }

                String service = JsonFunction.getString(jsonObject, "service", null);

                if (service == null) {
                    JsonFunction.put(response, "error", "Service not specified");
                    return response;
                }

                String request = JsonFunction.getString(jsonObject, "request", null);

                JSONObject parameter = JsonFunction.getJSONObject(jsonObject, "parameter", null);

                if (request == null) {
                    JsonFunction.put(response, "error", "Request not specified");
                    return response;
                }
                applicationContext.logDebugMessage(
                        "-----------------------------------------------------------------------------");
                applicationContext.logDebugMessage("API CALL | Service: " + service + " | Request: " + request);

                switch (service) {
                    default: {
                        JsonFunction.put(response, "error", "Service not found: " + service);
                        return response;
                    }

                    case "platform": {
                        switch (request) {
                            default: {
                                JsonFunction.put(response, "error", "Request not found: " + request);
                                return response;
                            }

                            case "state": {
                                JSONObject stateObject = applicationContext.state.apiProcessRequestPlatform(parameter);

                                if (stateObject != null && !stateObject.containsKey("error")) {
                                    applicationContext.signJSONObject(response, "state", stateObject);
                                } else {
                                    response = stateObject;
                                }

                                break;
                            }
                        }

                        break;
                    }

                    case "user": {
                        switch (request) {
                            default: {
                                JsonFunction.put(response, "error", "Request not found: " + request);
                                return response;
                            }

                            case "state": {
                                applicationContext.signJSONObject(response, "state",
                                        applicationContext.state.userAccountState.toJSONObject());
                                break;
                            }
                        }

                        break;
                    }

                    case "nativeAsset": {
                        switch (request) {
                            default: {
                                JsonFunction.put(response, "error", "Request not found: " + request);
                                return response;
                            }

                            case "state": {
                                applicationContext.signJSONObject(response, "state",
                                        applicationContext.state.nativeAssetState.toJSONObject());
                                break;
                            }
                        }

                        break;
                    }

                    case "trade": {
                        switch (request) {
                            default: {
                                JsonFunction.put(response, "error", "Request not found: " + request);
                                return response;
                            }

                            case "state": {
                                JSONObject tradeState = getTradeState();
                                applicationContext.signJSONObject(response, "state", tradeState);
                                return response;
                            }
                        }
                    }

                    case "platformSwap": {
                        switch (request) {
                            default: {
                                JsonFunction.put(response, "error", "Request not found: " + request);
                                return response;
                            }

                            case "state": {
                                applicationContext.signJSONObject(response, "state",
                                        applicationContext.state.platformSwap.state.toJSONObject());
                                break;
                            }

                            case "platformState": {
                                JSONObject stateObject = applicationContext.state.platformSwap.state
                                        .apiProcessRequestPlatform(parameter);

                                if (stateObject != null && !stateObject.containsKey("error")) {
                                    applicationContext.signJSONObject(response, "platform", stateObject);
                                } else {
                                    response = stateObject;
                                }

                                break;
                            }
                        }

                        break;
                    }

                    case "voting": {
                        switch (request) {
                            default: {
                                JsonFunction.put(response, "error", "Request not found: " + request);
                                return response;
                            }

                            case "state": {
                                applicationContext.signJSONObject(response, "state",
                                        applicationContext.state.voting.state.toJSONObject());
                                break;
                            }

                            case "poll": {
                                JSONObject stateObject = applicationContext.state.voting.state
                                        .apiProcessRequestPoll(parameter);

                                if (stateObject != null && !stateObject.containsKey("error")) {
                                    applicationContext.signJSONObject(response, "poll", stateObject);
                                } else {
                                    response = stateObject;
                                }

                                break;
                            }

                            case "allowList": {
                                JSONObject stateObject = applicationContext.state.voting.state
                                        .apiProcessRequestAllowList(parameter);

                                if (stateObject != null && !stateObject.containsKey("error")) {
                                    applicationContext.signJSONObject(response, "allowList", stateObject);
                                } else {
                                    response = stateObject;
                                }

                                break;
                            }
                        }

                        break;
                    }

                    case "collateralizedSwap": {
                        switch (request) {
                            default: {
                                JsonFunction.put(response, "error", "Request not found: " + request);
                                return response;
                            }

                            case "state": {
                                applicationContext.signJSONObject(response, "state",
                                        applicationContext.state.collateralizedSwap.state.toJSONObject());
                                break;
                            }

                            case "instanceState": {
                                JSONObject stateObject = applicationContext.state.collateralizedSwap.state
                                        .apiProcessRequestInstance(parameter);

                                if (stateObject != null && !stateObject.containsKey("error")) {
                                    applicationContext.signJSONObject(response, "instance", stateObject);
                                } else {
                                    response = stateObject;
                                }

                                break;
                            }
                        }

                        break;
                    }

                    case "rgame": {
                        switch (request) {
                            default: {
                                JsonFunction.put(response, "error", "Request not found: " + request);
                                return response;
                            }

                            case "state": {
                                applicationContext.signJSONObject(response, "state",
                                        applicationContext.state.rgame.toJSONObject());
                                break;
                            }

                            case "battle": {
                                JSONObject stateObject = applicationContext.state.rgame
                                        .apiProcessBattleJsonRequest(parameter);

                                if (stateObject != null && !stateObject.containsKey("error")) {
                                    applicationContext.signJSONObject(response, "battle", stateObject);
                                } else {
                                    response = stateObject;
                                }

                                break;
                            }
                        }

                        break;
                    }
                }
            }

            return response;
        }
    }
}
