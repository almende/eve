package com.almende.util;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.jivesoftware.smack.util.Base64;

/**
 * Utility to encrypt text/passwords
 * 
 * WARNING: NOT SAFE TO USE IN A PRODUCTION ENVIRONMENT!
 * Why? Well, currently the master password is put in the code...
 * 
 * Sources:
 * http://docs.oracle.com/javase/1.4.2/docs/guide/security/jce/JCERefGuide.html#PBEEx
 * http://stackoverflow.com/questions/1132567/encrypt-password-in-configuration-files-java
 */
public final class EncryptionUtil {
	
	private EncryptionUtil(){};
	// master password
	// FIXME: do not store the master password in the code
	private static final char[] P = ("This is our secret master p......d, " +
			"which should definetely NOT be stored in the code!").toCharArray();
	
	// salt
    private static final byte[] S = {
        (byte)0xc7, (byte)0x73, (byte)0x21, (byte)0x8c,
        (byte)0x7e, (byte)0xc8, (byte)0xee, (byte)0x99
    };

    // Iteration count
    private static final int C = 20;

	/**
	 * Encrypt a string
	 * @param text
	 * @return encryptedText
	 * @throws InvalidKeyException
	 * @throws InvalidAlgorithmParameterException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 * @throws NoSuchPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws UnsupportedEncodingException
	 */
	public static String encrypt(String text) 
			throws InvalidKeyException, InvalidAlgorithmParameterException, 
			NoSuchAlgorithmException, InvalidKeySpecException, 
			NoSuchPaddingException, IllegalBlockSizeException, 
			BadPaddingException, UnsupportedEncodingException {
	    PBEParameterSpec pbeParamSpec = new PBEParameterSpec(S, C);
	    PBEKeySpec pbeKeySpec = new PBEKeySpec(P);
	    SecretKeyFactory keyFac = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
	    SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);

	    Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
	    pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);

	    byte[] encryptedText = pbeCipher.doFinal(text.getBytes("UTF-8"));
	    return Base64.encodeBytes(encryptedText);	    
	}
	
	/**
	 * Decrypt an encrypted string
	 * @param encryptedText
	 * @return text
	 * @throws InvalidKeyException
	 * @throws InvalidAlgorithmParameterException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 * @throws NoSuchPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws UnsupportedEncodingException
	 */
	public static String decrypt(String encryptedText)
			throws InvalidKeyException, InvalidAlgorithmParameterException, 
			NoSuchAlgorithmException, InvalidKeySpecException, 
			NoSuchPaddingException, IllegalBlockSizeException, 
			BadPaddingException, UnsupportedEncodingException {
		PBEParameterSpec pbeParamSpec = new PBEParameterSpec(S, C);
	    PBEKeySpec pbeKeySpec = new PBEKeySpec(P);
	    SecretKeyFactory keyFac = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
	    SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);

	    Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
	    pbeCipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);

	    byte[] text = pbeCipher.doFinal(Base64.decode(encryptedText));
	    return new String(text, "UTF-8");
	}
}