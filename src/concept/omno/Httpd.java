package concept.omno;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import concept.utility.JsonFunction;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
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
                applicationContext.logErrorMessage("Omno | HTTPD failed to start");
            }
            return;
        }

        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

        server.createContext("/api", new HttpHandlerLocal());
        server.setExecutor(threadPoolExecutor);
        server.start();

        synchronized (applicationContext) {
            applicationContext.logInfoMessage("Omno | REST API server started: " + hostname + ":" + port + "/api");
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
                    applicationContext.logInfoMessage("Omno | POST not allowed. Use GET instead.");
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

            httpExchange.sendResponseHeaders(200, responseString.length());
            OutputStream outputStream = httpExchange.getResponseBody();
            outputStream.write(responseString.getBytes());
            outputStream.flush();
            outputStream.close();
        }

        private JSONObject processRequest(JSONObject jsonObject) {
            JSONObject response = new JSONObject();

            synchronized (applicationContext) {
                if (!applicationContext.isConfigured) {
                    JsonFunction.put(response, "error", "Omno | Application not yet configured");
                    return response;
                }

                String apiPassword = JsonFunction.getString(jsonObject, "password", "");

                if (apiPassword == null || !apiPassword.equals(applicationContext.apiPassword)) {
                    JsonFunction.put(response, "error", "Omno | Incorrect API password attempt");
                    return response;
                }

                String service = JsonFunction.getString(jsonObject, "service", null);

                if (service == null) {
                    JsonFunction.put(response, "error", "Omno | Service not specified");
                    return response;
                }

                String request = JsonFunction.getString(jsonObject, "request", null);

                JSONObject parameter = JsonFunction.getJSONObject(jsonObject, "parameter", null);

                if (request == null) {
                    JsonFunction.put(response, "error", "Omno | Request not specified");
                    return response;
                }

                switch (service) {
                    default: {
                        JsonFunction.put(response, "error", "Omno | Service not found: " + service);
                        return response;
                    }

                    case "platform": {
                        switch (request) {
                            default: {
                                JsonFunction.put(response, "error", "Omno | Request not found: " + request);
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
                                JsonFunction.put(response, "error", "Omno | Request not found: " + request);
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
                                JsonFunction.put(response, "error", "Omno | Request not found: " + request);
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
                                JsonFunction.put(response, "error", "Omno | Request not found: " + request);
                                return response;
                            }

                            case "state": {
                                applicationContext.signJSONObject(response, "state",
                                        applicationContext.state.platformTokenExchangeById.toJSONObject());
                                break;
                            }
                        }

                        break;
                    }

                    case "platformSwap": {
                        switch (request) {
                            default: {
                                JsonFunction.put(response, "error", "Omno | Request not found: " + request);
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
                                JsonFunction.put(response, "error", "Omno | Request not found: " + request);
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
                                JsonFunction.put(response, "error", "Omno | Request not found: " + request);
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
                                JsonFunction.put(response, "error", "Omno | Request not found: " + request);
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
