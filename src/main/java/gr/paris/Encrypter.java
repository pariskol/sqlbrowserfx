package gr.paris;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;

import gr.sqlfx.factories.DialogFactory;

public class Encrypter {

	static KeysetHandle keysetHandle;

	static {
		
		String keysetFilename = "my_keyset.json";
//	    CleartextKeysetHandle.write(keysetHandle, JsonKeysetWriter.withFile(
//	        new File(keysetFilename)));

		try {
			AeadConfig.register();
			keysetHandle = CleartextKeysetHandle.read(JsonKeysetReader.withFile(new File(keysetFilename)));
		} catch (GeneralSecurityException | IOException e) {
			DialogFactory.createErrorDialog(e);
		}
	}

	public static byte[] encrypt(String plaintext) throws GeneralSecurityException {
		// 2. Get the primitive.
		Aead aead = keysetHandle.getPrimitive(Aead.class);
		byte[] ciphertext = aead.encrypt(plaintext.getBytes(), new byte[0]);
//	    return new String(ciphertext, StandardCharsets.UTF_8);
		return ciphertext;
	}

	public static String decrypt(byte[] ciphertext) throws GeneralSecurityException {
		Aead aead = keysetHandle.getPrimitive(Aead.class);
		byte[] decrypted = aead.decrypt(ciphertext, new byte[0]);
		return new String(decrypted, StandardCharsets.UTF_8);
	}

	public static void tink() throws GeneralSecurityException, IOException {
//		 // Generate the key material...
//	    KeysetHandle keysetHandle = KeysetHandle.generateNew(
//	        AeadKeyTemplates.AES128_GCM);
//
//	    // and write it to a file.
		String keysetFilename = "my_keyset.json";
//	    CleartextKeysetHandle.write(keysetHandle, JsonKeysetWriter.withFile(
//	        new File(keysetFilename)));

		KeysetHandle keysetHandle = CleartextKeysetHandle.read(JsonKeysetReader.withFile(new File(keysetFilename)));

		// 2. Get the primitive.
		Aead aead = keysetHandle.getPrimitive(Aead.class);

		// 3. Use the primitive to encrypt a plaintext,
		byte[] ciphertext = aead.encrypt("yoyoyo".getBytes(), new byte[0]);

		// ... or to decrypt a ciphertext.
		byte[] decrypted = aead.decrypt(ciphertext, new byte[0]);
		System.out.println(new String(decrypted, StandardCharsets.UTF_8));
	}
}
