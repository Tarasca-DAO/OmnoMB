package concept.omno;

import concept.platform.ArdorApi;
import concept.platform.EconomicCluster;
import concept.platform.Transaction;
import concept.utility.JsonFunction;
import concept.utility.NxtCryptography;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class ApplicationContext implements Runnable {
    public boolean isConfigured = false;

    UnconfirmedTransactionCache unconfirmedTransactionCache = new UnconfirmedTransactionCache(this);

    public PlatformContext platformContext = new PlatformContext(this);

    public State state;

    public ArdorApi ardorApi;
    public NxtCryptography nxtCryptography;
    public long contractAccountId = 0;

    String adminPasswordString = null;
    public String contractName = "";
    String stateName;

    public boolean isVerifier = false;
    long verifyAccount = 0;
    byte[] secretForRandom = null;

    String apiPassword;
    String apiHost;
    int apiPort;

    public RemoteApi remoteApi;

    boolean isHttpdRunning = false;

    public Path stateRootDirectory = null;
    boolean reloadAndRescan = false;

    public int transactionChain = 2;

    int heightStart = 2090000;
    int heightStop = -1;

    int logLevel = 9;

    private boolean quit = false;
    private boolean quitComplete = false;

    public void stop() {
        quit = true;
    }

    public void run() {

        while (!quit) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException ignored) {
            }

            synchronized (this) {
                platformContext.update();
            }

            processBlock();
        }

        quitComplete = true;
    }

    public void quit() {
        quit = true;
    }

    public boolean waitStopComplete() {

        if (quit) {

            while (!quitComplete) {
                try {
                    TimeUnit.MILLISECONDS.sleep(50);
                } catch (InterruptedException ignored) {
                }
            }
        }

        return quit;
    }

    public ApplicationContext(JSONObject jsonObject) {
        configurationRead(jsonObject, true);
        state = new State(this);
    }

    ApplicationContext(String pathString) throws IOException, ParseException {
        configurationRead(pathString);
        state = new State(this);
    }

    public boolean loadRemoteState() {

        if (remoteApi == null) {
            return false;
        }

        State state = remoteApi.getState();

        if (state == null || !state.isValid()) {
            return false;
        }

        this.state = state;
        return true;
    }

    public enum LogLevel {
        DEBUG(9, "DEBUG"),
        INFO(2, "INFO"),
        ERROR(1, "ERROR");

        private int level;
        private String label;

        private LogLevel(int level, String label) {
            this.level = level;
            this.label = label;
        }

        public int getLevel() {
            return level;
        }

        public String getLabel() {
            return label;
        }
    }

    public void logDebugMessage(String message) {
        logLevelMessage(LogLevel.DEBUG, message, false);
    }

    public void logInfoMessage(String message) {
        logLevelMessage(LogLevel.INFO, message, false);
    }

    public void logErrorMessage(String message) {
        logLevelMessage(LogLevel.ERROR, message, false);
    }

    public void logLevelMessage(LogLevel level, String message, boolean useJsonFormat) {
        if (level.getLevel() > logLevel) {
            return;
        }

        if (useJsonFormat) {
            JSONObject detail = new JSONObject();
            JSONObject item = new JSONObject();

            JsonFunction.put(item, "item", "log");

            JsonFunction.put(detail, "level", level.getLabel());
            JsonFunction.put(detail, "message", message);

            JsonFunction.put(item, "content", detail);

            System.out.println(item.toJSONString());
        } else {
            if (level == LogLevel.ERROR) {
                System.out.print("\u001B[31m"); // red
            } else if (level == LogLevel.INFO) {
                System.out.print("\u001B[0m"); // reset
            } else if (level == LogLevel.DEBUG) {
                System.out.print("\u001B[33m"); // yellow
            }

            // Create simple date format
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            // Format date
            String strDate = formatter.format(new Date());

            String formatMessage = "| " + strDate + " | Omno | " + level.getLabel() + " | " + message + "\u001B[0m";

            System.out.println(formatMessage);

            // Write the log to file
            try {
                // If not exist, create the file
                File file = new File("OmnoL2.log");
                if (!file.exists()) {
                    file.createNewFile();
                }

                // Write the log to file
                Files.write(Paths.get("OmnoL2.log"), (formatMessage + "\n").getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void processBlock() {

        synchronized (this) {
            if (!isConfigured || state == null) {
                logErrorMessage("error: incomplete configuration, cannot continue.");
                return;
            }

            if (state.economicCluster != null && heightStop > 0 && state.economicCluster.getHeight() >= heightStop) {
                return;
            }

            int applicationBlockCount = applicationBlockCount();

            if (applicationBlockCount <= 0) {
                logInfoMessage("not yet start height, blocks remaining: " + (applicationBlockCount + 1) + ": "
                        + heightStart + ": " + platformContext.getHeight());
                return;
            }

            EconomicCluster economicCluster = new EconomicCluster(platformContext.getBlock().getHeight(),
                    platformContext.getBlock().getBlockId(), platformContext.getBlock().getTimestamp());

            if (state != null && state.economicCluster.blockId == economicCluster.blockId) {
                return;
            }

        }

        update();

        saveState();
    }

    public void saveState() {

        synchronized (this) {
            boolean saveSuccess = state.saveState(contractName, stateRootDirectory);

            if (state.isValid()) {
                unconfirmedTransactionCache.broadcastPendingTransactions();
            }

            if (!saveSuccess) {
                logInfoMessage("WARNING: could not save state");
            }
        }
    }

    public void broadcastTransaction(JSONObject jsonObject) {
        unconfirmedTransactionCache.addPendingBroadcast(jsonObject);
    }

    public void broadcastTransaction(Transaction transaction) {
        unconfirmedTransactionCache.addPendingBroadcast(transaction);
    }

    public int applicationBlockCount() {
        return (platformContext.getHeight() + 1 - heightStart);
    }

    public void update() {

        EconomicCluster economicClusterState;
        EconomicCluster economicClusterPlatform;

        synchronized (this) {
            if (state == null || !state.isValid()) {
                state = State.loadLastValidState(this, stateName, stateRootDirectory);

                if (state == null || !state.isValid()) {
                    logInfoMessage("State: " + stateName + " NOT FOUNT, need re-sync");
                    logInfoMessage("From " + heightStart + " | Total blocks: "
                            + (platformContext.economicCluster.height - heightStart + 1));
                    EconomicCluster economicCluster = new EconomicCluster(ardorApi, heightStart - 1);
                    state = new State(this, economicCluster);
                } else {
                    logInfoMessage("State: " + state.toJSONObject().toJSONString());
                    logInfoMessage("Loaded State: " + stateName + " | State height: "
                            + state.economicCluster.height);
                }
            }

            economicClusterState = state.economicCluster.clone();
            economicClusterPlatform = platformContext.economicCluster.clone();
        }

        while (!quit && economicClusterState.height < economicClusterPlatform.height) {

            synchronized (this) {

                if (economicClusterState.height % 720 == 0) {
                    logInfoMessage("State re-sync progress height: " + economicClusterState.height);
                }

                state.nextBlock();

                economicClusterState = state.economicCluster.clone();
                economicClusterPlatform = platformContext.economicCluster.clone();
            }
        }

        synchronized (this) {

            unconfirmedTransactionCache.update();

            logInfoMessage("State: " + stateName + " | EconomicClusterState: " + economicClusterState.toJSONObject());
        }
    }

    private void configurationRead(JSONObject jsonConfiguration, boolean allowEmptyAdminPassword) {

        verifyAccount = JsonFunction.getLongFromStringUnsigned(jsonConfiguration, "verifyAccount", 0);
        isVerifier = JsonFunction.getBoolean(jsonConfiguration, "isVerifier", false);

        if (isVerifier) {

            if (verifyAccount == 0) {
                logErrorMessage("configurationRead | Not configured, isVerifier requires verifyAccount");
                return;
            }

            contractAccountId = verifyAccount;
            nxtCryptography = new NxtCryptography();

        } else {
            try {
                nxtCryptography = new NxtCryptography(
                        JsonFunction.getBytesFromHexString(jsonConfiguration, "privateKey", null));
            } catch (Exception e) {
                logErrorMessage("privateKey malformed, requires 64 character hexadecimal string");
                return;
            }

            if (!nxtCryptography.hasPrivateKey()) {
                logErrorMessage("not configured, if not verifier then privateKey is required");
                return;
            }

            contractAccountId = nxtCryptography.getAccountId();
        }

        try {
            secretForRandom = JsonFunction.getBytesFromHexString(jsonConfiguration, "secretForRandom", null);
        } catch (Exception e) {
            logErrorMessage("secretForRandom malformed, requires 64 character hexadecimal string");
            return;
        }

        adminPasswordString = JsonFunction.getString(jsonConfiguration, "adminPassword", null);

        if (!allowEmptyAdminPassword && (adminPasswordString == null || adminPasswordString.equals(""))) {
            logErrorMessage("contract requires node administrator password (adminPassword)");
            return;
        }

        contractName = JsonFunction.getString(jsonConfiguration, "contractName", null);

        if (contractName == null) {
            logErrorMessage("Not configured missing: contractName");
            return;
        }

        stateName = contractName;

        {
            JSONObject apiObject = JsonFunction.getJSONObject(jsonConfiguration, "api", null);

            if (apiObject == null) {
                logErrorMessage("API not configured");
                return;
            }

            apiPassword = JsonFunction.getString(apiObject, "password", null);

            if (apiPassword == null) {
                logErrorMessage("API password not set");
                return;
            }

            apiHost = JsonFunction.getString(apiObject, "host", "localhost");
            apiPort = JsonFunction.getInt(apiObject, "port", 30001);
        }

        {
            JSONObject apiObject = JsonFunction.getJSONObject(jsonConfiguration, "apiRemote", null);

            if (apiObject != null) {

                String apiRemotePassword = JsonFunction.getString(apiObject, "password", null);

                if (apiRemotePassword == null) {
                    logErrorMessage("remote API password not set");
                    return;
                }

                String apiRemoteProtocol = JsonFunction.getString(apiObject, "apiRemoteProtocol", "http");
                String apiRemoteHost = JsonFunction.getString(apiObject, "host", "localhost");
                String apiRemotePort = JsonFunction.getString(apiObject, "port", "30001");

                remoteApi = new RemoteApi(this, apiRemoteProtocol, apiRemoteHost, apiRemotePort, apiRemotePassword);
            }
        }

        transactionChain = JsonFunction.getInt(jsonConfiguration, "transactionChain", 2);

        heightStart = JsonFunction.getInt(jsonConfiguration, "heightStart", heightStart);
        heightStop = JsonFunction.getInt(jsonConfiguration, "heightStop", heightStop);

        reloadAndRescan = JsonFunction.getBoolean(jsonConfiguration, "reloadAndRescan", false);
        stateRootDirectory = Paths.get(JsonFunction.getString(jsonConfiguration, "stateRootDirectory", "state"));

        try {
            if (!Files.exists(stateRootDirectory)) {
                Files.createDirectories(stateRootDirectory);
            }
        } catch (IOException e) {
            logErrorMessage("Could not create configured stateRootDirectory: " + stateRootDirectory.toString());
            return;
        }

        isConfigured = true;

        logInfoMessage("Configuration loaded");

        ardorApi = new ArdorApi(JsonFunction.getString(jsonConfiguration, "protocol", "http"),
                JsonFunction.getString(jsonConfiguration, "host", "localhost"),
                JsonFunction.getString(jsonConfiguration, "port", "27876"));
    }

    private void configurationRead(String filePath) throws IOException, ParseException {

        logInfoMessage("Configuration file: " + filePath);

        File file;

        try {
            file = new File(filePath);
        } catch (Exception e) {
            logErrorMessage("Could not open file: " + filePath);
            return;
        }

        long fileLength = file.length();

        if (fileLength == 0) {
            logErrorMessage("Empty configuration: " + filePath);
            return;
        }

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonConfiguration = (JSONObject) jsonParser.parse(new String(Files.readAllBytes(file.toPath())));

        configurationRead(jsonConfiguration, false);
    }

    public void signJSONObject(JSONObject response, String key, JSONObject jsonObject) {

        if (response == null || key == null || jsonObject == null) {
            return;
        }

        JsonFunction.put(response, key, jsonObject);

        if (!isVerifier && nxtCryptography != null) {
            byte[] privateKey = nxtCryptography.getPrivateKey();

            byte[] signature = new byte[0x40];

            NxtCryptography.signBytes(jsonObject.toJSONString().getBytes(StandardCharsets.UTF_8), signature, 0x00,
                    privateKey);

            JsonFunction.put(response, "signature", JsonFunction.hexStringFromBytes(signature));
            JsonFunction.put(response, "publicKey", JsonFunction.hexStringFromBytes(nxtCryptography.getPublicKey()));
        }
    }
}
