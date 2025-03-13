# AWS KMS Integration

Protect Secrets Manager connection details with AWS KMS

Keeper Secrets Manager integrates with AWS KMS in order to provide protection for Keeper Secrets Manager configuration files.  With this integration, you can protect connection details on your machine while taking advantage of Keeper's zero-knowledge encryption of all your secret credentials.
Features

* Encrypt and Decrypt your Keeper Secrets Manager configuration files with AWS KMS (Symmetric/Asymmetric Key) 
* Protect against unauthorized access to your Secrets Manager connections
* Requires only minor changes to code for immediate protection.  Works with all Keeper Secrets Manager Java/Kotlin SDK functionality

# Prerequisites

* Supports the Java/Kotlin Secrets Manager SDK.
* Requires Aws packages: awssdk-auth and aaswdsdk-kms.
* Key needs `Encrypt` and `Decrypt` permissions.

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
	implementation ("software.amazon.awssdk:kms:2.20.28")
    implementation ("software.amazon.awssdk:auth:2.20.28")
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

<!-- aws-kms -->
		<dependency>
			<groupId>software.amazon.awssdk</groupId>
			<artifactId>kms</artifactId>
			<version>2.20.28</version>
		</dependency>
			
		<!-- aws-auth -->
		<dependency>
			<groupId>software.amazon.awssdk</groupId>
			<artifactId>auth</artifactId>
			<version>2.20.28</version>
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
  
  
** Configure AWS Connection **


Configuration variables can be provided as 

 By default the @aws-sdk library will utilize the default connection session setup with the AWS CLI with the aws configure command.  If you would like to specify the connection details, the two configuration files located at `~/.aws/config` and ~/.aws/credentials can be manually edited.

See the AWS documentation for more information on setting up an AWS session: https://docs.aws.amazon.com/cli/latest/reference/configure/

Alternatively, configuration variables can be provided explicitly as an access key using the AwsSessionConfig data class and providing `awsAccessKeyId`,  `awsSecretAccessKey` and  `region` variables.

You will need an AWS Access Key to use the AWS KMS integration.

For more information on AWS Access Keys see the AWS documentation: https://aws.amazon.com/premiumsupport/knowledge-center/create-access-key/
        
        
Initializes AwsKeyValueStorage

```
   import com.keepersecurity.secretmanager.aws.kms.AwsKeyValueStorage;
   import com.keepersecurity.secretmanager.aws.kms.AwsSessionConfig;
   import software.amazon.awssdk.regions.Region;
    
     String awsAccessKeyId = "<AWS Access Id>";
	 String awsSecretAccessKey = "<AWS Access Secret Key>";
	 Region region = <Region>;
    AwsSessionConfig sessionConfig = new AwsSessionConfig(awsAccessKeyId, awsSecretAccessKey , region);
```

An access key using the `AwsSessionConfig` data class and providing `awsAccessKeyId`,`awsSecretAccessKey` and `region` variables.

You will need an Azure App directory App to use the Azure Key Vault integration.


For more information on Azure App Directory App registration and Permissions see the Azure documentation: https://learn.microsoft.com/en-us/azure/key-vault/general/authentication

**Add AWS KMS Storage to Your Code**

Now that the AWS connection has been configured, you need to tell the Secrets Manager SDK to utilize the KMS as storage.

To do this, use AwsKeyValueStorage as your Secrets Manager storage in the SecretsManager constructor.

The storage will require an AWS Key ID, as well as the name of the Secrets Manager configuration file which will be encrypted by AWS KMS. Below is the sample Test class

```
import com.keepersecurity.secretmanager.aws.kms.AwsKeyValueStorage;
import com.keepersecurity.secretmanager.aws.kms.AwsSessionConfig;
import software.amazon.awssdk.regions.Region;
import com.keepersecurity.secretsManager.core.InMemoryStorage;
import com.keepersecurity.secretsManager.core.SecretsManager;
import com.keepersecurity.secretsManager.core.SecretsManagerOptions;		
import java.security.Security;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;

		class Test{
		   public static void main(String args[]){
		  			String keyId = "<Key ID>";
				    String awsAccessKeyId = "<AWS Access ID>";
				    String awsSecretAccessKey = "<AWS Secret>";
				    String oneTimeToken = "[One Time Token]";
				    Region region = Region.<cloud-region>;
				    String configFileLocation = "client_config_test.json";
				try{
		    			//set AWS configuration	
				    AwsSessionConfig sessionConfig = new AwsSessionConfig(awsAccessKeyId, awsSecretAccessKey , region);
				    //Get Storage 
			  		AwsKeyValueStorage awskvstorage =  new AwsKeyValueStorage(keyId, configFileLocation, sessionConfig);
				 	initializeStorage(awskvstorage, oneTimeToken);
			         SecretsManagerOptions options = new SecretsManagerOptions(awskvstorage);
			    	 	//getSecrets(OPTIONS);
				}catch (Exception e) {
		  			  System.out.println(e.getMessage());
		 		}
	 	  }
	 	}
			
```
