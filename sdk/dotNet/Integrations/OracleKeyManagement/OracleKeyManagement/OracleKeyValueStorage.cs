#nullable enable

using System;
using System.Collections.Generic;
using System.Text.Json;
using System.Threading.Tasks;
using SecretsManager;
using Microsoft.Extensions.Logging;
using System.IO;
using System.Text;
using System.Linq;
using Oci.KeymanagementService.Responses;

namespace OracleKeyManagement
{
    public class OracleKeyValueStorage : IKeyValueStorage
    {
        private const string DefaultConfigFileLocation = "client-config.json";

        private OciKmsClient ksmClient;
        private Oci.KeymanagementService.KmsCryptoClient cryptoClient;
        private Oci.KeymanagementService.KmsManagementClient managementClient;
        private Dictionary<string, string> config = new();
        private string lastSavedConfigHash;
        private readonly string configFileLocation = DefaultConfigFileLocation;
        private readonly ILogger logger;

        public string KeyId { get; private set; }
        public string KeyVersionId { get; private set; }

        private bool IsAsymmetric;

        public OracleKeyValueStorage(string KeyId, string KeyVersionId, string? configFileLocation = null, OciSessionConfig? ociSessionConfig = null, ILogger? logger = null)
        {
            this.logger = logger ?? LoggerFactory.Create(builder => builder.AddConsole()).CreateLogger<OracleKeyValueStorage>();
            this.KeyId = KeyId;
            this.KeyVersionId = KeyVersionId;
            if (configFileLocation == null)
            {
                configFileLocation = DefaultConfigFileLocation;
            }
            else
            {
                this.configFileLocation = Path.GetFullPath(configFileLocation);
            }

            ksmClient = new OciKmsClient(ociSessionConfig,this.logger);
            cryptoClient = ksmClient.GetCryptoClient();
            managementClient = ksmClient.GetManagementClient();
            lastSavedConfigHash = "";
            GetKeyDetailsAsync().Wait();
            LoadConfigAsync().Wait();
        }

        public string? GetString(string key)
        {
            if (config.Count == 0)
            {
                LoadConfigAsync().Wait();
            }
            return config.TryGetValue(key, out var value) ? value : null;
        }

        public void SaveString(string key, string value)
        {
            if (config.Count == 0)
            {
                LoadConfigAsync().Wait();
            }
            config[key] = value;
            SaveConfigAsync(config).Wait();
        }

        public byte[]? GetBytes(string key)
        {
            if (config.Count == 0)
            {
                LoadConfigAsync().Wait();
            }

            var stringValue = config.TryGetValue(key, out var result) ? result : null;
            return stringValue == null ? null : CryptoUtils.Base64ToBytes(stringValue);
        }

        public void SaveBytes(string key, byte[] value)
        {
            if (config.Count == 0)
            {
                LoadConfigAsync().Wait();
            }
            config[key] = CryptoUtils.BytesToBase64(value);
            SaveConfigAsync(config).Wait();
        }

        public void Delete(string key)
        {
            config.Remove(key);
            SaveConfigAsync(config).Wait();
        }

        private async Task CreateConfigFileIfMissingAsync()
        {
            try
            {
                if (File.Exists(configFileLocation))
                {
                    logger.LogInformation("Config file already exists at: {Path}", configFileLocation);
                    return;
                }

                logger.LogInformation("Config file does not exist at: {Path}", configFileLocation);
                string? directory = Path.GetDirectoryName(configFileLocation);
                if (directory != null && !Directory.Exists(directory))
                {
                    Directory.CreateDirectory(directory);
                }

                EncryptOptions options = new EncryptOptions
                {
                    KeyId = KeyId,
                    keyVersionId = KeyVersionId,
                    Message = "{}",
                    IsAsymmetric = IsAsymmetric,
                    CryptoClient = cryptoClient
                };
                // Encrypt an empty configuration and write to the file
                byte[] blob = await IntegrationUtils.EncryptBufferAsync(options, logger);
                await File.WriteAllBytesAsync(configFileLocation, blob);

                logger.LogInformation("Config file created at: {Path}", configFileLocation);
            }
            catch (Exception ex)
            {
                logger.LogError("Error creating config file: {Message}", ex.Message);
            }
        }

        private async Task LoadConfigAsync()
        {
            await CreateConfigFileIfMissingAsync();

            try
            {
                // Read the config file
                byte[] contents;
                try
                {
                    string configData = File.ReadAllText(configFileLocation);

                    try
                    {
                        bool fileExists = File.Exists(configFileLocation);
                        var obj = JsonSerializer.Deserialize<Dictionary<string, string>>(configData);
                        contents = Encoding.UTF8.GetBytes(configData);
                        logger.LogInformation("Valid JSON parsed successfully.");
                    }
                    catch (Exception ex)
                    {
                        logger.LogDebug($"Error parsing valid JSON: {ex.Message}");
                        contents = await File.ReadAllBytesAsync(configFileLocation);
                    }

                    logger.LogInformation("Loaded config file {Path}", configFileLocation);
                }
                catch (Exception ex)
                {
                    logger.LogError("Failed to load config file {Path}: {Message}", configFileLocation, ex.Message);
                    throw new Exception($"Failed to load config file {configFileLocation}");
                }

                if (contents.Length == 0)
                {
                    logger.LogWarning("Empty config file {Path}", configFileLocation);
                    contents = Encoding.UTF8.GetBytes("{}");
                }

                // Check if the content is plain JSON
                Dictionary<string, string>? parsedConfig = null;
                Exception? jsonError = null;
                bool decryptionError = false;

                try
                {
                    string configData = Encoding.UTF8.GetString(contents);
                    parsedConfig = JsonSerializer.Deserialize<Dictionary<string, string>>(configData);

                    if (parsedConfig != null)
                    {
                        config = parsedConfig;
                        await SaveConfigAsync(config);
                        lastSavedConfigHash = ComputeMD5Hash(SerializeConfig(config));
                        return;
                    }
                }
                catch (Exception ex)
                {
                    jsonError = ex;
                }

                // If parsing as JSON failed, try decryption
                if (jsonError != null)
                {
                    try
                    {
                        DecryptOptions options = new DecryptOptions
                        {
                            KeyId = KeyId,
                            keyVersionId = KeyVersionId,
                            CipherText = contents,
                            IsAsymmetric = IsAsymmetric,
                            CryptoClient = cryptoClient,
                        };
                        string decryptedJson = await IntegrationUtils.DecryptBufferAsync(options, logger);
                        parsedConfig = JsonSerializer.Deserialize<Dictionary<string, string>>(decryptedJson);

                        if (parsedConfig != null)
                        {
                            config = parsedConfig;
                            lastSavedConfigHash = ComputeMD5Hash(SerializeConfig(config));
                            return;
                        }
                    }
                    catch (Exception ex)
                    {
                        decryptionError = true;
                        logger.LogDebug("Failed to parse decrypted config file: {Message}", ex.Message);
                        throw new Exception($"Failed to parse decrypted config file {configFileLocation}");
                    }
                }

                if (jsonError != null && decryptionError)
                {
                    logger.LogError("Config file is not a valid JSON file: {Message}", jsonError.Message);
                    throw new Exception($"{configFileLocation} may contain JSON format problems");
                }
            }
            catch (Exception ex)
            {
                logger.LogError("Error loading config: {Message}", ex.Message);
                throw;
            }
        }

        private async Task GetKeyDetailsAsync()
        {
            try
            {

                var requestId = Guid.NewGuid().ToString();
                var keyDetailsRequest = new Oci.KeymanagementService.Requests.GetKeyRequest
                {
                  KeyId = KeyId  
                };

                GetKeyResponse keyDetailsResponse = await managementClient.GetKey(keyDetailsRequest);
                var Algorithm = keyDetailsResponse.Key.KeyShape.Algorithm;

                if (Algorithm == Oci.KeymanagementService.Models.KeyShape.AlgorithmEnum.Rsa){
                    IsAsymmetric = true;
                }else if(Algorithm == Oci.KeymanagementService.Models.KeyShape.AlgorithmEnum.Aes){
                    IsAsymmetric = false;
                }else {
                    throw new Exception("Unsupported Key Spec for Oracle KMS Storage");
                }
            }
            catch (Exception ex)
            {
                logger.LogError($"Failed to get key details: {ex.Message}");
                throw;
            }
        }

        private async Task SaveConfigAsync(Dictionary<string, string>? updatedConfig = null, bool force = false)
        {
            try
            {
                // Retrieve current config
                Dictionary<string, string> currentConfig = config ?? new();
                string configJson = SerializeConfig(currentConfig);
                string configHash = ComputeMD5Hash(configJson);

                // Compare updatedConfig hash with current config hash
                if (updatedConfig != null && updatedConfig.Count > 0)
                {
                    string updatedConfigJson = SerializeConfig(updatedConfig);
                    string updatedConfigHash = ComputeMD5Hash(updatedConfigJson);

                    if (updatedConfigHash != configHash)
                    {
                        configHash = updatedConfigHash;
                        config = new Dictionary<string, string>(updatedConfig);
                    }
                }

                // Check if saving is necessary
                if (!force && configHash == lastSavedConfigHash)
                {
                    logger.LogWarning("Skipped config JSON save. No changes detected.");
                    return;
                }

                // Ensure the config file exists
                await CreateConfigFileIfMissingAsync();

                var serializedConfig = SerializeConfig(config);
                EncryptOptions options = new EncryptOptions
                {
                    KeyId = KeyId,
                    keyVersionId = KeyVersionId,
                    Message = serializedConfig,
                    CryptoClient = cryptoClient,
                    IsAsymmetric = IsAsymmetric
                };
                // Encrypt the config JSON and write to the file
                byte[] blob = await IntegrationUtils.EncryptBufferAsync(options, logger);
                await File.WriteAllBytesAsync(configFileLocation, blob);

                // Update the last saved config hash
                lastSavedConfigHash = configHash;
            }
            catch (Exception ex)
            {
                logger.LogError("Error saving config: {Message}", ex.Message);
            }
        }

        public async Task<string> DecryptConfigAsync(bool autosave = true)
        {
            byte[] ciphertext;
            string plaintext = "";

            try
            {
                // Read the config file
                if (!File.Exists(configFileLocation))
                {
                    logger.LogError("Config file not found: {File}", configFileLocation);
                    throw new FileNotFoundException($"Config file not found: {configFileLocation}");
                }

                ciphertext = await File.ReadAllBytesAsync(configFileLocation);
                if (ciphertext.Length == 0)
                {
                    logger.LogWarning("Empty config file {File}", configFileLocation);
                    return "";
                }
            }
            catch (Exception ex)
            {
                logger.LogError("Failed to load config file {File}: {Message}", configFileLocation, ex.Message);
                throw new Exception($"Failed to load config file {configFileLocation}");
            }

            try
            {
                DecryptOptions options = new DecryptOptions
                {
                    CipherText = ciphertext,
                    KeyId = KeyId,
                    keyVersionId = KeyVersionId,
                    IsAsymmetric = IsAsymmetric,
                    CryptoClient = cryptoClient,
                };
                // Decrypt the file contents
                plaintext = await IntegrationUtils.DecryptBufferAsync(options, logger);
                if (string.IsNullOrWhiteSpace(plaintext))
                {
                    logger.LogInformation("Failed to decrypt config file {File}", configFileLocation);
                }
                else if (autosave)
                {
                    // Optionally autosave the decrypted content
                    await File.WriteAllTextAsync(configFileLocation, plaintext);
                }
            }
            catch (Exception ex)
            {
                logger.LogError("Failed to write decrypted config file {File}: {Message}", configFileLocation, ex.Message);
                throw new Exception($"Failed to write decrypted config file {configFileLocation}");
            }

            return plaintext;
        }

        public async Task<bool> ChangeKeyAsync(string newKeyId, string? newKeyVersionId, OciSessionConfig? newOciSessionConfig)
        {
            var oldKeyId = KeyId;
            var oldKeyVersionId = KeyVersionId;
            var oldKmsClient = ksmClient;
            try
            {
                // Check if config needs initialization
                if (config == null || (config is System.Collections.ICollection collection && collection.Count == 0))
                {
                    await LoadConfigAsync();
                }

                KeyId = newKeyId;
                if (!(newKeyVersionId == null || newKeyVersionId == "")){
                    KeyVersionId = newKeyVersionId;
                }
                if (newOciSessionConfig != null){
                    ksmClient =new OciKmsClient(newOciSessionConfig, this.logger);
                }

                await GetKeyDetailsAsync();
                await SaveConfigAsync(new Dictionary<string, string>(), true);
            }
            catch (Exception ex)
            {
                // Restore the previous key config and KMS client if the operation fails
                KeyId = oldKeyId;
                KeyVersionId = oldKeyVersionId;
                ksmClient = oldKmsClient;

                logger.LogError($"Failed to change the key to '{newKeyId}' for config '{configFileLocation}': {ex.Message}");
                throw new InvalidOperationException($"Failed to change the key for {configFileLocation}");
            }
            return true;
        }

        private static string SerializeConfig(Dictionary<string, string>? config)
        {
            if (config == null)
            {
                return "{}";
            }
            var sortedKeys = Enumerable.OrderBy(config.Keys, k => k).ToList();
            var sortedConfig = sortedKeys.ToDictionary(k => k, k => config[k]);
            return JsonSerializer.Serialize(sortedConfig, new JsonSerializerOptions { WriteIndented = true });
        }

        private static string ComputeMD5Hash(string input)
        {
            using var md5 = System.Security.Cryptography.MD5.Create();
            byte[] hashBytes = md5.ComputeHash(Encoding.UTF8.GetBytes(input));
            return BitConverter.ToString(hashBytes).Replace("-", "").ToLowerInvariant();
        }
    }
}
