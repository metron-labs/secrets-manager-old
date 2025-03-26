package com.keepersecurity.secretmanager.oracle.kv;

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



public class OracleKeyVaultConnector {
	public OracleSessionConfig sessionConfig;
	private String profile;

	public OracleKeyVaultConnector(OracleSessionConfig sessionconfig, String profile) {
		this.sessionConfig = sessionconfig;
		this.profile = profile;
	}
	
	public AuthenticationDetailsProvider getprovider() throws IOException {
		ConfigFile config = ConfigFileReader.parse(sessionConfig.getConfigPath());
		Supplier<InputStream> privateKeySupplier = new SimplePrivateKeySupplier(config.get("key_file"));
		AuthenticationDetailsProvider provider = SimpleAuthenticationDetailsProvider.builder()
				.tenantId(config.get("tenancy")).userId(config.get("user")).fingerprint(config.get("fingerprint"))
				.privateKeySupplier(privateKeySupplier).region(sessionConfig.getRegion()).build();
		return provider;
	}
	
	private KeyShape.Algorithm getKeySpecType(String keyId) throws IOException {
        AuthenticationDetailsProvider provider = new ConfigFileAuthenticationDetailsProvider(this.profile);
		KmsManagementClient client = KmsManagementClient.builder().endpoint(sessionConfig.getManagementEndpoint()).build(provider);
		GetKeyRequest getKeyRequest = GetKeyRequest.builder()
		.keyId(sessionConfig.getKeyId())
		.build();

        GetKeyResponse response = client.getKey(getKeyRequest);
		return response.getKey().getKeyShape().getAlgorithm();
	}
	
	public boolean isSymmetricKey(String keyId) throws IOException{
		if (KeyShape.Algorithm.Aes.equals(getKeySpecType(keyId))) {
			return true;
		}
		return false;
	}
	
	public byte[] encryptRSA(byte[] message) throws IOException {
		ClientConfiguration clientConfiguration = ClientConfiguration.builder().connectionTimeoutMillis(300000).build();
		HttpProvider httpProvider = new JerseyHttpProvider();
		KmsCryptoClient kmsCryptoClient = KmsCryptoClient.builder().endpoint(sessionConfig.getCryptoEndpoint()).httpProvider(httpProvider)
			.configuration(clientConfiguration).build(getprovider());
		String encodedData = Base64.getEncoder().encodeToString(message);
		EncryptDataDetails encryptDataDetails = EncryptDataDetails.builder()
				.encryptionAlgorithm(EncryptionAlgorithm.RsaOaepSha256)
				.keyId(sessionConfig.getKeyId())
				.keyVersionId(sessionConfig.getKeyVersionId())
				.plaintext(encodedData)
				.build();
 
		EncryptRequest encryptRequest = EncryptRequest.builder()
				.encryptDataDetails(encryptDataDetails)
				.build();

		EncryptResponse encryptResponse = kmsCryptoClient.encrypt(encryptRequest);
		EncryptedData encryptedData = encryptResponse.getEncryptedData();
		return encryptedData.getCiphertext().getBytes();
	}
	
	public byte[] decryptRSA(byte[] message) throws IOException {
	ClientConfiguration clientConfiguration = ClientConfiguration.builder().connectionTimeoutMillis(300000).build();
	HttpProvider httpProvider = new JerseyHttpProvider();
	KmsCryptoClient kmsCryptoClient = KmsCryptoClient.builder().endpoint(sessionConfig.getCryptoEndpoint()).httpProvider(httpProvider)
			.configuration(clientConfiguration).build(getprovider());
	
	DecryptDataDetails decryptDataDetails = DecryptDataDetails.builder().ciphertext(new String(message)).encryptionAlgorithm(DecryptDataDetails.EncryptionAlgorithm.RsaOaepSha256).keyId(sessionConfig.getKeyId()).keyVersionId(sessionConfig.getKeyVersionId()).build();
	DecryptRequest decryptRequest = DecryptRequest.builder()
			.decryptDataDetails(decryptDataDetails)
			.build();
	DecryptResponse decryptResponse = kmsCryptoClient.decrypt(decryptRequest);
	DecryptedData decryptedData = decryptResponse.getDecryptedData();
	return Base64.getDecoder().decode(decryptedData.getPlaintext());
	}
	
	public byte[] encryptAES(byte[] message) throws IOException {
		ClientConfiguration clientConfiguration = ClientConfiguration.builder().connectionTimeoutMillis(300000).build();
		HttpProvider httpProvider = new JerseyHttpProvider();
		KmsCryptoClient kmsCryptoClient = KmsCryptoClient.builder().endpoint(sessionConfig.getCryptoEndpoint()).httpProvider(httpProvider)
			.configuration(clientConfiguration).build(getprovider());
		String encodedData = Base64.getEncoder().encodeToString(message);

		EncryptDataDetails encryptDataDetails = EncryptDataDetails.builder()
				.encryptionAlgorithm(EncryptionAlgorithm.Aes256Gcm)
				.keyId(sessionConfig.getKeyId())
				.keyVersionId(sessionConfig.getKeyVersionId())
				.plaintext(encodedData)
				.build();
 
		EncryptRequest encryptRequest = EncryptRequest.builder()
				.encryptDataDetails(encryptDataDetails)
				.build();

		EncryptResponse encryptResponse = kmsCryptoClient.encrypt(encryptRequest);

		EncryptedData encryptedData = encryptResponse.getEncryptedData();
		return encryptedData.getCiphertext().getBytes();
	}

	public byte[] decryptAES(byte[] message) throws IOException{
		ClientConfiguration clientConfiguration = ClientConfiguration.builder().connectionTimeoutMillis(300000).build();
		HttpProvider httpProvider = new JerseyHttpProvider();
		KmsCryptoClient kmsCryptoClient = KmsCryptoClient.builder().endpoint(sessionConfig.getCryptoEndpoint()).httpProvider(httpProvider)
			.configuration(clientConfiguration).build(getprovider());
		
		DecryptDataDetails decryptDataDetails = DecryptDataDetails.builder()
				.ciphertext(new String(message))
				.encryptionAlgorithm(DecryptDataDetails.EncryptionAlgorithm.Aes256Gcm)
				.keyId(sessionConfig.getKeyId())
				.keyVersionId(sessionConfig.getKeyVersionId())
				.build();
		
		DecryptRequest decryptRequest = DecryptRequest.builder()
				.decryptDataDetails(decryptDataDetails)
				.build();
		
		DecryptResponse decryptResponse = kmsCryptoClient.decrypt(decryptRequest);
		DecryptedData decryptedData = decryptResponse.getDecryptedData();
		return Base64.getDecoder().decode(decryptedData.getPlaintext());
	}
}
