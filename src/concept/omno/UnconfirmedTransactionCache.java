package concept.omno;

import concept.platform.Transaction;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class UnconfirmedTransactionCache {
    ApplicationContext applicationContext;
    List<Transaction> transactions = new ArrayList<>();
    List<Transaction> listPendingBroadcast = new ArrayList<>();

    public List<Transaction> removeInvalid() {
        List<Transaction> listTransactionExpiredNotCommitted = new ArrayList<>();

        Iterator<Transaction> iterator;

        iterator = transactions.iterator();

        while (iterator.hasNext()) {
            Transaction transaction = iterator.next();

            try {
                if (!transaction.isValidUnconfirmed(applicationContext.ardorApi)) {
                    iterator.remove();
                }
            } catch (Exception ignored) {
            }
        }

        iterator = transactions.iterator();

        while (iterator.hasNext()) {
            Transaction transaction = iterator.next();

            if ((transaction.timestampDeadline + (60 * 60 * 1)) < applicationContext.platformContext.economicCluster
                    .getTimestamp()) {
                JSONObject response = applicationContext.ardorApi.getTransaction(transaction.chain,
                        transaction.fullHash);

                if (response == null) {
                    listTransactionExpiredNotCommitted.add(transaction);
                    iterator.remove();
                }

            }
        }

        return listTransactionExpiredNotCommitted;
    }

    public void update() {
        List<Transaction> listReIssue = removeInvalid();
        Iterator<Transaction> iterator = listReIssue.iterator();

        while (iterator.hasNext()) {
            Transaction transaction = iterator.next();

            if (!transaction.isValid(applicationContext.ardorApi)) {
                iterator.remove();
                continue;
            }

            // TODO
            // deadline_expired_but_not_committed
            // in the rare case that a transaction is lost it can be safely re-issued after
            // it has expired
            // re-issue determined after delay to account for possible local clock offset.
            // re-issued with deterministic offset timestamp.

            // addPendingBroadcast();

        }
    }

    public UnconfirmedTransactionCache(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void addTransaction(Transaction transaction) {
        if (transaction == null) {
            return;
        }

        transactions.add(transaction);
    }

    public void addPendingBroadcast(JSONObject json) {

        if (json == null) {
            return;
        }

        applicationContext.logDebugMessage(
                "-----------------------------------------------------------------------------");
        applicationContext.logDebugMessage("addPendingBroadcast: ");
        applicationContext.logDebugMessage(json.toJSONString());

        Transaction transaction = new Transaction(json);

        listPendingBroadcast.add(transaction);
    }

    public void addPendingBroadcast(Transaction transaction) {

        if (transaction == null) {
            return;
        }

        if (transaction.isValid(applicationContext.ardorApi)) {
            listPendingBroadcast.add(transaction);
        }
    }

    public void broadcastPendingTransactions() {
        if (applicationContext.isVerifier) {
            return;
        }

        for (Transaction transaction : listPendingBroadcast) {
            if (transaction.sender == transaction.recipient) {
                continue;
            }

            boolean result = applicationContext.ardorApi.transactionBytesBroadcast(transaction.unsignedTransactionBytes,
                    transaction.attachment, applicationContext.nxtCryptography.getPrivateKey());

            if (!result) {
                applicationContext.logErrorMessage(
                        "Could not broadcast transaction: " + transaction.source.toJSONString());
                continue;
            }

            addTransaction(transaction);
        }

        listPendingBroadcast.clear();
    }
}
