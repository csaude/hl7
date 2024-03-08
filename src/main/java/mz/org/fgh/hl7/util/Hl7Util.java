package mz.org.fgh.hl7.util;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.lang.StringUtils;

public class Hl7Util {
	
    private static final int SALT_LENGTH = 8; // Length of salt in bytes
	
	public static String getCurrentTimeStamp() {
		return new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date());
	}

	public static String listToString(List<String> locationsBySite) {
		String locations = StringUtils.join(locationsBySite, "','");
		locations = "'" + locations + "'";
		return locations;
	}
	
	public static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new Random().nextBytes(salt);
        return salt;
    }
        
	public static byte[] deriveKey(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Use OpenSSL's key derivation function (EVP_BytesToKey) to derive the key from the password and salt
        // OpenSSL uses a single iteration of MD5 hashing
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 1, 256); // 256-bit key
        return factory.generateSecret(spec).getEncoded();
    }
}
