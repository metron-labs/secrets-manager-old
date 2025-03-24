# Oracle Key Management
Keeper Secrets Manager integrates with **Oracle Key Vault Management Service (OCI KMS)** to provide protection for Keeper Secrets Manager configuration files. With this integration, you can secure connection details on your machine while leveraging Keeper's **zero-knowledge encryption** for all your secret credentials.

## Features
* Encrypt and decrypt your Keeper Secrets Manager configuration files using **OCI KMS**.
* Protect against unauthorized access to your **Secrets Manager connections**.
* Requires only minor code modifications for immediate protection. Works with all Keeper Secrets Manager **Java/Kotlin SDK** functionality.


## Prerequisites

* Supports the Java/Kotlin Secrets Manager SDK.
* Requires Oracle packages: oci-java-sdk-keymanagement, oci-java-sdk-common and oci-java-sdk-common-httpclient-jersey.
* OCI KMS Key needs `ENCRYPT` and `DECRYPT` permissions.

# Download and Installation
**Install With Gradle or Maven**
	
	
 <details>
  <summary>Gradle</summary>
  
  ```
  repositories {
    mavenCentral()
}

dependencies {
    implementation 'com.keepersecurity.secrets-manager:core:17.0.0+'
    implementation("org.bouncycastle:bc-fips:1.0.2.4")
    implementation("com.oracle.oci.sdk:oci-java-sdk-keymanagement:3.60.0")
    implementation("com.oracle.oci.sdk:oci-java-sdk-common-httpclient-jersey:3.60.0") 
    implementation("com.oracle.oci.sdk:oci-java-sdk-common:3.60.0")
    implementation("com.google.code.gson:gson:2.12.1")
}
```

  </details> 
  <details> <summary>Maven</summary>

 ```
 <dependency>
  <groupId>com.keepersecurity.secrets-manager</groupId>
  <artifactId>core</artifactId>
  <version>[17.0.0,)</version>
</dependency>
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bc-fips</artifactId>
    <version>1.0.2.4</version>
</dependency>
<dependency>
    <groupId>com.oracle.oci.sdk</groupId>
    <artifactId>oci-java-sdk-keymanagement</artifactId>
    <version>3.60.0</version>
    <scope>compile</scope>
</dependency>
<dependency>
    <groupId>com.oracle.oci.sdk</groupId>
    <artifactId>oci-java-sdk-common-httpclient-jersey</artifactId>
    <version>3.60.0</version>
</dependency>
<dependency>
    <groupId>com.oracle.oci.sdk</groupId>
    <artifactId>oci-java-sdk-common</artifactId>
    <version>3.60.0</version>
</dependency>
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.12.1</version>
</dependency>

```
   </details> 
  
  
** Configure Oracle Connection **

Initilaizes OracleKeyValueStorage


        
Configuration variables can be provided as 

```
    import com.keepersecurity.secretmanager.oracle.kv.OracleSessionConfig;
    
    String configPath = <~/.oci/config>
    String kmsEndpoint="<OCI KMS ENDPOINT>" 
    String vaultId="<OCI VAULT ID>"
    String keyId="<OCI KEY ID>"
    String keyVersionId="<OCI KEY VERSION>"
    OracleSessionConfig sessionConfig = public OracleSessionConfig(configPath, kmsEndpoint, vaultId, keyId, keyVersionId) 
```

An access key using the `OracleSessionConfig` data class and providing `configPath`,`kmsEndpoint` ,`vaultId`, `keyId` and `keyVersionId` variables.

You will need an ~/.oci/config to use the OCI KMS Integration.


**Add Oracle Key Vault Storage to Your Code**

Now that the Oracle connection has been configured, you need to tell the Secrets Manager SDK to utilize the KMS as storage.

To do this, use OracleKeyValueStorage as your Secrets Manager storage in the SecretsManager constructor.

The storage will require the name of the Secrets Manager configuration file which will be encrypted by Oracle Key Vault.

```
		import com.keepersecurity.secretmanager.oracle.kv.OracleSessionConfig;
		import  com.keepersecurity.secretmanager.oracle.kv.OracleKeyValueStorage;
		import com.keepersecurity.secretsManager.core.SecretsManagerOptions;
		
	    String configFileLocation = "<KSM-Config.json>";
		try{
		  	// created instance OracleSessionConfig with oracle configuration details mentioned above
		  	
	  		OracleKeyValueStorage STORAGE =  OracleKeyValueStorage.getInternalStorage(configFileLocation, sessionConfig);
			Security.addProvider(BouncyCastleFipsProvider())
			SecretsManagerOptions OPTIONS = new SecretsManagerOptions(STORAGE);
	    	 //getSecrets(OPTIONS)
		}catch (Exception e) {
  			  System.out.println(e.getMessage());
 		}
			
```
