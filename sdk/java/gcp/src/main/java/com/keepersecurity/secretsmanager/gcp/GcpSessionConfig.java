package com.keepersecurity.secretsmanager.gcp;
/*
*  _  __
* | |/ /___ ___ _ __  ___ _ _ (R)
* | ' </ -_) -_) '_ \/ -_) '_|
* |_|\_\___\___| .__/\___|_|
*              |_|
*
* Keeper Secrets Manager
* Copyright 2025 Keeper Security Inc.
* Contact: sm@keepersecurity.com
*/

/**
 * The {@code GcpSessionConfig} class represents a configuration for an GCP session,
 * containing the GCP key ID, key ring, project Id  and key version.
 * 
 */
public class GcpSessionConfig {

	private String location;
	private String keyRing;
	private String keyId;
	private String projectId;
	private String keyVersion;
	private String credentialsPath;

	/**
	 * Constructs a new {@code GcpSessionConfig} instance with the specified project Id, location, keyId, version
	 * @param projectId   GCP project Id
	 * @param location    Location
	 * @param keyRing     Key Ring
	 * @param keyId		  Key ID
	 * @param keyVersion  Key Version
	 * @param credentialsPath  Credential file path
	 */
	public GcpSessionConfig(String projectId, String location, String keyRing, String keyId, String keyVersion, String credentialsPath) {
		super();
		this.location = location;
		this.keyRing = keyRing;
		this.keyId = keyId;
		this.projectId = projectId;
		this.keyVersion = keyVersion;
		this.credentialsPath = credentialsPath;
	}
	/**
	 * Get GCP projectId
	 * @return project Id
	 */
	public String getProjectId() {
		return projectId;
	}
	/**
	 * Set GCP project Id
	 * @param projectId
	 */
	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}
	/**
	 * Return GCP location
	 * @return location
	 */
	public String getLocation() {
		return location;
	}
	/**
	 * Set location
	 * @param location   GCP location
	 */
	public void setLocation(String location) {
		this.location = location;
	}
	/**
	 * Return Key Ring
	 * @return key ring
	 */
	public String getKeyRing() {
		return keyRing;
	}
	/**
	 * Set Key Ring
	 * @param keyRing : GCP key ring
	 */
	public void setKeyRing(String keyRing) {
		this.keyRing = keyRing;
	}
	/**
	 * Return Key ID
	 * @return key Id
	 */
	public String getKeyId() {
		return keyId;
	}
	/**
	 * Set Encryption/decryption key Id
	 * @param keyId : Key ID
	 */
	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}
	/**
	 * Return Key Version
	 * @return key version
	 */
	public String getKeyVersion() {
		return keyVersion;
	}
	/**
	 * Set Key Version
	 * @param keyVersion Key Version
	 */
	public void setKeyVersion(String keyVersion) {
		this.keyVersion = keyVersion;
	}
	/**
	 * Return GCP Credential path
	 * @return credential path
	 */
	public String getCredentialsPath() {
		return credentialsPath;
	}
	/**
	 * Set GCP credential path
	 * @param credentialsPath
	 */
	public void setCredentialsPath(String credentialsPath) {
		this.credentialsPath = credentialsPath;
	}
}
