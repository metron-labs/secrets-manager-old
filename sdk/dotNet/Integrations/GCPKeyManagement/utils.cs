#nullable enable

using Google.Cloud.Kms.V1;
using System;
using System.Security.Cryptography;
using System.Threading.Tasks;
using Google.Protobuf;
using Microsoft.Extensions.Logging;
using System.Text;
using System.IO;
using System.Linq;

public class IntegrationUtils
{
    private const int AesKeySize = IntegrationConstants.AES_KEY_SIZE; // 256-bit key
    private const int NonceSize = IntegrationConstants.NONCE_SIZE;  // AES-GCM nonce size

    public static async Task<byte[]> EncryptBufferAsync(EncryptBufferOptions options, ILogger logger)
    {
        try
        {
            // Step 1: Generate a random 32-byte AES key
            byte[] key = GenerateRandomBytes(AesKeySize);

            // Step 2: Generate a 16-byte nonce
            byte[] nonce = GenerateRandomBytes(NonceSize);

            // Step 3: Encrypt the message using AES-GCM
            byte[] ciphertext, tag;
            using (AesGcm aes = new AesGcm(key, IntegrationConstants.AES_GCM_TAG_BYTE_SIZE))
            {
                byte[] plaintextBytes = Encoding.UTF8.GetBytes(options.Message);
                ciphertext = new byte[plaintextBytes.Length];
                tag = new byte[AesGcm.TagByteSizes.MaxSize];

                aes.Encrypt(nonce, plaintextBytes, ciphertext, tag);
            }

            var encryptOptions = new EncryptOptions
            {
                KeyProperties = options.KeyProperties,
                Message = key,
                CryptoClient = options.CryptoClient,
                IsAsymmetric = options.IsAsymmetric,
                EncryptionAlgorithm = options.EncryptionAlgorithm,
            };

            var EncryptedKey = options.IsAsymmetric ? await EncryptDataAsymmetric(encryptOptions) : await EncryptDataSymmetric(encryptOptions);

            // Step 5: Build the encrypted blob
            using (MemoryStream ms = new MemoryStream())
            {
                using (BinaryWriter writer = new BinaryWriter(ms))
                {
                    writer.Write(IntegrationConstants.BLOB_HEADER);
                    WriteLengthPrefixed(writer, EncryptedKey);
                    WriteLengthPrefixed(writer, nonce);
                    WriteLengthPrefixed(writer, tag);
                    WriteLengthPrefixed(writer, ciphertext);
                }
                return ms.ToArray();
            }
        }
        catch (Exception ex)
        {
            logger.LogCritical($"GCP KMS Storage failed to encrypt: {ex.Message}");
            return Array.Empty<byte>(); // Return empty array in case of an error
        }
    }

    public static async Task<string> DecryptBufferAsync(DecryptBufferOptions options, ILogger logger)
    {
        try
        {
            // Step 1: Validate BLOB_HEADER
            byte[] header = new byte[IntegrationConstants.HEADER_SIZE];
            Array.Copy(options.Ciphertext, header, IntegrationConstants.HEADER_SIZE);
            if (!header.AsEnumerable().SequenceEqual(IntegrationConstants.BLOB_HEADER))
            {
                throw new InvalidOperationException("Invalid ciphertext structure: invalid header.");
            }

            int pos = IntegrationConstants.HEADER_SIZE;
            byte[][] parts = new byte[4][];

            // Step 2: Extract length-prefixed parts
            for (int i = 0; i < 4; i++)
            {
                if (pos + IntegrationConstants.LENGTH_PREFIX_SIZE > options.Ciphertext.Length)
                    throw new InvalidOperationException("Invalid ciphertext structure: size buffer length mismatch.");

                ushort partLength = BitConverter.ToUInt16(options.Ciphertext, pos);
                pos += IntegrationConstants.LENGTH_PREFIX_SIZE;

                if (pos + partLength > options.Ciphertext.Length)
                    throw new InvalidOperationException("Invalid ciphertext structure: part length mismatch.");

                parts[i] = options.Ciphertext[pos..(pos + partLength)];
                pos += partLength;
            }

            if (parts.Length != 4)
                throw new InvalidOperationException("Invalid ciphertext structure: incorrect number of parts.");

            byte[] encryptedKey = parts[0];
            byte[] nonce = parts[1];
            byte[] tag = parts[2];
            byte[] encryptedText = parts[3];


            // Step 3: Unwrap AES key using GCPKeyManagement Key
            var decryptData = new DecryptOptions
            {
                KeyProperties = options.KeyProperties,
                CipherText = encryptedKey,
                CryptoClient = options.CryptoClient,
                IsAsymmetric = options.IsAsymmetric,
                EncryptionAlgorithm = options.EncryptionAlgorithm,
            };

            var decryptedKey = await DecryptData(decryptData);

            // Step 4: Decrypt the message using AES-GCM
            try
            {
                using AesGcm aesGcm = new AesGcm(decryptedKey, IntegrationConstants.AES_GCM_TAG_BYTE_SIZE);
                byte[] decryptedData = new byte[encryptedText.Length];

                aesGcm.Decrypt(nonce, encryptedText, tag, decryptedData);

                // Step 5: Convert decrypted data to a UTF-8 string
                return Encoding.UTF8.GetString(decryptedData);
            }
            catch (Exception ex)
            {
                logger.LogError($"Decryption failed: {ex.Message}");
                return string.Empty;
            }
        }
        catch (Exception ex)
        {
            logger.LogError($"GCP Key Management Storage failed to decrypt: {ex.Message}");
            return string.Empty;
        }
    }


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

        RSAEncryptionPadding padding = GetPadding(options.EncryptionAlgorithm);
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

    public static async Task<byte[]> DecryptData(
        DecryptOptions options)
    {

        if (options.IsAsymmetric)
        {
            var request = new AsymmetricDecryptRequest
            {
                Name = options.KeyProperties.ToResourceName(),
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
        if (SupportedEncryptionAlgorithms.TryGetValue(encryptionAlgorithm, out RSAEncryptionPadding? hashAlgorithm))
        {
            return hashAlgorithm;
        }
        throw new Exception("Unsupported encryption algorithm is used for the provided key");
        
    }

    private static readonly System.Collections.Generic.Dictionary<string, RSAEncryptionPadding> SupportedEncryptionAlgorithms = new()
    {
        { "RsaDecryptOaep2048Sha256", RSAEncryptionPadding.OaepSHA256 },
        { "RsaDecryptOaep3072Sha256", RSAEncryptionPadding.OaepSHA256},
        { "RsaDecryptOaep4096Sha256", RSAEncryptionPadding.OaepSHA256},
        { "RsaDecryptOaep4096Sha512", RSAEncryptionPadding.OaepSHA512 },
        { "RsaDecryptOaep2048Sha1", RSAEncryptionPadding.OaepSHA1 },
        { "RsaDecryptOaep3072Sha1", RSAEncryptionPadding.OaepSHA1 },
        { "RsaDecryptOaep4096Sha1", RSAEncryptionPadding.OaepSHA1 }
    };


    private static byte[] GenerateRandomBytes(int size)
    {
        byte[] bytes = new byte[size];
        RandomNumberGenerator.Fill(bytes);
        return bytes;
    }

    private static void WriteLengthPrefixed(BinaryWriter writer, byte[] data)
    {
        writer.Write((ushort)data.Length);
        writer.Write(data);
    }
}

