package concept.utility;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;

public class RestApi {
    public String hostProtocolString;
    public String hostNameString;
    public String hostPortString;
    private String serviceString;

    public RestApi(String hostProtocolString, String hostNameString, String hostPortString, String serviceString) {
        this.hostProtocolString = hostProtocolString;
        this.hostNameString = hostNameString;
        this.hostPortString = hostPortString;
        this.serviceString = serviceString;
    }

    public JSONObject jsonObjectHttpApi(boolean methodIsPost, HashMap<String, String> parameters) throws IOException {
        StringBuilder urlStringBuilder = new StringBuilder();

        urlStringBuilder.append(hostProtocolString).append("://").append(hostNameString).append(":").append(hostPortString).append("/");

        if (serviceString != null) {
            urlStringBuilder.append(serviceString);
        }

        HttpURLConnection connection;

        if (methodIsPost) {
            URL url = new URL(urlStringBuilder.toString());

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setReadTimeout(60000);

            StringBuilder stringBuilder = new StringBuilder();

            if (parameters != null) {
                for (String parameterObject: parameters.keySet()) {
                    stringBuilder.append("&").append(parameterObject).append("=").append(parameters.get(parameterObject));
                }
            }

            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setUseCaches(false);
            connection.setDoOutput(true);

            DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());

            dataOutputStream.writeBytes(stringBuilder.toString());
        }
        else {
            boolean first = true;

            if (parameters != null) {
                for (String parameterObject: parameters.keySet()) {

                    if (first) {
                        urlStringBuilder.append("?");
                        first = false;
                    } else {
                        urlStringBuilder.append("&");
                    }

                    urlStringBuilder.append(parameterObject).append("=").append(parameters.get(parameterObject));
                }
            }

            URL url = new URL(urlStringBuilder.toString());

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(60000);

            connection.connect();
        }

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder stringBuilder = new StringBuilder();

        String line = bufferedReader.readLine();

        while (line != null)
        {
            stringBuilder.append(line).append("\n");
            line = bufferedReader.readLine();
        }

        return (JSONObject) JSONValue.parse(stringBuilder.toString());
    }

    public JSONObject jsonObjectHttpApiRaw(boolean methodIsPost, JSONObject jsonObject) throws IOException {
        StringBuilder urlStringBuilder = new StringBuilder();

        urlStringBuilder.append(hostProtocolString).append("://").append(hostNameString).append(":").append(hostPortString).append("/");

        if (serviceString != null) {
            urlStringBuilder.append(serviceString);
        }

        HttpURLConnection connection;

        if (methodIsPost) {
            URL url = new URL(urlStringBuilder.toString());

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setReadTimeout(60000);

            StringBuilder stringBuilder = new StringBuilder();

            if (jsonObject != null) {
                stringBuilder.append(URLEncoder.encode(jsonObject.toJSONString(), "UTF-8"));
            }

            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setUseCaches(false);
            connection.setDoOutput(true);

            DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());

            dataOutputStream.writeBytes(stringBuilder.toString());
        }
        else {

            if (jsonObject != null) {
                urlStringBuilder.append("?");
                urlStringBuilder.append(URLEncoder.encode(jsonObject.toJSONString(), "UTF-8"));
            }

            URL url = new URL(urlStringBuilder.toString());

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(6000);

            connection.connect();
        }

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder stringBuilder = new StringBuilder();

        String line = bufferedReader.readLine();

        while (line != null)
        {
            stringBuilder.append(line).append("\n");
            line = bufferedReader.readLine();
        }

        return (JSONObject) JSONValue.parse(stringBuilder.toString());
    }

    public void setHostProtocolString(String hostProtocolString) {
        this.hostProtocolString = hostProtocolString;
    }

    public void setHostNameString(String hostNameString) {
        this.hostNameString = hostNameString;
    }

    public void setHostPortString(String hostPortString) {
        this.hostPortString = hostPortString;
    }
}
