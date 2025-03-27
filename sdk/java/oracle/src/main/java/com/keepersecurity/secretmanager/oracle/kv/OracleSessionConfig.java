package com.keepersecurity.secretmanager.oracle.kv;
import com.oracle.bmc.Region;

/**
#  _  __
# | |/ /___ ___ _ __  ___ _ _ (R)
# | ' </ -_) -_) '_ \/ -_) '_|
# |_|\_\___\___| .__/\___|_|
#              |_|
#
# Keeper Secrets Manager
# Copyright 2025 Keeper Security Inc.
# Contact: sm@keepersecurity.com
**/

public class OracleSessionConfig {

	private String cryptoEndpoint;
	private String vaultId;
	private String keyId;
	private String keyVersionId;
	private String configPath ;
	private String managementEndpoint;
	private Region region;
	/**
	 * @param configPath
	 * @param cryptoEndpoint
	 * @param managementEndpoint
	 * @param vaultId
	 * @param keyId
	 * @param keyVersionId
	 * @param region
	 */
	public OracleSessionConfig(String configPath, String cryptoEndpoint, String managementEndpoint, String vaultId, String keyId, String keyVersionId, Region region) {
		super();
		this.configPath = configPath;
		this.cryptoEndpoint = cryptoEndpoint;
		this.vaultId = vaultId;
		this.keyId = keyId;
		this.keyVersionId = keyVersionId;
		this.managementEndpoint = managementEndpoint;
		this.region = region;
	}
	
	public String getConfigPath() {
		return this.configPath;
	}

	public void setConfigPath(String configPath) {
		this.configPath = configPath;
	}

	public String getCryptoEndpoint() {
		return cryptoEndpoint;
	}

	public void setCryptoEndpoint(String cryptoEndpoint) {
		this.cryptoEndpoint = cryptoEndpoint;
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
	
	public void setManagementEndpoint(String managementEndpoint) {
		this.managementEndpoint = managementEndpoint;
	}
	
	public String getManagementEndpoint() {
		return managementEndpoint;
	}

	public Region getRegion() {
		return region;
	}

	public void setRegion(Region region) {
		this.region = region;
	}
}
