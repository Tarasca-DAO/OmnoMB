package concept.utility;

import concept.cryptography.Curve25519;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class NxtCryptography {
    private byte[] publicKey = null;
    private byte[] privateKey = null;
    long accountId = 0;
    String accountRS = null;

    public NxtCryptography() {}

    public NxtCryptography(byte[] privateKey) {

        if (privateKey != null && privateKey.length == 0x20) {
            this.privateKey = Arrays.copyOf(privateKey, 0x20);
            publicKey = publicKeyFromPrivateKey(privateKey);
        }
    }

    public NxtCryptography(String privateKeyString) {

        if (privateKeyString == null || privateKeyString.length() == 0) {
            return;
        }

        if (privateKeyString.length() == 0x40) {
            privateKey = bytesFromHexString(privateKeyString);
        } else {
            privateKey = getHash(privateKeyString.getBytes(StandardCharsets.UTF_8), "SHA-256");
        }

        publicKey = publicKeyFromPrivateKey(privateKey);
    }

    public String getAccountRS() {
        return accountRS;
    }

    public String getAccountRSAbbreviated() {
        String result = accountRS;

        if (result != null && result.length() > 6 && result.getBytes(StandardCharsets.UTF_8)[5] == '-') {
            result = result.substring(6);
        }

        return result;
    }

    public void setAccountRS(String accountRS) {
        this.accountRS = accountRS;
    }

    public void setPublicKeyString(String publicKeyString) {
        if (publicKeyString != null && publicKeyString.length() == 0x040) {
            setPublicKey(bytesFromHexString(publicKeyString));
        }
    }

    public void setPublicKey(byte[] publicKey) {
        this.privateKey = null;
        this.publicKey = publicKey;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public String getPublicKeyString() {
        return hexStringFromBytes(publicKey);
    }

    public byte[] getPrivateKey() {
        return  privateKey;
    }

    public String getPrivateKeyString() {
        return hexStringFromBytes(privateKey);
    }

    public boolean hasPrivateKey() {
        return privateKey != null;
    }
    public boolean hasPublicKey() {
        return publicKey != null;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public long getAccountId() {

        if (accountId == 0) {
            accountId = accountIdFromPublicKey(this.publicKey);
        }

        return accountId;
    }

    public String getAccountIdString() {
        return Long.toUnsignedString(getAccountId());
    }

    public static long accountIdFromPrivateKey(byte[] privateKey) {
        return accountIdFromPublicKey(publicKeyFromPrivateKey(privateKey));
    }

    public static long accountIdFromPublicKey(byte[] publicKey) {

        if (publicKey == null | publicKey.length != 0x20) {
            return 0;
        }

        MessageDigest md = null;

        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) { /* empty */ }

        assert md != null;
        byte[] publicKeyHash = md.digest(publicKey);

        return longLsbFromBytes(publicKeyHash);
    }

    public static void signBytes(byte[] message, byte[] signature, int signatureOffset, byte[] privateKey) {
        /*
            signBytes

            source wikipedia.org/wiki/KCDSA
            modified using as source NXT Crypto.java
         */

        for(int i = signatureOffset; i < signatureOffset + 0x40 && i < signature.length; i++) {
            signature[i] = 0;
        }

        MessageDigest messageDigest = null;

        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch ( NoSuchAlgorithmException e) { /* empty */ }

        assert messageDigest != null;

        byte[] m = messageDigest.digest(message);

        byte[] publicKey = new byte[0x20];
        byte[] privateKeyForSigning = new byte[0x20];

        Curve25519.keygen(publicKey, privateKeyForSigning, privateKey);

        messageDigest.update(m);
        byte[] k = messageDigest.digest(privateKeyForSigning);

        byte[] w = new byte[0x20];
        Curve25519.keygen(w, null, k);

        messageDigest.update(m);
        byte[] r = messageDigest.digest(w);

        byte[] s = new byte[0x20];
        Curve25519.sign(s, r, k, privateKeyForSigning);

        System.arraycopy(s, 0, signature, signatureOffset, 0x20);
        System.arraycopy(r, 0, signature, signatureOffset + 0x20, 0x20);
    }

    public static boolean verifyBytes(byte[] message, byte[] signature, byte[] publicKey) {
        /*
            verifyBytes

            source wikipedia.org/wiki/KCDSA
            modified using as source NXT Crypto.java
         */

        boolean isValid = false;

        if (Curve25519.isCanonicalSignature(signature) && Curve25519.isCanonicalPublicKey(publicKey)) {

            byte[] s = new byte[0x20];
            System.arraycopy(signature, 0, s, 0, 0x20);

            byte[] r = new byte[0x20];
            System.arraycopy(signature, 0x20, r, 0, 0x20);

            byte[] z = new byte[0x20];

            Curve25519.verify(z, s, r, publicKey);

            MessageDigest messageDigest = null;

            try {
                messageDigest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) { /* empty */ }

            assert messageDigest != null;

            byte[] m = messageDigest.digest(message);

            messageDigest.update(m);
            byte[] r2 = messageDigest.digest(z);

            isValid = Arrays.equals(r, r2);
        }

        return isValid;
    }

    public static long longLsbFromBytes(byte[] bytes) {
        BigInteger bi = new BigInteger(1, new byte[] {bytes[7], bytes[6], bytes[5], bytes[4], bytes[3], bytes[2], bytes[1], bytes[0]});
        return bi.longValue();
    }

    public static byte[] publicKeyFromPrivateKey(byte[] privateKey) {
        byte[] publicKey = new byte[0x20];

        Curve25519.keygen(publicKey, null, privateKey);

        return publicKey;
    }

    public static String hexStringFromBytes(byte[] bytes) {

        if (bytes == null || bytes.length == 0) {
            return null;
        }

        return String.format("%0" + (bytes.length << 1) + "x", new BigInteger(1, bytes));
    }

    public static byte[] bytesFromHexString(String s) {

        if (s == null || s.length() < 2) {
            return null;
        }

        int len = s.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }

        return data;
    }

    public static byte[] getHash(byte[] data, String algorithm) {

        if (algorithm == null || algorithm.length() == 0 || data == null) {
            return null;
        }

        MessageDigest messageDigest;

        try {
            messageDigest = MessageDigest.getInstance(algorithm);
        } catch ( NoSuchAlgorithmException e) {
            return null;
        }

        return messageDigest.digest(data);
    }
}
