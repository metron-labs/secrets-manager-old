using Google.Cloud.Kms.V1;
using System.Runtime.Serialization;

public static class IntegrationConstants
{
    // Supported Key Specs
    public static readonly string[] SupportedKeySpecs = 
    {
        KeySpecEnum.RSA_2048,
        KeySpecEnum.RSA_4096,
        KeySpecEnum.RSA_3072,
        KeySpecEnum.SYMMETRIC_DEFAULT
    };

    public static readonly byte[] BLOB_HEADER = { 0xFF, 0xFF }; // Encrypted BLOB Header: U+FFFF is a non-utf-8-character

    public const int HEADER_SIZE = 2;

    public const int AES_KEY_SIZE = 32;
    public const int NONCE_SIZE = 12;
    public const int AES_GCM_TAG_BYTE_SIZE = 16;

    public const int LENGTH_PREFIX_SIZE = 2;
    public static readonly string[] SupportedKeyPurpose =
    {
        KeyPurpose.RAW_ENCRYPT_DECRYPT.ToString(),
        KeyPurpose.ENCRYPT_DECRYPT.ToString(),
        KeyPurpose.ASYMMETRIC_DECRYPT.ToString()
    };
}

public static class KeySpecEnum
{
    public const string RSA_2048 = "RSA_2048";
    public const string RSA_4096 = "RSA_4096";
    public const string RSA_3072 = "RSA_3072";
    public const string SYMMETRIC_DEFAULT = "SYMMETRIC_DEFAULT";
}

public enum KeyPurpose
{
    [EnumMember(Value = "ENCRYPT_DECRYPT")]
    ENCRYPT_DECRYPT,

    [EnumMember(Value = "ASYMMETRIC_DECRYPT")]
    ASYMMETRIC_DECRYPT,

    [EnumMember(Value = "CRYPTO_KEY_PURPOSE_UNSPECIFIED")]
    CRYPTO_KEY_PURPOSE_UNSPECIFIED,

    [EnumMember(Value = "ASYMMETRIC_SIGN")]
    ASYMMETRIC_SIGN,

    [EnumMember(Value = "RAW_ENCRYPT_DECRYPT")]
    RAW_ENCRYPT_DECRYPT,

    [EnumMember(Value = "MAC")]
    MAC
}

public class Options
{
    public bool IsAsymmetric { get; set; }
    public KeyManagementServiceClient CryptoClient { get; set; }
    public GCPKeyConfig KeyProperties { get; set; }
    public string EncryptionAlgorithm { get; set; }
}

public class BufferOptions : Options
{
    public string KeyPurpose { get; set; }
}

public class EncryptBufferOptions : BufferOptions
{
    public string Message { get; set; }
}

public class DecryptBufferOptions : BufferOptions
{
    public byte[] Ciphertext { get; set; }
}

public class EncryptOptions : Options
{
    public byte[] Message { get; set; }
}

public class DecryptOptions : Options
{
    public byte[] CipherText { get; set; }
}
