package concept.omno.service.PlatformTokenExchangeById;

import concept.omno.ApplicationContext;
import concept.omno.object.NativeAsset;
import concept.omno.object.Operation;
import concept.omno.object.PlatformToken;
import concept.omno.object.UserAccount;
import concept.utility.JsonFunction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class State {
    ApplicationContext applicationContext;

    HashMap<Long, Offer> offer = new HashMap<>();

    long issueCount = 0;

    long incomeAccount = 0;
    PlatformToken operationFee = new PlatformToken();

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();

        if (offer != null && offer.size() != 0) {
            JSONArray jsonArray = new JSONArray();

            for (Offer value : offer.values()) {
                JsonFunction.add(jsonArray, value.toJSONObject());
            }

            JsonFunction.put(jsonObject, "offer", jsonArray);
        }

        JsonFunction.put(jsonObject, "issueCount", Long.toUnsignedString(issueCount));

        JsonFunction.put(jsonObject, "incomeAccount", Long.toUnsignedString(incomeAccount));
        JsonFunction.put(jsonObject, "operationFee", operationFee.toJSONObject());

        return jsonObject;
    }

    public void define(JSONObject jsonObject, boolean validate) {
        if (jsonObject == null) {
            return;
        }

        defineOffer(JsonFunction.getJSONArray(jsonObject, "offer", null), validate);
        issueCount = JsonFunction.getLongFromStringUnsigned(jsonObject, "issueCount", 0);

        incomeAccount = JsonFunction.getLongFromStringUnsigned(jsonObject, "incomeAccount",
                applicationContext.contractAccountId);
        operationFee = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "operationFee", null));
    }

    public State(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        incomeAccount = applicationContext.contractAccountId;
    }

    public State(ApplicationContext applicationContext, JSONObject jsonObject, boolean validate) {
        this(applicationContext);
        define(jsonObject, validate);
    }

    public void defineOffer(JSONArray jsonArray, boolean validate) {
        if (jsonArray == null || jsonArray.size() == 0) {
            return;
        }

        for (Object o : jsonArray) {
            defineOffer((JSONObject) o, validate);
        }
    }

    public void defineOffer(JSONObject jsonObject, boolean validate) {
        if (jsonObject == null) {
            return;
        }

        Offer item = new Offer(jsonObject);

        if (validate && (applicationContext.state.nativeAssetState == null
                || !item.isValid(applicationContext.state.nativeAssetState))) {
            return;
        }

        offer.put(item.id, item);
    }

    public boolean issueOffer(Offer offer) {

        if (offer == null || !offer.isValid(applicationContext.state.nativeAssetState)) {
            return false;
        }

        offer.id = ++issueCount;

        this.offer.put(offer.id, offer);

        return true;
    }

    public boolean removeOffer(long id) {

        if (offer.containsKey(id)) {
            offer.remove(id);
            return true;
        }

        return false;
    }

    public Offer getOffer(long id) {

        if (offer.containsKey(id)) {
            return offer.get(id);
        }

        return null;
    }

    public List<Offer> listOfferClone(List<Offer> offerList) {

        if (offerList == null || offerList.size() == 0) {
            return null;
        }

        List<Offer> result = new ArrayList<>();

        for (Offer item : offerList) {
            result.add(item.clone());
        }

        return result;
    }

    public static void sortListOfferAscending(List<Offer> list) {

        if (list == null || list.size() == 0) {
            return;
        }

        boolean modified = true;

        int counter = list.size();

        while (modified) {

            modified = false;

            for (int i = 0; i < counter - 1; i++) {

                long id = list.get(i).id;
                long idNext = list.get(i + 1).id;

                if (id > idNext) {
                    Offer item = list.get(i);
                    Offer itemNext = list.get(i + 1);

                    list.set(i, itemNext);
                    list.set(i + 1, item);

                    modified = true;
                }
            }

            counter--;
        }
    }

    public List<Offer> getListOfferClone() {

        if (offer == null || offer.size() == 0) {
            return null;
        }

        List<Offer> result = listOfferClone(new ArrayList<>(offer.values()));

        // maybe replace source with treeMap;
        sortListOfferAscending(result);

        return result;
    }

    public boolean canAcceptOffer(long id, PlatformToken balance, long multiplier) {

        if (!offer.containsKey(id) || balance == null || balance.isZero()) {
            return false;
        }

        Offer offer = this.offer.get(id);

        return offer.canTakeFromBalance(balance, multiplier);
    }

    public boolean acceptOffer(long id, PlatformToken balance, long multiplier, boolean mergeBalance) {

        if (!canAcceptOffer(id, balance, multiplier)) {
            return false;
        }

        Offer offer = this.offer.get(id);

        if (mergeBalance) {
            PlatformToken give = offer.give.clone();
            give.multiply(multiplier);
            balance.merge(give, true);
        }

        offer.reduce(multiplier);

        if (offer.multiplier == 0) {
            this.offer.remove(offer.id);
        }

        return true;
    }

    public boolean offerCancel(long id, PlatformToken balance) {

        if (!offer.containsKey(id)) {
            return false;
        }

        Offer offerToCancel = offer.get(id);

        if (balance != null) {
            PlatformToken give = offerToCancel.give.clone();
            give.multiply(offerToCancel.multiplier);
            balance.merge(give, true);
        }

        offer.remove(offerToCancel.id);

        return true;
    }

    private boolean operationOfferCreate(Operation operation) {

        if (operation == null || operation.parameterJson == null) {
            return false;
        }

        JSONObject jsonObject = operation.parameterJson;

        PlatformToken give = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "give", null));
        PlatformToken take = new PlatformToken(JsonFunction.getJSONObject(jsonObject, "take", null));
        long multiplier = JsonFunction.getLongFromStringUnsigned(jsonObject, "multiplier", 0);

        Offer offer = new Offer(operation.account, give, take, multiplier);

        if (multiplier <= 0 || !give.isValid() || !take.isValid()) {
            applicationContext.logInfoMessage(
                    "operationOfferCreate: invalid offer - multiplier <= 0 || ! give.isValid() || !take.isValid()");
            return false;
        }

        if (!offer.isValid(applicationContext.state.nativeAssetState)) {
            applicationContext.logInfoMessage("operationOfferCreate: invalid offer");
            return false;
        }

        PlatformToken giveTotal = give.clone();
        giveTotal.multiply(multiplier);

        if (!giveTotal.isValid()) {
            applicationContext.logInfoMessage("operationOfferCreate: invalid giveTotal");
            return false;
        }

        UserAccount userAccount = applicationContext.state.userAccountState.getUserAccount(operation.account);

        if (!offer.canGiveFromBalance(userAccount.balance)) {
            applicationContext.logInfoMessage("operationOfferCreate - canGiveFromBalance");
            return false;
        }

        if (!applicationContext.state.userAccountState.hasRequiredBalance(operation.account, giveTotal)) {
            applicationContext
                    .logInfoMessage("operationOfferCreate - userAccountState.hasRequiredBalance");
            return false;
        }

        if (!issueOffer(offer)) {
            applicationContext.logInfoMessage("operationOfferCreate -issueOffer");
            return false;
        }

        applicationContext.state.userAccountState.subtractFromBalance(operation.account, giveTotal);
        return true;
    }

    private boolean operationOfferAccept(Operation operation) {

        if (operation == null || operation.parameterJson == null) {
            return false;
        }

        JSONObject jsonObject = operation.parameterJson;

        long id = JsonFunction.getLongFromStringUnsigned(jsonObject, "id", 0);
        long multiplier = JsonFunction.getLongFromStringUnsigned(jsonObject, "multiplier", 0);

        Offer offer = getOffer(id);

        if (offer == null) {
            applicationContext.logInfoMessage("operationOfferAccept: offer == null");
            return false;
        }

        PlatformToken takeTotal = offer.take.clone();
        PlatformToken giveTotal = offer.give.clone();
        takeTotal.multiply(multiplier);
        giveTotal.multiply(multiplier);

        if (!takeTotal.isValid() || !giveTotal.isValid()) {
            applicationContext.logInfoMessage("operationOfferAccept: invalid takeTotal || giveTotal");
            return false;
        }

        UserAccount userAccount = applicationContext.state.userAccountState.getUserAccount(operation.account);

        boolean hasRoyalty = offer.hasRoyalty(applicationContext.state.nativeAssetState);

        if (!acceptOffer(id, userAccount.balance, multiplier, !hasRoyalty)) {
            applicationContext.logInfoMessage("operationOfferAccept: !acceptOffer");
            return false;
        }

        applicationContext.state.userAccountState.subtractFromBalance(operation.account, takeTotal);

        if (hasRoyalty) {
            NativeAsset royaltyNativeAssetGive = applicationContext.state.nativeAssetState
                    .getRoyaltyNativeAsset(offer.give);
            NativeAsset royaltyNativeAssetTake = applicationContext.state.nativeAssetState
                    .getRoyaltyNativeAsset(offer.take);

            double royaltyFraction;
            long royaltyAccount;

            if (royaltyNativeAssetGive != null) {
                royaltyFraction = royaltyNativeAssetGive.royaltyFraction;
                royaltyAccount = royaltyNativeAssetGive.royaltyAccount;

                PlatformToken forOwner = takeTotal.clone();
                PlatformToken forRoyalty = forOwner.clone();

                forRoyalty.multiply(royaltyFraction);
                forOwner.merge(forRoyalty, false);

                //
                applicationContext.state.userAccountState.addToBalance(operation.account, giveTotal);

                applicationContext.state.userAccountState.addToBalance(royaltyAccount, forRoyalty);
                applicationContext.state.userAccountState.addToBalance(offer.account, forOwner);
                //

            } else if (royaltyNativeAssetTake != null) {
                royaltyFraction = royaltyNativeAssetTake.royaltyFraction;
                royaltyAccount = royaltyNativeAssetTake.royaltyAccount;

                PlatformToken forOperationAccount = giveTotal.clone();
                PlatformToken forRoyalty = forOperationAccount.clone();

                forRoyalty.multiply(royaltyFraction);
                forOperationAccount.merge(forRoyalty, false);

                //
                applicationContext.state.userAccountState.addToBalance(offer.account, takeTotal);

                applicationContext.state.userAccountState.addToBalance(royaltyAccount, forRoyalty);
                applicationContext.state.userAccountState.addToBalance(operation.account, forOperationAccount);
                //

            }
        }

        return true;
    }

    private boolean operationOfferCancel(Operation operation) {

        if (operation == null || operation.parameterJson == null) {
            return false;
        }

        JSONObject jsonObject = operation.parameterJson;

        long id = JsonFunction.getLongFromStringUnsigned(jsonObject, "id", 0);

        if (id <= 0) {
            return false;
        }

        Offer offer = getOffer(id);

        if (offer == null || offer.account != operation.account) {
            return false;
        }

        UserAccount userAccount = applicationContext.state.userAccountState.getUserAccount(offer.account);

        offerCancel(id, userAccount.balance);

        return true;
    }

    private void configure(JSONObject jsonObject) {
        if (jsonObject == null) {
            return;
        }

        incomeAccount = JsonFunction.getLongFromStringUnsigned(jsonObject, "incomeAccount",
                applicationContext.contractAccountId);

        JSONObject feeObject = JsonFunction.getJSONObject(jsonObject, "operationFee", null);

        if (feeObject != null) {
            PlatformToken newFee = new PlatformToken(feeObject);

            if (newFee.isValid()) {
                operationFee = newFee;
            }
        }
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

            case "offer": {
                result = operationOfferCreate(operation);
                break;
            }

            case "accept": {
                result = operationOfferAccept(operation);
                break;
            }

            case "cancel": {
                result = operationOfferCancel(operation);
                break;
            }
        }

        return result;
    }
}
