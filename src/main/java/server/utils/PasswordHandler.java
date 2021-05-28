package server.utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

public class PasswordHandler {

    private static final int ITERATIONS = 65536;
    private static final int KEYLENGTH = 256;

    public PasswordHandler() {
        return;
    }

    public static String salt(){
        SecureRandom secureRandom = new SecureRandom();
        //generating random salt to hash our password
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hash(String clearTextPassword, String salt){
        char[] pass = clearTextPassword.toCharArray();
        byte[] byteSalt =  salt.getBytes();

        KeySpec keySpec =  new PBEKeySpec(clearTextPassword.toCharArray(), byteSalt, ITERATIONS, KEYLENGTH);

        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = factory.generateSecret(keySpec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        }catch( NoSuchAlgorithmException | InvalidKeySpecException e){
            e.printStackTrace();
        }
        return null;
    }

    public static boolean authenticate(String providedPassword, String hash, String salt){
        return hash(providedPassword, salt).equals(hash);
    }
}
