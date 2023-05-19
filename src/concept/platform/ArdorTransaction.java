package concept.platform;

import concept.utility.NxtCryptography;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class ArdorTransaction {
    private static final int[] transactionBytesSignatureOffset = {0x45, 0x2d};
    private static final int transactionBytesVersionOffset = 0x06;

    private long ecBlockId;
    private int ecBlockHeight;
    private int chain;

    private byte[] publicKey = null;
    private byte[] privateKey = null;

    private long recipientAccountId = 0;

    ArdorTransaction(int chain, int ecBlockHeight, long ecBlockId, int deadline) {
        this.chain = chain;
        this.ecBlockHeight = ecBlockHeight;
        this.ecBlockId = ecBlockId;
    }

    public void setPrivateKey(byte[] privateKey) {
        this.privateKey = privateKey;
        publicKey = NxtCryptography.publicKeyFromPrivateKey(privateKey);
    }

    public void setPublicKey(byte[] publicKey) {
        this.privateKey = null;
        this.publicKey = publicKey;
    }

    public void setEcBlockId(long ecBlockId) {
        this.ecBlockId = ecBlockId;
    }

    public void setEcBlockHeight(int ecBlockHeight) {
        this.ecBlockHeight = ecBlockHeight;
    }

    public void setChain(int chain) {
        this.chain = chain;
    }

    public void setRecipientAccountId(long recipientAccountId) {
        this.recipientAccountId = recipientAccountId;
    }

    public static byte[] calculateTransactionBytesFullHash(byte[] transactionBytes) {
        byte[] transactionBytesUnsigned = Arrays.copyOf(transactionBytes, transactionBytes.length);
        byte transactionVersionByte = (byte) (transactionBytes[transactionBytesVersionOffset] - 1);
        assert transactionVersionByte == 0x01 || transactionVersionByte == 0x00;

        byte[] signature = Arrays.copyOfRange(transactionBytes, transactionBytesSignatureOffset[transactionVersionByte], transactionBytesSignatureOffset[transactionVersionByte] + 0x40);

        for(int i = transactionBytesSignatureOffset[transactionVersionByte]; i < transactionBytesSignatureOffset[transactionVersionByte] + 0x40; i++) {
            transactionBytesUnsigned[i] = 0;
        }

        MessageDigest md = null;

        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) { /* empty */ }

        assert md != null;
        byte[] signatureHash = md.digest(signature);

        md.update(transactionBytesUnsigned);

        return md.digest(signatureHash);
    }

    public static void signUnsignedTransactionBytes(byte[] unsignedTransactionBytes, byte[] privateKey) {

        if (unsignedTransactionBytes == null || unsignedTransactionBytes.length < 0x40 || privateKey == null || privateKey.length != 0x20) {
            return;
        }

        byte transactionVersionByte = (byte) (unsignedTransactionBytes[transactionBytesVersionOffset] - 1);
        assert transactionVersionByte == 0x01 || transactionVersionByte == 0x00;
        NxtCryptography.signBytes(unsignedTransactionBytes, unsignedTransactionBytes, transactionBytesSignatureOffset[transactionVersionByte], privateKey);
    }
}
