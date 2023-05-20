package concept.omno.service.voting;

import concept.omno.ApplicationContext;
import concept.omno.Operations;
import concept.omno.object.Operation;
import concept.omno.object.PlatformToken;
import concept.omno.service.voting.object.AccountAllowList;
import concept.utility.JsonFunction;
import concept.utility.NxtCryptography;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public class Voting {

    public State state;

    public Voting(ApplicationContext applicationContext) {
        state = new State(applicationContext);
    }

    public Voting(ApplicationContext applicationContext, JSONObject jsonObject) {
        this(applicationContext);
        state.define(jsonObject);
    }

    public static class Poll {
        int id;
        byte[] key;
        int heightEnd, removeAfter;
        int accountAllowListId;
        int voteModel = 1;
        int voteChoiceCount = 1;
        int voteChoicesMinimum = 1;
        int voteChoicesMaximum = 1;
        boolean voteChoiceAllowDuplicate = false;
        boolean validIfDraw = false;

        PlatformToken platformToken;
        JSONArray choiceOperation;

        HashSet<String> voter = new HashSet<>();
        HashMap<Long, Long> tally = new HashMap<>();

        Poll(JSONObject jsonObject) {
            define(jsonObject);
        }

        public boolean isValid() {
            return (id > 0 && heightEnd > 0 && voteModel > 0 && voteChoiceCount > 0 && voteChoicesMaximum > 0);
        }

        public boolean isOpen(int currentHeight) {
            return (heightEnd > currentHeight);
        }

        public JSONObject toJSONObject() {
            JSONObject jsonObject = new JSONObject();

            JsonFunction.put(jsonObject, "id", id);

            if (key != null && key.length != 0) {
                JsonFunction.put(jsonObject, "key", JsonFunction.hexStringFromBytes(key));
            }

            JsonFunction.put(jsonObject, "heightEnd", heightEnd);
            JsonFunction.put(jsonObject, "removeAfter", removeAfter);
            JsonFunction.put(jsonObject, "accountAllowListId", accountAllowListId);
            JsonFunction.put(jsonObject, "voteModel", voteModel);
            JsonFunction.put(jsonObject, "voteChoiceCount", voteChoiceCount);
            JsonFunction.put(jsonObject, "voteChoicesMinimum", voteChoicesMinimum);
            JsonFunction.put(jsonObject, "voteChoicesMaximum", voteChoicesMaximum);
            JsonFunction.put(jsonObject, "voteChoiceAllowDuplicate", voteChoiceAllowDuplicate);
            JsonFunction.put(jsonObject, "validIfDraw", validIfDraw);

            if (platformToken != null && platformToken.isValid() && !platformToken.isZero() && platformToken.countUniqueTokensAll() == 1) {
                JsonFunction.put(jsonObject, "platformToken", platformToken.toJSONObject());
            }

            if (choiceOperation != null && choiceOperation.size() != 0) {
                JsonFunction.put(jsonObject, "choiceOperation", choiceOperation);
            }

            if (voter != null && voter.size() != 0) {
                JsonFunction.put(jsonObject, "voter", JsonFunction.jsonArrayFromStringHashSet(voter));
            }

            if (tally != null && tally.size() != 0) {
                JsonFunction.put(jsonObject, "tally", JsonFunction.jsonObjectStringUnsignedLongPairsFromMap(tally, false));
            }

            return  jsonObject;
        }

        private boolean define(JSONObject jsonObject) {
            if (jsonObject == null) {
                return false;
            }

            id = JsonFunction.getInt(jsonObject, "id", -1);
            key = JsonFunction.getBytesFromHexString(jsonObject, "key", null);
            heightEnd = JsonFunction.getInt(jsonObject, "heightEnd", -1);
            removeAfter = JsonFunction.getInt(jsonObject, "removeAfter", -1);
            accountAllowListId = JsonFunction.getInt(jsonObject, "accountAllowListId", -1);
            voteModel = JsonFunction.getInt(jsonObject, "voteModel", 1);
            voteChoiceCount = JsonFunction.getInt(jsonObject, "voteChoiceCount", 1);
            voteChoicesMinimum = JsonFunction.getInt(jsonObject, "voteChoicesMinimum", 1);
            voteChoicesMaximum = JsonFunction.getInt(jsonObject, "voteChoicesMaximum", 1);
            voteChoiceAllowDuplicate = JsonFunction.getBoolean(jsonObject, "voteChoiceAllowDuplicate", false);
            validIfDraw = JsonFunction.getBoolean(jsonObject, "validIfDraw", false);

            platformToken = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "platformToken", null));

            choiceOperation = JsonFunction.getJSONArray(jsonObject, "choiceOperation", null);

            voter = JsonFunction.getHashSetStringFromJsonArray(jsonObject, "voter", new HashSet<>());

            tally = JsonFunction.getHashMapLongLongFromUnsignedStringKeyValuePairs(jsonObject, "tally", new HashMap<>());

            return true;
        }

        private boolean isValidSignature(byte[] publicKey, byte[] signature, List<Integer> choice) {
            if (key == null || key.length == 0) {
                return true;
            }

            if (publicKey == null || publicKey.length == 0 || signature == null || signature.length == 0 || choice == null || choice.size() == 0) {
                return false;
            }

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            try {
                byteArrayOutputStream.write(key);

                for (int value: choice) {
                    byteArrayOutputStream.write(ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.BIG_ENDIAN).putInt(value).array());
                }

            } catch (Exception e) {
                return false;
            }

            return NxtCryptography.verifyBytes(byteArrayOutputStream.toByteArray(), signature, publicKey);
        }

        private byte[] signVote(byte[] privateKey, List<Integer> choice) {
            if (key == null || key.length == 0) {
                return null;
            }

            if (privateKey == null || privateKey.length == 0 || choice == null || choice.size() == 0) {
                return null;
            }

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            try {
                byteArrayOutputStream.write(key);

                for (int value: choice) {
                    byteArrayOutputStream.write(ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.BIG_ENDIAN).putInt(value).array());
                }

            } catch (Exception e) {
                return null;
            }

            byte[] signature = new byte[0x20];

            NxtCryptography.signBytes(byteArrayOutputStream.toByteArray(), signature, 0, privateKey);

            return signature;
        }

        public PlatformToken getPlatformToken() {

            if (platformToken != null && platformToken.isZero()) {
                return null;
            }

            return platformToken;
        }

        private boolean canVote(byte[] account) {
            if (account == null || account.length == 0) {
                return false;
            }

            if (voter == null || voter.size() == 0) {
                return true;
            }

            return !voter.contains(JsonFunction.hexStringFromBytes(account));
        }

        private void addVoter(byte[] account) {

            if (voter == null) {
                voter = new HashSet<>();
            }

            if (canVote(account)) {
                voter.add(JsonFunction.hexStringFromBytes(account));
            }
        }

        public void addChoice(long id, long value) {
            if (id < 0 || id >= voteChoiceCount) {
                return;
            }

            if (tally == null) {
                tally = new HashMap<>();
            }

            long count = value;

            if (tally.size() != 0 && tally.containsKey(id)) {
                count += tally.get(id);
            }

            tally.put(id, count);
        }

        private long getHashMapValueTotal(HashMap<Long, Long> hashMap) {

            if (hashMap == null || hashMap.size() == 0) {
                return 0;
            }

            long result = 0;

            for (long value: hashMap.values()) {

                long resultNew = result + value;

                if (resultNew < result) {
                    result = -1;
                    break;
                }

                result = resultNew;
            }

            return result;
        }

        public long getTallyTotal() {
            return getHashMapValueTotal(tally);
        }

        private long getHashMapValueHigh(HashMap<Long, Long> hashMap) {

            if (hashMap == null || hashMap.size() == 0) {
                return 0;
            }

            long result = 0;

            for (long value: hashMap.values()) {

                if (result < value) {
                    result = value;
                }
            }

            return result;
        }

        private Set<Long> getHashMapKeysWithValueHigh(HashMap<Long, Long> hashMap) {

            if (hashMap == null || hashMap.size() == 0) {
                return null;
            }

            long valueHigh = getHashMapValueHigh(hashMap);

            Set<Long> result = new TreeSet<>();

            for (long key: hashMap.keySet()) {

                long value = hashMap.get(key);

                if (value > valueHigh) {
                    throw new RuntimeException();
                }

                if (value == valueHigh) {
                    result.add(key);
                }
            }

            return result;
        }

        private boolean isChoiceConditionMet(JSONObject jsonObject) {

            if (jsonObject == null || tally == null) {
                return false;
            }

            int choice = JsonFunction.getInt(jsonObject, "choice", -1);

            if (choice > voteChoicesMaximum || choice < voteChoicesMinimum) {
                return false;
            }

            boolean validAnyRank = JsonFunction.getBoolean(jsonObject, "validAnyRank", false);

            Set<Long> listChoiceWin = getHashMapKeysWithValueHigh(tally);

            if (!validAnyRank && (listChoiceWin == null || listChoiceWin.size() == 0 || (listChoiceWin.size() > 1 && !validIfDraw))) {
                return false;
            }

            boolean choiceIsHigh = listChoiceWin != null && listChoiceWin.size() > 0 && listChoiceWin.contains((long) choice);

            if (!validAnyRank && !choiceIsHigh) {
                return false;
            }

            double thresholdFraction = JsonFunction.getDoubleFromString(jsonObject, "thresholdFraction", 0);

            if (thresholdFraction < 0) {
                return false;
            }

            long thresholdAbsolute = JsonFunction.getLongFromStringUnsigned(jsonObject, "thresholdAbsolute", 1);

            if (thresholdAbsolute < 0) {
                return false;
            }

            boolean thresholdAndRules = JsonFunction.getBoolean(jsonObject, "thresholdAndRules", true);

            long tallyValueTotal = getTallyTotal();
            long choiceTallyValue = 0;

            if (tally.containsKey((long) choice)) {
                choiceTallyValue = tally.get((long) choice);
            }

            boolean thresholdFractionTrue = (thresholdFraction >= ((double) choiceTallyValue / (double) tallyValueTotal));
            boolean thresholdAbsoluteTrue = thresholdAbsolute <= choiceTallyValue;

            if (!thresholdFractionTrue && !thresholdAbsoluteTrue) {
                return false;
            }

            if (thresholdAndRules && !(thresholdFractionTrue && thresholdAbsoluteTrue)) {
                return false;
            }

            return true;
        }

        public List<Operation> getPollOperationListByResult(long account) {

            if (choiceOperation == null || choiceOperation.size() == 0) {
                return null;
            }

            List<Operation> result = new ArrayList<>();

            for (Object object: choiceOperation) {

                if (! (object instanceof JSONObject)) {
                    continue;
                }

                if (!isChoiceConditionMet((JSONObject) object)) {
                    continue;
                }

                JSONObject jsonObject = (JSONObject) object;

                JSONArray jsonArray = JsonFunction.getJSONArray(jsonObject, "operation", null);

                if (jsonArray == null || jsonArray.size() == 0) {
                    continue;
                }

                List<Operation> operationList = Operations.createListOfOperations(account, jsonArray);

                if (operationList != null && operationList.size() > 0) {
                    result.addAll(operationList);
                }
            }

            return result;
        }
    }

    public static class State {
        HashMap<Integer, Poll> poll = new HashMap<>();
        HashMap<Integer, AccountAllowList> accountAllowList= new HashMap<>();

        long incomeAccount;
        PlatformToken operationFee = new PlatformToken();

        ApplicationContext applicationContext;

        State(ApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
            this.incomeAccount = applicationContext.contractAccountId;
        }

        State(ApplicationContext applicationContext, JSONObject jsonObject) {
            this(applicationContext);
            define(jsonObject);
        }

        public JSONObject toJSONObject() {
            JSONObject jsonObject = new JSONObject();

            if (poll != null && poll.size() != 0) {
                JSONArray jsonArray = new JSONArray();

                for (Poll item: poll.values()) {
                    if (item.isValid()) {
                        JsonFunction.add(jsonArray, item.toJSONObject());
                    }
                }

                if (jsonArray.size() != 0) {
                    JsonFunction.put(jsonObject, "poll", jsonArray);
                }
            }

            if (accountAllowList != null && accountAllowList.size() != 0) {
                JSONArray jsonArray = new JSONArray();

                for (AccountAllowList item: accountAllowList.values()) {
                    if (item.isValid()) {
                        JsonFunction.add(jsonArray, item.toJSONObject());
                    }
                }

                if (jsonArray.size() != 0) {
                    JsonFunction.put(jsonObject, "accountAllowList", jsonArray);
                }
            }

            JsonFunction.put(jsonObject, "incomeAccount", Long.toUnsignedString(incomeAccount));
            JsonFunction.put(jsonObject, "operationFee", operationFee.toJSONObject());

            return  jsonObject;
        }

        public boolean define(JSONObject jsonObject) {
            if (jsonObject == null) {
                return false;
            }

            JSONArray jsonArray;

            jsonArray = JsonFunction.getJSONArray(jsonObject, "poll", null);

            if (jsonArray != null && jsonArray.size() != 0) {
                for (Object object: jsonArray) {
                    Poll item = new Poll((JSONObject) object);

                    if (item.isValid()) {
                        poll.put(item.id, item);
                    }
                }
            }

            jsonArray = JsonFunction.getJSONArray(jsonObject, "accountAllowList", null);

            if (jsonArray != null && jsonArray.size() != 0) {
                for (Object object: jsonArray) {
                    AccountAllowList item = new AccountAllowList((JSONObject) object);

                    if (item.isValid()) {
                        accountAllowList.put(item.id, item);
                    }
                }
            }

            incomeAccount = JsonFunction.getLongFromStringUnsigned(jsonObject, "incomeAccount", applicationContext.contractAccountId);
            operationFee = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "operationFee", null));

            return true;
        }

        public void configure(JSONObject jsonObject) {
            if (jsonObject == null) {
                return;
            }

            incomeAccount = JsonFunction.getLongFromStringUnsigned(jsonObject, "incomeAccount", incomeAccount);
            JSONObject feeObject = JsonFunction.getJSONObject(jsonObject, "operationFee", null);

            if (feeObject != null) {
                PlatformToken newFee = new PlatformToken(feeObject);

                if (newFee.isValid()) {
                    operationFee = newFee;
                }
            }
        }

        public boolean processOperation(Operation operation) {
            boolean result = false;

            if (operation == null) {
                return false;
            }

            if (operation.account != applicationContext.contractAccountId) {
                if (!applicationContext.state.userAccountState.subtractFromBalance(operation.account, operationFee)) {
                    return false;
                }

                applicationContext.state.userAccountState.addToBalance(incomeAccount, operationFee);
            }

            switch (operation.request) {

                case "configure": {
                    result = operationConfigure(operation);
                    break;
                }

                case "configurePoll": {
                    result = operationConfigurePoll(operation);
                    break;
                }

                case "removePoll": {
                    result = operationRemovePoll(operation);
                    break;
                }

                case "allowListAccountAdd": {
                    result = operationAllowListAccountAdd(operation);
                    break;
                }

                case "allowListAccountRemove": {
                    result = operationAllowListAccountRemove(operation);
                    break;
                }

                case "allowListRemove": {
                    result = operationAllowListRemove(operation);
                    break;
                }

                case "vote": {
                    result = operationVote(operation);
                    break;
                }
            }

            return result;
        }

        private boolean operationConfigure(Operation operation) {

            if (operation == null || operation.parameterJson == null) {
                return false;
            }

            if (operation.account != applicationContext.contractAccountId) {
                return false;
            }

            configure(operation.parameterJson);

            return true;
        }

        private boolean operationConfigurePoll(Operation operation) {

            if (operation == null || operation.parameterJson == null) {
                return false;
            }

            if (operation.account != applicationContext.contractAccountId) {
                return false;
            }

            JSONObject jsonObject = operation.parameterJson;

            Poll poll = new Poll(jsonObject);

            if (!poll.isValid()) {
                return false;
            }

            if (poll.isValid() && (this.poll.size() == 0 || ! this.poll.containsKey(poll.id))) {
                this.poll.put(poll.id, poll);
            } else {
                return false;
            }

            return true;
        }

        private boolean operationRemovePoll(Operation operation) {

            if (operation == null || operation.parameterJson == null) {
                return false;
            }

            if (operation.account != applicationContext.contractAccountId) {
                return false;
            }

            JSONObject jsonObject = operation.parameterJson;

            int id = JsonFunction.getInt(jsonObject, "pollId", -1);

            Poll poll = getPoll(id);

            if (poll == null) {
                return false;
            }

            this.poll.remove(poll.id);

            return true;
        }

        private boolean operationAllowListAccountAdd(Operation operation) {

            if (operation == null || operation.parameterJson == null) {
                return false;
            }

            if (operation.account != applicationContext.contractAccountId) {
                return false;
            }

            JSONObject jsonObject = operation.parameterJson;

            int id = JsonFunction.getInt(jsonObject, "id", -1);

            if (id <= 0) {
                return false;
            }

            AccountAllowList accountAllowList = getAccountAllowList(id);

            if (accountAllowList == null) {
                int signatureAlgorithm = JsonFunction.getInt(jsonObject, "signatureAlgorithm", 1);
                accountAllowList = new AccountAllowList(id, signatureAlgorithm);
            }

            byte[] account = JsonFunction.getBytesFromHexString(jsonObject, "publicKey", null);

            if (account == null || account.length == 0) {
                return false;
            }

            accountAllowList.allow(account);

            this.accountAllowList.put(accountAllowList.id, accountAllowList);

            return true;
        }

        private boolean operationAllowListAccountRemove(Operation operation) {

            if (operation == null || operation.parameterJson == null) {
                return false;
            }

            if (operation.account != applicationContext.contractAccountId) {
                return false;
            }

            JSONObject jsonObject = operation.parameterJson;

            int id = JsonFunction.getInt(jsonObject, "id", -1);

            if (id <= 0) {
                return false;
            }

            AccountAllowList accountAllowList = getAccountAllowList(id);

            if (accountAllowList == null) {
                return false;
            }

            byte[] account = JsonFunction.getBytesFromHexString(jsonObject, "publicKey", null);

            if (account == null || account.length == 0) {
                return false;
            }

            accountAllowList.remove(account);

            return true;
        }

        private boolean operationAllowListRemove(Operation operation) {

            if (operation == null || operation.parameterJson == null) {
                return false;
            }

            if (operation.account != applicationContext.contractAccountId) {
                return false;
            }

            JSONObject jsonObject = operation.parameterJson;

            int id = JsonFunction.getInt(jsonObject, "id", -1);

            if (id <= 0) {
                return false;
            }

            AccountAllowList accountAllowList = getAccountAllowList(id);

            if (accountAllowList == null) {
                return false;
            }

            this.accountAllowList.remove(id);

            return true;
        }

        private boolean operationVote(Operation operation) {

            if (operation == null || operation.parameterJson == null) {
                return false;
            }

            JSONObject jsonObject = operation.parameterJson;

            JSONArray voteArray = JsonFunction.getJSONArray(jsonObject, "vote", null);

            if (voteArray == null || voteArray.size() == 0) {
                return false;
            }

            for (Object object: voteArray) {
                if (!(object instanceof JSONObject)) {
                    continue;
                }

                JSONObject o = (JSONObject) object;

                int id = JsonFunction.getInt(o, "id", -1);

                Poll poll = getPoll(id);

                if (poll == null || poll.heightEnd <= applicationContext.state.economicCluster.getHeight()) {
                    continue;
                }

                List<Integer> choiceList = JsonFunction.getListIntegerFromJsonArray(o, "choice", null);

                if (choiceList == null || choiceList.size() == 0 || choiceList.size() > poll.voteChoicesMaximum || choiceList.size() < poll.voteChoicesMinimum) {
                    continue;
                }

                List<Integer> choiceListUnique = listUniqueValues(choiceList);

                if (!poll.voteChoiceAllowDuplicate && choiceListUnique.size() < choiceList.size()) {
                    continue;
                }

                byte[] publicKey = JsonFunction.getBytesFromHexString(o, "publicKey", null);

                boolean voteByAccount = false;
                PlatformToken platformToken = poll.getPlatformToken();

                if (publicKey == null || ((poll.key == null || poll.key.length == 0))) {
                    publicKey = applicationContext.ardorApi.getAccountPublicKey(operation.account);
                    voteByAccount = true;
                }

                if (!voteByAccount && platformToken != null) {
                    continue;
                }

                if (!poll.canVote(publicKey)) {
                    continue;
                }

                if (poll.accountAllowListId > 0) {

                    AccountAllowList accountAllowList = getAccountAllowList(poll.accountAllowListId);

                    if (accountAllowList == null) {
                        continue;
                    }

                    if (!accountAllowList.isAllowed(publicKey)) {
                        continue;
                    }

                    if (poll.key != null && poll.key.length != 0) {
                        byte[] signature = JsonFunction.getBytesFromHexString(o, "signature", null);

                        if (!poll.isValidSignature(publicKey, signature, choiceList)) {
                            continue;
                        }
                    }
                }

                long value = 1;

                if (platformToken != null && !platformToken.isZero()) {
                    value = applicationContext.state.userAccountState.getBalanceByUniqueAsset(operation.account, platformToken);

                    if (value <= 0) {
                        continue;
                    }
                }

                poll.addVoter(publicKey);

                for (int choice: choiceList) {
                    poll.addChoice(choice, value);
                }

                applicationContext.logDebugMessage("Omno | Voting: " + poll.toJSONObject().toJSONString());
            }

            return true;
        }

        public List<Integer> listUniqueValues(List<Integer> list) {

            List<Integer> result = new ArrayList<>();

            if (list == null || list.size() == 0) {
                return result;
            }

            for (int value: list) {
                if (result.size() == 0 || !result.contains(value)) {
                    result.add(value);
                    continue;
                }

                break;
            }

            return result;
        }

        public Poll getPoll(int id) {

            if (id <= 0) {
                return null;
            }

            Poll result = null;

            if (poll != null && poll.size() != 0 && poll.containsKey(id)) {
                result = poll.get(id);

                if (!result.isValid()) {
                    return null;
                }
            }

            return result;
        }

        public AccountAllowList getAccountAllowList(int id) {

            AccountAllowList result = null;

            if (accountAllowList != null && accountAllowList.size() != 0 && accountAllowList.containsKey(id)) {

                result = accountAllowList.get(id);
            }

            return result;
        }

        public boolean processBlock(List<Operation> operationList) {

            if (poll != null && poll.size() != 0 ) {

                List<Poll> list = new ArrayList<>(poll.values());

                int currentHeight = applicationContext.state.economicCluster.getHeight();

                for (Poll item: list) {

                    if (item.heightEnd == currentHeight && item.choiceOperation != null) {

                        List<Operation> pollOperationList = item.getPollOperationListByResult(applicationContext.contractAccountId);

                        if (pollOperationList != null && pollOperationList.size() > 0) {
                            operationList.addAll(pollOperationList);
                        }
                    }

                    if (item.removeAfter >= 0 && (item.heightEnd + item.removeAfter < currentHeight)) {
                        applicationContext.logInfoMessage("Omno | Voting: Expired poll: " + item.id);
                        poll.remove(item.id);
                    }
                }
            }

            return true;
        }

        public JSONObject apiProcessRequestPoll(JSONObject jsonObject) {

            JSONObject result = new JSONObject();

            if (jsonObject == null) {
                JsonFunction.put(result, "error", "missing parameter");
                return result;
            }

            int id = JsonFunction.getInt(jsonObject, "id", -1);

            if (id <= 0) {
                JsonFunction.put(result, "error", "invalid id");
                return result;
            }

            Poll poll = getPoll(id);

            if (poll == null) {
                JsonFunction.put(result, "error", "invalid id");
                return result;
            }

            result = poll.toJSONObject();

            return result;
        }

        public JSONObject apiProcessRequestAllowList(JSONObject jsonObject) {

            JSONObject result = new JSONObject();

            if (jsonObject == null) {
                JsonFunction.put(result, "error", "missing parameter");
                return result;
            }

            int id = JsonFunction.getInt(jsonObject, "id", -1);

            if (id <= 0) {
                JsonFunction.put(result, "error", "invalid id");
                return result;
            }

            AccountAllowList accountAllowList = getAccountAllowList(id);

            if (accountAllowList == null) {
                JsonFunction.put(result, "error", "invalid id");
                return result;
            }

            result = accountAllowList.toJSONObject();

            return result;
        }
    }
}
