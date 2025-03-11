#nullable enable

using Google.Cloud.Kms.V1;
using System;
using System.Security.Cryptography;
using System.Threading.Tasks;
using Google.Protobuf;

public class KMSUtils
{
    public static async Task<byte[]> EncryptDataAsymmetric(EncryptOptions options)
    {
        string keyName = options.KeyProperties.ToResourceName();

        // Get the public key from Cloud KMS
        PublicKey publicKey = await options.CryptoClient.GetPublicKeyAsync(new GetPublicKeyRequest { Name = keyName });

        if (publicKey.Name != keyName)
        {
            throw new Exception("GetPublicKey: request corrupted in-transit");
        }

        string[] blocks = publicKey.Pem.Split("-", StringSplitOptions.RemoveEmptyEntries);
        byte[] pem = Convert.FromBase64String(blocks[1]);


        // Encrypt using RSA and OAEP padding
        using RSA rsa = RSA.Create();
        rsa.ImportSubjectPublicKeyInfo(pem, out _);

        RSAEncryptionPadding padding = GetPadding(GetHashingAlgorithm(options.EncryptionAlgorithm));
        byte[] ciphertext = rsa.Encrypt(options.Message, padding);

        return ciphertext;
    }

    public static async Task<byte[]> EncryptDataSymmetric(EncryptOptions options)
    {
        string keyName = options.KeyProperties.ToResourceName();
        byte[] encodedData = options.Message;

        var kmsClient = options.CryptoClient;
        var request = new EncryptRequest
        {
            Name = keyName,
            Plaintext = ByteString.CopyFrom(encodedData),
        };

        EncryptResponse encryptResponse = await kmsClient.EncryptAsync(request);
        byte[] ciphertext = encryptResponse.Ciphertext.ToByteArray();

        return ciphertext;
    }

    public async Task<byte[]> DecryptDataAndValidateCRCAsync(
        DecryptOptions options)
    {

        if (options.IsAsymmetric)
        {
            var request = new AsymmetricDecryptRequest
            {
                Name =options.KeyProperties.ToResourceName(), 
                Ciphertext = ByteString.CopyFrom(options.CipherText),
            };
            var response = await options.CryptoClient.AsymmetricDecryptAsync(request);

            return response.Plaintext.ToByteArray();
        }
        else
        {
            var request = new DecryptRequest
            {
                Name = options.KeyProperties.ToKeyName(),
                Ciphertext = ByteString.CopyFrom(options.CipherText),
            };
            var response = await options.CryptoClient.DecryptAsync(request);

            return response.Plaintext.ToByteArray();
        }
    }


    private static RSAEncryptionPadding GetPadding(string encryptionAlgorithm)
    {
        return encryptionAlgorithm switch
        {
            "RSAES_OAEP_SHA_256" => RSAEncryptionPadding.OaepSHA256,
            "RSAES_OAEP_SHA_1" => RSAEncryptionPadding.OaepSHA1,
            _ => throw new ArgumentException("Unsupported encryption algorithm", nameof(encryptionAlgorithm)),
        };
    }

    private static readonly System.Collections.Generic.Dictionary<string, string> SupportedEncryptionAlgorithms = new()
    {
        { "RSA_DECRYPT_OAEP_2048_SHA256", "SHA256" },
        { "RSA_DECRYPT_OAEP_3072_SHA256", "SHA256" },
        { "RSA_DECRYPT_OAEP_4096_SHA256", "SHA256" },
        { "RSA_DECRYPT_OAEP_4096_SHA512", "SHA512" },
        { "RSA_DECRYPT_OAEP_2048_SHA1", "SHA1" },
        { "RSA_DECRYPT_OAEP_3072_SHA1", "SHA1" },
        { "RSA_DECRYPT_OAEP_4096_SHA1", "SHA1" }
    };

    public static string GetHashingAlgorithm(RSAEncryptionPadding encryptionAlgorithm)
    {
        if (SupportedEncryptionAlgorithms.TryGetValue(encryptionAlgorithm.ToString(), out string? hashAlgorithm))
        {
            return hashAlgorithm;
        }
        throw new Exception("Unsupported encryption algorithm is used for the provided key");
    }
}


