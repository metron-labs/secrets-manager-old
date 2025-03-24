package com.keepersecurity.secretmanager.oracle.kv;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Supplier;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.Region;
import com.oracle.bmc.ConfigFileReader.ConfigFile;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimplePrivateKeySupplier;
import com.oracle.bmc.http.client.HttpProvider;
import com.oracle.bmc.http.client.jersey.JerseyHttpProvider;
import com.oracle.bmc.keymanagement.KmsCryptoClient;
import com.oracle.bmc.keymanagement.model.EncryptDataDetails;
import com.oracle.bmc.keymanagement.model.EncryptedData;
import com.oracle.bmc.keymanagement.model.EncryptDataDetails.EncryptionAlgorithm;
import com.oracle.bmc.keymanagement.requests.EncryptRequest;
import com.oracle.bmc.keymanagement.responses.EncryptResponse;


public class OracleKeyVaultConnector {

	 public OracleSessionConfig sessionConfig;

	public OracleKeyVaultConnector(OracleSessionConfig sessionconfig) {
		this.sessionConfig = sessionconfig;
	}
	
	public  AuthenticationDetailsProvider getprovider() throws IOException {
		
		ConfigFile config = ConfigFileReader.parse(sessionConfig.getConfigPath());
		System.out.println(config.get("tenancy"));
		System.out.println(config.get("user"));
		System.out.println(config.get("fingerprint"));
		
		Supplier<InputStream> privateKeySupplier = new SimplePrivateKeySupplier("/home/user1-metron/.oci/parag@metronlabs.com_2025-02-20T10_46_34.992Z.pem");
		AuthenticationDetailsProvider provider = SimpleAuthenticationDetailsProvider.builder()
				.tenantId(config.get("tenancy")).userId(config.get("user")).fingerprint(config.get("fingerprint"))
				.privateKeySupplier(privateKeySupplier).region(Region.US_ASHBURN_1).build();
		return provider;
	}
	
	private String getKeySpecType(String keyId) {
		return "SYMMETRIC_DEFAULT";
	}
	
	public boolean isSymmetricKey(String keyId) {
		if (Constants.SYMMETRIC_DEFAULT.equals(getKeySpecType(keyId))) {
			return true;
		}
		return false;
	}
	
	public byte[] encryptRSA(byte[] message) throws IOException {
		
		ClientConfiguration clientConfiguration = ClientConfiguration.builder().connectionTimeoutMillis(300000).build();
		HttpProvider httpProvider = new JerseyHttpProvider();
	
		KmsCryptoClient kmsCryptoClient = KmsCryptoClient.builder().endpoint(sessionConfig.getKmsEndpoint()).httpProvider(httpProvider)
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
		System.out.println("Encrypted Data: " + encryptedData.getCiphertext());
		return encryptedData.getCiphertext().getBytes();
	}
	
	public byte[] decrypt(byte[] message) throws IOException {
		return new byte[10];
	}
	
}



