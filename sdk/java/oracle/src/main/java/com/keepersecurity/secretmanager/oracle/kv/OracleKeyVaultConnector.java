package com.keepersecurity.secretmanager.oracle.kv;

/*
*  _  __
* | |/ /___ ___ _ __  ___ _ _ (R)
* | ' </ -_) -_) '_ \/ -_) '_|
* |_|\_\___\___| .__/\___|_|
*              |_|
*
* Keeper Secrets Manager
* Copyright 2025 Keeper Security Inc.
* Contact: sm@keepersecurity.com
*/

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.function.Supplier;
import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.ConfigFileReader.ConfigFile;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimplePrivateKeySupplier;
import com.oracle.bmc.http.client.HttpProvider;
import com.oracle.bmc.http.client.jersey.JerseyHttpProvider;
import com.oracle.bmc.keymanagement.KmsCryptoClient;
import com.oracle.bmc.keymanagement.KmsManagementClient;
import com.oracle.bmc.keymanagement.model.DecryptDataDetails;
import com.oracle.bmc.keymanagement.model.DecryptedData;
import com.oracle.bmc.keymanagement.model.EncryptDataDetails;
import com.oracle.bmc.keymanagement.model.EncryptedData;
import com.oracle.bmc.keymanagement.model.KeyShape;
import com.oracle.bmc.keymanagement.model.EncryptDataDetails.EncryptionAlgorithm;
import com.oracle.bmc.keymanagement.requests.DecryptRequest;
import com.oracle.bmc.keymanagement.requests.EncryptRequest;
import com.oracle.bmc.keymanagement.requests.GetKeyRequest;
import com.oracle.bmc.keymanagement.responses.DecryptResponse;
import com.oracle.bmc.keymanagement.responses.EncryptResponse;
import com.oracle.bmc.keymanagement.responses.GetKeyResponse;


/**
 * This class is used to connect to Oracle KeyVault
 */
public class OracleKeyVaultConnector {
	public OracleSessionConfig sessionConfig;
	private String profile;

	public OracleKeyVaultConnector(OracleSessionConfig sessionconfig, String profile) {
		this.sessionConfig = sessionconfig;
		this.profile = profile;
	}

	/**
	 * This method return AuthenticationDetailsProvider
	 * @return Return Authentication provider 
	 * @throws IOException
	 */
	public AuthenticationDetailsProvider getprovider() throws IOException {
		ConfigFile config = ConfigFileReader.parse(sessionConfig.getConfigPath());
		Supplier<InputStream> privateKeySupplier = new SimplePrivateKeySupplier(config.get("key_file"));
		AuthenticationDetailsProvider provider = SimpleAuthenticationDetailsProvider.builder()
				.tenantId(config.get("tenancy")).userId(config.get("user")).fingerprint(config.get("fingerprint"))
				.privateKeySupplier(privateKeySupplier).region(sessionConfig.getRegion()).build();
		return provider;
	}

	/**
	 * This method returns KeyShape.Algorithm
	 * @param keyId: Key ID
	 * @return Return Key Sepc Type
	 * @throws IOException
	 */
	private KeyShape.Algorithm getKeySpecType(String keyId) throws IOException {
		AuthenticationDetailsProvider provider = new ConfigFileAuthenticationDetailsProvider(this.profile);
		KmsManagementClient client = KmsManagementClient.builder().endpoint(sessionConfig.getManagementEndpoint())
				.build(provider);
		GetKeyRequest getKeyRequest = GetKeyRequest.builder().keyId(sessionConfig.getKeyId()).build();

		GetKeyResponse response = client.getKey(getKeyRequest);
		return response.getKey().getKeyShape().getAlgorithm();
	}

	/**
	 * This method check Key is symmetric return true/false
	 * @param keyId: Key ID
	 * @return Return boolean true/false
	 * @throws IOException
	 */
	public boolean isSymmetricKey(String keyId) throws IOException {
		if (KeyShape.Algorithm.Aes.equals(getKeySpecType(keyId))) {
			return true;
		}
		return false;
	}

	/**
	 * This method encrypt using RSA
	 * @param message
	 * @return Return byte array
	 * @throws IOException
	 */
	public byte[] encryptRSA(byte[] message) throws IOException {
		ClientConfiguration clientConfiguration = ClientConfiguration.builder().connectionTimeoutMillis(300000).build();
		HttpProvider httpProvider = new JerseyHttpProvider();
		KmsCryptoClient kmsCryptoClient = KmsCryptoClient.builder().endpoint(sessionConfig.getCryptoEndpoint())
				.httpProvider(httpProvider).configuration(clientConfiguration).build(getprovider());
		String encodedData = Base64.getEncoder().encodeToString(message);
		EncryptDataDetails encryptDataDetails = EncryptDataDetails.builder()
				.encryptionAlgorithm(EncryptionAlgorithm.RsaOaepSha256).keyId(sessionConfig.getKeyId())
				.keyVersionId(sessionConfig.getKeyVersionId()).plaintext(encodedData).build();

		EncryptRequest encryptRequest = EncryptRequest.builder().encryptDataDetails(encryptDataDetails).build();

		EncryptResponse encryptResponse = kmsCryptoClient.encrypt(encryptRequest);
		EncryptedData encryptedData = encryptResponse.getEncryptedData();
		return encryptedData.getCiphertext().getBytes();
	}

	/**
	 * This method decrypt using RSA
	 * @param message
	 * @return The decrypted configuration as a String.
	 * @throws IOException
	 */
	public byte[] decryptRSA(byte[] message) throws IOException {
		ClientConfiguration clientConfiguration = ClientConfiguration.builder().connectionTimeoutMillis(300000).build();
		HttpProvider httpProvider = new JerseyHttpProvider();
		KmsCryptoClient kmsCryptoClient = KmsCryptoClient.builder().endpoint(sessionConfig.getCryptoEndpoint())
				.httpProvider(httpProvider).configuration(clientConfiguration).build(getprovider());

		DecryptDataDetails decryptDataDetails = DecryptDataDetails.builder().ciphertext(new String(message))
				.encryptionAlgorithm(DecryptDataDetails.EncryptionAlgorithm.RsaOaepSha256)
				.keyId(sessionConfig.getKeyId()).keyVersionId(sessionConfig.getKeyVersionId()).build();
		DecryptRequest decryptRequest = DecryptRequest.builder().decryptDataDetails(decryptDataDetails).build();
		DecryptResponse decryptResponse = kmsCryptoClient.decrypt(decryptRequest);
		DecryptedData decryptedData = decryptResponse.getDecryptedData();
		return Base64.getDecoder().decode(decryptedData.getPlaintext());
	}

	/**
	 * This method encrypt using AES
	 * @param message
	 * @return Return encrpted in byte 
	 * @throws IOException
	 */
	public byte[] encryptAES(byte[] message) throws IOException {
		ClientConfiguration clientConfiguration = ClientConfiguration.builder().connectionTimeoutMillis(300000).build();
		HttpProvider httpProvider = new JerseyHttpProvider();
		KmsCryptoClient kmsCryptoClient = KmsCryptoClient.builder().endpoint(sessionConfig.getCryptoEndpoint())
				.httpProvider(httpProvider).configuration(clientConfiguration).build(getprovider());
		String encodedData = Base64.getEncoder().encodeToString(message);

		EncryptDataDetails encryptDataDetails = EncryptDataDetails.builder()
				.encryptionAlgorithm(EncryptionAlgorithm.Aes256Gcm).keyId(sessionConfig.getKeyId())
				.keyVersionId(sessionConfig.getKeyVersionId()).plaintext(encodedData).build();

		EncryptRequest encryptRequest = EncryptRequest.builder().encryptDataDetails(encryptDataDetails).build();

		EncryptResponse encryptResponse = kmsCryptoClient.encrypt(encryptRequest);

		EncryptedData encryptedData = encryptResponse.getEncryptedData();
		return encryptedData.getCiphertext().getBytes();
	}

	/**
	 * This method decrypt using AES
	 * @param message
	 * @return Return decrypted value in byte array
	 * @throws IOException
	 */
	public byte[] decryptAES(byte[] message) throws IOException {
		ClientConfiguration clientConfiguration = ClientConfiguration.builder().connectionTimeoutMillis(300000).build();
		HttpProvider httpProvider = new JerseyHttpProvider();
		KmsCryptoClient kmsCryptoClient = KmsCryptoClient.builder().endpoint(sessionConfig.getCryptoEndpoint())
				.httpProvider(httpProvider).configuration(clientConfiguration).build(getprovider());

		DecryptDataDetails decryptDataDetails = DecryptDataDetails.builder().ciphertext(new String(message))
				.encryptionAlgorithm(DecryptDataDetails.EncryptionAlgorithm.Aes256Gcm).keyId(sessionConfig.getKeyId())
				.keyVersionId(sessionConfig.getKeyVersionId()).build();

		DecryptRequest decryptRequest = DecryptRequest.builder().decryptDataDetails(decryptDataDetails).build();

		DecryptResponse decryptResponse = kmsCryptoClient.decrypt(decryptRequest);
		DecryptedData decryptedData = decryptResponse.getDecryptedData();
		return Base64.getDecoder().decode(decryptedData.getPlaintext());
	}
}
