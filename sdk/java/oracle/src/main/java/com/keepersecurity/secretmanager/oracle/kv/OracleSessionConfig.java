package com.keepersecurity.secretmanager.oracle.kv;

public class OracleSessionConfig {

	private String kmsEndpoint;
	private String vaultId;
	private String keyId;
	private String keyVersionId;
	private String configPath ;
	
	public OracleSessionConfig(String configPath, String kmsEndpoint, String vaultId, String keyId, String keyVersionId) {
		super();
		this.configPath = configPath;
		this.kmsEndpoint = kmsEndpoint;
		this.vaultId = vaultId;
		this.keyId = keyId;
		this.keyVersionId = keyVersionId;
	}
	
	public String getConfigPath() {
		return this.configPath;
	}
	public void setConfigPath(String configPath) {
		this.configPath = configPath;
	}
	public String getKmsEndpoint() {
		return kmsEndpoint;
	}
	public void setKmsEndpoint(String kmsEndpoint) {
		this.kmsEndpoint = kmsEndpoint;
	}
	public String getVaultId() {
		return vaultId;
	}
	public void setVaultId(String vaultId) {
		this.vaultId = vaultId;
	}
	public String getKeyId() {
		return keyId;
	}
	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}
	public String getKeyVersionId() {
		return keyVersionId;
	}
	public void setKeyVersionId(String keyVersionId) {
		this.keyVersionId = keyVersionId;
	}
}
