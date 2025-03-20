using System;
using Amazon;
using Amazon.KeyManagementService;
using Amazon.Runtime;
using AWSKeyManagement;
using Microsoft.Extensions.Logging;

public class AwsKmsClient
{
    private readonly AmazonKeyManagementServiceClient kmsClient;

    public AwsKmsClient(AWSSessionConfig awsSessionConfig = null, ILogger logger = null)
    {
        if (awsSessionConfig == null)
        {
            kmsClient = new AmazonKeyManagementServiceClient();
        }
        else
        {   
            if (awsSessionConfig.AwsAccessKeyId == null || awsSessionConfig.AwsSecretAccessKey == null){
                logger.LogInformation("AWS Access Key ID and Secret Access Key are not given, choosing default credentials");
                awsSessionConfig.AwsAccessKeyId = Environment.GetEnvironmentVariable("AWS_ACCESS_KEY_ID");
                awsSessionConfig.AwsSecretAccessKey = Environment.GetEnvironmentVariable("AWS_SECRET_ACCESS_KEY");
            }
            var credentials = new BasicAWSCredentials(
                awsSessionConfig.AwsAccessKeyId,
                awsSessionConfig.AwsSecretAccessKey
            );

            kmsClient = new AmazonKeyManagementServiceClient(credentials, RegionEndpoint.GetBySystemName(awsSessionConfig.RegionName));
        }
    }

    public AmazonKeyManagementServiceClient GetCryptoClient()
    {
        return kmsClient;
    }
}
