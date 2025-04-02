package com.keepersecurity.secretmanager.aws.kms;

/*
#  _  __
# | |/ /___ ___ _ __  ___ _ _ (R)
# | ' </ -_) -_) '_ \/ -_) '_|
# |_|\_\___\___| .__/\___|_|
#              |_|
#
# Keeper Secrets Manager
# Copyright 2025 Keeper Security Inc.
# Contact: sm@keepersecurity.com
*/

public class AwsSessionConfig {
	private String awsAccessKeyId;
	private String awsSecretAccessKey;

	/**
	 * Constructs a new {@code AwsSessionConfig} instance with the specified AWS
	 * access key ID
	 * and secret access key.
	 *
	 * @param awsAccessKeyId     The AWS access key ID.
	 * @param awsSecretAccessKey The AWS secret access key.
	 */
	public AwsSessionConfig(String awsAccessKeyId, String awsSecretAccessKey) {
		this.awsAccessKeyId = awsAccessKeyId;
		this.awsSecretAccessKey = awsSecretAccessKey;
	}

	public String getAwsAccessKeyId() {
		return awsAccessKeyId;
	}

	public void setAwsAccessKeyId(String awsAccessKeyId) {
		this.awsAccessKeyId = awsAccessKeyId;
	}

	public String getAwsSecretAccessKey() {
		return awsSecretAccessKey;
	}

	public void setAwsSecretAccessKey(String awsSecretAccessKey) {
		this.awsSecretAccessKey = awsSecretAccessKey;
	}
}
