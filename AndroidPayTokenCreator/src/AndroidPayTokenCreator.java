import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;


public class AndroidPayTokenCreator {

	/* from Google sample code*/
	/** Utility for decrypting encrypted network tokens as per Android Pay InApp 
	spec. */ 
	  static final String SECURITY_PROVIDER = "BC"; 
	  static final Charset DEFAULT_CHARSET = Charset.forName("UTF8"); 
	  static final String ASYMMETRIC_KEY_TYPE = "EC"; 
	  static final String KEY_AGREEMENT_ALGORITHM = "ECDH"; 
	  /** OpenSSL name of the NIST P­126 Elliptic Curve */ 
	  static final String EC_CURVE = "prime256v1"; 
	  static final String SYMMETRIC_KEY_TYPE = "AES"; 
	  static final String SYMMETRIC_ALGORITHM = "AES/CTR/NoPadding"; 
	  static final byte[] SYMMETRIC_IV = Hex.decode("00000000000000000000000000000000"); 
	  static final int SYMMETRIC_KEY_BYTE_COUNT = 16; 
	  static final String MAC_ALGORITHM = "HmacSHA256"; 
	  static final int MAC_KEY_BYTE_COUNT = 16; 
	  static final byte[] HKDF_INFO = "Android".getBytes(DEFAULT_CHARSET);
	  static final byte[] HKDF_SALT = null /* equivalent to a zeroed salt of hashLen */;
	  /* end of Google sample code*/
    
	public static String createTokenWithPublicKey(String publicKeyString, String message) throws Exception{
		PublicKey publicKey;
		byte[] publicKeyBytes = Base64.decode(publicKeyString);
		byte[] ephemeralPublicKeyBytes; 
		byte[] encryptedMessageLocal; 
		byte[] tag;

		try { 
			KeyFactory asymmetricKeyFactory = 
							KeyFactory.getInstance(ASYMMETRIC_KEY_TYPE, SECURITY_PROVIDER); 
/*			Only works for the normal Public Key format not the uncompressed point format	
* 			X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
			publicKey = asymmetricKeyFactory.generatePublic( publicKeySpec);*/
			

			ECParameterSpec asymmetricKeyParams = generateECParameterSpec(); 
			publicKey = asymmetricKeyFactory.generatePublic(new ECPublicKeySpec( ECPointUtil.decodePoint(asymmetricKeyParams.getCurve(), 
			                                                                                             publicKeyBytes), 
				                                                                                         asymmetricKeyParams));
				
				
		} catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException e1) {
			System.err.println("Failed to create PKCS8Encoded Public Key from keybytes");
			throw e1;
		}

		// Create ephemerals
		KeyPair ephemerals = null;
		ECParameterSpec asymmetricKeyParams = generateECParameterSpec(); 
		try{

			KeyPairGenerator gen = KeyPairGenerator.getInstance(ASYMMETRIC_KEY_TYPE, SECURITY_PROVIDER);
			gen.initialize(asymmetricKeyParams);//set the curve
			ephemerals = gen.generateKeyPair();

		}catch(NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException  e){
			System.err.println("Failed to create Public Key from ephemeralPublicKeyBytes");
			throw e;
		}
		// Deriving shared secret. 
		byte[] sharedSecret = null;
		try{
			KeyAgreement keyAgreement = KeyAgreement.getInstance(KEY_AGREEMENT_ALGORITHM, SECURITY_PROVIDER); 
			keyAgreement.init(ephemerals.getPrivate()); 
			keyAgreement.doPhase(publicKey, true);
			sharedSecret = keyAgreement.generateSecret();
		}catch(NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException  e){
			System.err.println("Failed to create sharedSecret");
			throw e;
		}
		ECPublicKey ecPublicKey = (ECPublicKey)ephemerals.getPublic();
		ephemeralPublicKeyBytes = toUncompressedPointsOnly(ecPublicKey);
		// Deriving encryption and mac keys. 
		HKDFBytesGenerator hkdfBytesGenerator = new HKDFBytesGenerator(new SHA256Digest()); 
		byte[] khdfInput = ByteUtils.concatenate(ephemeralPublicKeyBytes, sharedSecret); 
		hkdfBytesGenerator.init(new HKDFParameters(khdfInput, HKDF_SALT, HKDF_INFO)); 
		byte[] encryptionKey = new byte[SYMMETRIC_KEY_BYTE_COUNT]; 
		hkdfBytesGenerator.generateBytes(encryptionKey, 0, SYMMETRIC_KEY_BYTE_COUNT); 
		byte[] macKey = new byte[MAC_KEY_BYTE_COUNT]; 
		hkdfBytesGenerator.generateBytes(macKey, 0, MAC_KEY_BYTE_COUNT); 

		// Decrypting the message. 
		try{
			Cipher cipher = Cipher.getInstance(SYMMETRIC_ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, SYMMETRIC_KEY_TYPE), new IvParameterSpec(SYMMETRIC_IV)); 
			encryptedMessageLocal = cipher.doFinal(message.getBytes(DEFAULT_CHARSET)); 
		} catch ( NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException 
						| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) { 
			System.err.println("Failed to encrypt ");
			throw e;
		}

		// Verifying Message Authentication Code (aka mac/tag) 
		try{
			Mac macGenerator = Mac.getInstance(MAC_ALGORITHM, SECURITY_PROVIDER); 
			macGenerator.init(new SecretKeySpec(macKey, MAC_ALGORITHM)); 
			tag = macGenerator.doFinal(encryptedMessageLocal); 
		}catch(NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException  e){
			System.err.println("Failed to generate the MAC");
			throw e;
		}


		return "{" 
		+ "\"encryptedMessage\":\"" + Base64.toBase64String(encryptedMessageLocal) + "\"," 
		+ "\"ephemeralPublicKey\":\"" + Base64.toBase64String(ephemeralPublicKeyBytes) + "\"," 
		+ "\"tag\":\"" +  Base64.toBase64String(tag) + "\"}";  
	}
	
	public static ECNamedCurveSpec generateECParameterSpec() { 
	    ECNamedCurveParameterSpec bcParams = 
		    ECNamedCurveTable.getParameterSpec(EC_CURVE); 
	    ECNamedCurveSpec params = new ECNamedCurveSpec(bcParams.getName(), 
		    bcParams.getCurve(), 
		    bcParams.getG(), bcParams.getN(), bcParams.getH(), 
		    bcParams.getSeed()); 
	    return params; 
	}

	public static byte[] toUncompressedPointsOnly(final ECPublicKey publicKey) {
	    int keySize = (publicKey.getParams().getOrder().bitLength() + Byte.SIZE - 1)
		    / Byte.SIZE;
	    byte[] points =  new byte[1 + 2 * keySize];
	    points[0] = (byte) 0x04;
	    stripFillByteAndZeroPad( publicKey.getW().getAffineX().toByteArray(),keySize, points, 1);
	    stripFillByteAndZeroPad( publicKey.getW().getAffineY().toByteArray(),keySize, points, keySize + 1);
	    return points;
	}

	public static void stripFillByteAndZeroPad(byte[] arrayToStripAndPad, int maxLength, byte[] arrayToFill, int offset){
	    if(arrayToStripAndPad.length == maxLength + 1 && arrayToStripAndPad[0] == 0){//there is a fill byte
		System.arraycopy(arrayToStripAndPad, 1, arrayToFill, offset, maxLength);

	    }
	    else if(arrayToStripAndPad.length > maxLength ){//not a valid point
		throw new IllegalStateException("EC point coordinate value is too large");
	    }
	    else if(arrayToStripAndPad.length == maxLength){
		System.arraycopy(arrayToStripAndPad, 0, arrayToFill, offset, maxLength);

	    }
	    else System.arraycopy(arrayToStripAndPad, 0, arrayToFill, offset + maxLength - arrayToStripAndPad.length, arrayToStripAndPad.length);
	}
	
	public static void main(String[] args) throws Exception{
	    Security.addProvider(new BouncyCastleProvider());
	    String publicKey = "BLYg0ZZSEJ0XyG3C60PaJK/Z9b1YitDHChpS11mzpT9hYqHVGuoNbB94+NE4Uxj7laynDxqk0xvynfGHfrruP8w=";
	    String creditCardData = "secretData";
	    System.out.println(createTokenWithPublicKey(publicKey, creditCardData));
	}

}
