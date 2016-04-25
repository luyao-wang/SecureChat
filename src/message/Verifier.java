package message;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;


/**
 * Used by both Client and Server to encrypt and decrypt messages.
 * Since the process is shared it was less redundant to create a class for these
 * common processes.
 * By using the shared Symmetric-/SecretKey SealedObjects are encrypted and decrypted.
 * Used in combination with the Cipher 'cipherSecretKey'.
 * The PrivateKey is used together with the Signature to create SignedObjects. 
 * The Signature is also used to validated SignedObjects but with the PublicKey.
 */
public class Verifier {

	private SecretKey secretKey;
	private PrivateKey privateKey;
	private Cipher cipherSecretKey;
	private PublicKey publicKey;
	private Signature signature;
	
	public Verifier(SecretKey secKey, Cipher c, PublicKey pubKey, PrivateKey priKey, Signature sig){
		this.secretKey = secKey;
		this.privateKey = priKey;
		this.cipherSecretKey = c;
		this.publicKey = pubKey;
		this.signature = sig;
	}
	
	/**
	 * Creates a SignedObject from the given Message.
	 * @param m is the Message to sign.
	 * @return a SignedObject.
	 */
	public SignedObject createSignedObject(Message m) 
			throws InvalidKeyException, SignatureException, IOException{
		SignedObject signedobj = new SignedObject(m, privateKey, signature);
		return signedobj;
	}// createSignedObject end
	
	
	/**
	 * Creates a SignedObject from the given DisconnectMessage.
	 * @param m is the Message to sign.
	 * @return a SignedObject.
	 */
	public SignedObject createSignedObject(DisconnectMessage m) 
			throws InvalidKeyException, SignatureException, IOException{
		SignedObject signedobj = new SignedObject(m, privateKey, signature);
		return signedobj;
	}// createSignedObject end
	
	
	/**
	 * Creates a SignedObject from the given UserListMessage.
	 * @param m is the Message to sign.
	 * @return a SignedObject.
	 */
	public SignedObject createSignedObject(UserListMessage m) 
			throws InvalidKeyException, SignatureException, IOException{
		SignedObject signedobj = new SignedObject(m, privateKey, signature);
		return signedobj;
	}// createSignedObject end
	
	
	/**
	 * Converts a SignedObject into a Object.
	 * @param so is the SIgnedObject that should be converted to an Object.
	 * @return an Object.
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public Object convertSignedObject(SignedObject so) 
			throws ClassNotFoundException, IOException{
		return so.getObject();
	}// convertSignedObject end
	
	
	/**
	 * Validates a SignedObject.
	 * @param so is the SignedObject that should be validated.
	 * @return true if the signature in this SignedObject is the valid 
	 * signature for the object stored inside.
	 * @throws InvalidKeyException
	 * @throws SignatureException
	 */
	public boolean validateSignedObject(SignedObject so) 
			throws InvalidKeyException, SignatureException{
		return so.verify(publicKey, signature);
	}// validateSignedObject end
	
	
	/**
	 * Creates a SealedObject using the Cipher and shared Symmetric-/SecretKey.
	 * @param signed
	 * @return a SealdObject.
	 * @throws IllegalBlockSizeException
	 * @throws IOException
	 * @throws InvalidKeyException
	 */
	public SealedObject createSealedObject(SignedObject signed) 
			throws IllegalBlockSizeException, IOException, InvalidKeyException{
		cipherSecretKey.init(Cipher.ENCRYPT_MODE, secretKey);
		return new SealedObject(signed, cipherSecretKey);
	}// createSealedObject end
	
	
	/**
	 * Decrypts a SealedObject using the cipher with the shared Symmetric-/SecretKey.
	 * @param sealed is the SealedObject that should be decrypted.
	 * @return the SignedObject contained within the SealedObject.
	 * @throws InvalidKeyException
	 * @throws ClassNotFoundException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws IOException
	 */
	public SignedObject convertSealedObject(SealedObject sealed) 
			throws InvalidKeyException, ClassNotFoundException, IllegalBlockSizeException, 
			BadPaddingException, IOException{ 
		cipherSecretKey.init(Cipher.DECRYPT_MODE, secretKey);
		return (SignedObject) sealed.getObject(cipherSecretKey);
		
	}// convertSealedObject end
	
	
	
	
}
