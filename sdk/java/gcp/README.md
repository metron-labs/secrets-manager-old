# GCP KMS Integration

Protect Secrets Manager connection details with GCP KMS

Keeper Secrets Manager integrates with AWS KMS in order to provide protection for Keeper Secrets Manager configuration files.  With this integration, you can protect connection details on your machine while taking advantage of Keeper's zero-knowledge encryption of all your secret credentials.
Features

* Encrypt and Decrypt your Keeper Secrets Manager configuration files with GCP KMS (Symmetric/Asymmetric Key) 
* Protect against unauthorized access to your Secrets Manager connections
* Requires only minor changes to code for immediate protection.  Works with all Keeper Secrets Manager Java/Kotlin SDK functionality

# Prerequisites

* Supports the Java/Kotlin Secrets Manager SDK.
* Requires GCP package: google-cloud-kms.
* Key needs `Encrypt` and `Decrypt` permissions.

# Set Up Authentication
Before using Google Cloud APIs, you must authenticate your Python application. The easiest way to do this is by setting up a service account and downloading a service account key file (JSON). This service account should have the appropriate permissions to interact with the KMS API.

*Go to the Google Cloud Console. *Navigate to IAM & Admin → Service Accounts. *Create a new service account or select an existing one. *Assign the necessary permissions (e.g., Cloud KMS Admin, or Cloud KMS CryptoKey Encrypter/Decrypter). *Download the private key JSON file. Then, set the GOOGLE_APPLICATION_CREDENTIALS environment variable to point to the path of the downloaded key file:

export GOOGLE_APPLICATION_CREDENTIALS="/path/to/your-service-account-file.json"

# Permissions
Make sure that the service account you're using has appropriate permissions. Typically, you'll need:

Cloud KMS CryptoKey Encrypter/Decrypter permission for encrypting and decrypting data.
Cloud KMS Key Viewer permission to fetch key details. You can assign these roles via IAM in the Google Cloud Console or using gcloud.

# Download and Installation

**Install With Gradle or Maven**
	
	
 <details>
  <summary>Gradle</summary>
  
  ```
  repositories {
    mavenCentral()
}

dependencies {
	implementation("com.keepersecurity.secrets-manager:core:17.0.0")
	implementation ("com.google.cloud:google-cloud-kms:2.62.0")
	    
	implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
	implementation("com.fasterxml.jackson.core:jackson-core:2.18.2")
	implementation("com.google.code.gson:gson:2.12.1")
    implementation("org.slf4j:slf4j-api:1.7.32"){
        exclude("org.slf4j:slf4j-log4j12")
    }
	implementation("ch.qos.logback:logback-classic:1.2.6")
	implementation("ch.qos.logback:logback-core:1.2.6")
	implementation("org.bouncycastle:bc-fips:1.0.2.4")
}
```

  </details> 
  <details> <summary>Maven</summary>

 ```
 
 <!-- KMS-core -->
 	<dependency>
  		<groupId>com.keepersecurity.secrets-manager</groupId>
  		<artifactId>core</artifactId>
  		<version>[17.0.0,)</version>
	</dependency>

<!-- gcp-kms -->

	 <dependency>
    	<groupId>com.google.cloud</groupId>
   		 <artifactId>google-cloud-kms</artifactId>
    	<version>2.62.0</version>
	 </dependency>
		
		<!--gson -->
		<dependency>
		    <groupId>com.google.code.gson</groupId>
		    <artifactId>gson</artifactId>
		    <version>2.12.1</version>
		</dependency>

		<!--jackson-core -->
		<dependency>
			<groupId>com.fasterxml.jackson.core</groupId>
			<artifactId>jackson-core</artifactId>
			<version>2.18.2</version>
		</dependency>
		
		<!--jackson-databind -->
		<dependency>
			<groupId>com.fasterxml.jackson.core</groupId>
			<artifactId>jackson-core</artifactId>
			<version>2.18.2</version>
		</dependency>
		
		<!-- slf4j-api -->
		<dependency>
			<groupId>org.slf4j</groupId>
			<artifactId>slf4j-api</artifactId>
			<version>1.7.32</version>
			<scope>runtime</scope>
		</dependency>

		<!-- logback-classic -->
		<dependency>
			<groupId>ch.qos.logback</groupId>
			<artifactId>logback-classic</artifactId>
			<version>1.2.6</version>
			<scope>compile</scope>
		</dependency>

		<!-- logback-core -->
		<dependency>
			<groupId>ch.qos.logback</groupId>
			<artifactId>logback-core</artifactId>
			<version>1.2.6</version>
			<scope>compile</scope>
		</dependency>
		
		<!-- bc-fips -->
		<dependency>
    		<groupId>org.bouncycastle</groupId>
    		<artifactId>bc-fips</artifactId>
    		<version>1.0.2.4</version>
		</dependency>
		

```
   </details> 
  
  
** Configure GCP Connection **


Configuration variables can be provided as 

 By default the @aws-sdk library will utilize the default connection session setup with the AWS CLI with the aws configure command.  If you would like to specify the connection details, the two configuration files located at `~/.aws/config` and ~/.aws/credentials can be manually edited.

See the AWS documentation for more information on setting up an AWS session: https://docs.aws.amazon.com/cli/latest/reference/configure/

Alternatively, configuration variables can be provided explicitly as an access key using the AwsSessionConfig data class and providing `awsAccessKeyId`,  `awsSecretAccessKey` and  `region` variables.

You will need an AWS Access Key to use the AWS KMS integration.

For more information on AWS Access Keys see the AWS documentation: https://aws.amazon.com/premiumsupport/knowledge-center/create-access-key/
        
        
Initializes AwsKeyValueStorage

```
     	String projectId = "<GCP project id>";
	 	String location = "<GCP cloud Location>";
		String keyRing = "<Key Ring>>";
		String keyId = "<Key ID>";  //Symmetric or Asymmetric
		String keyVersion ="<Key Version>";
    	GcpSessionConfig sessionConfig = new GcpSessionConfig(projectId, location, keyRing, keyId, keyVersion);
```

An access key using the `GcpSessionConfig` data class and providing `projectId`,`location`, `keyRing`, `keyId` and `keyVersion` variables.


For more information on Azure App Directory App registration and Permissions see the Azure documentation: https://learn.microsoft.com/en-us/azure/key-vault/general/authentication

**Add AWS KMS Storage to Your Code**

Now that the AWS connection has been configured, you need to tell the Secrets Manager SDK to utilize the KMS as storage.

To do this, use AwsKeyValueStorage as your Secrets Manager storage in the SecretsManager constructor.

The storage will require an AWS Key ID, as well as the name of the Secrets Manager configuration file which will be encrypted by AWS KMS. Below is the sample Test class

```
import static com.keepersecurity.secretsManager.core.SecretsManager.initializeStorage;		

import com.keepersecurity.secretsManager.core.KeeperRecord;
import com.keepersecurity.secretsManager.core.KeeperRecordData;
import com.keepersecurity.secretsManager.core.KeeperSecrets;
import com.keepersecurity.secretsManager.core.SecretsManager;
import com.keepersecurity.secretsManager.core.SecretsManagerOptions;
import com.keepersecurity.secretsmanager.gcp.GcpKeyValueStorage;
import com.keepersecurity.secretsmanager.gcp.GcpSessionConfig;
import java.security.Security;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;

		class Test{
		   public static void main(String args[]){
		  			
				  	String oneTimeToken = "[One Time Token]";
					String projectId = "<GCP project id>";
					String location = "<GCP cloud Location>";
					String keyRing = "<Key Ring>>";
					String keyId = "<Key ID>";  //Symmetric or Asymmetric
					String keyVersion ="<Key Version>";
				    String configFileLocation = "client_config_test.json";
				   Security.addProvider(new BouncyCastleFipsProvider());
				try{
		    			GcpSessionConfig sessionConfig = new GcpSessionConfig(projectId, location, keyRing, keyId, keyVersion);
						GcpKeyValueStorage storage = new GcpKeyValueStorage(configFileLocation, sessionConfig);
						initializeStorage(storage, oneTimeToken);
           			SecretsManagerOptions options = new SecretsManagerOptions(storage);	
			    	 	//getSecrets(OPTIONS);
				}catch (Exception e) {
		  			  System.out.println(e.getMessage());
		 		}
	 	   }
	 	}
			
```
