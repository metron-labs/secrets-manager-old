package com.keepersecurity.secretmanager.oracle.kv;

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

import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * Json utility class
 */
public class JsonUtil {

	final static Logger logger = LoggerFactory.getLogger(JsonUtil.class);
	private static ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Check Json File Valid / Invalid
	 * @param filePath
	 * @return Return the boolean true/false
	 */
	public static boolean isValidJsonFile(String filePath) {
		try (FileReader reader = new FileReader(filePath)) {
			JsonElement jsonElement = JsonParser.parseReader(reader);
			return jsonElement != null;
		} catch (IOException | JsonSyntaxException e) {
			logger.debug(e.getMessage());
		} 
		return false;
	}


	/**
	 * Check Json content Valid / Invalid
	 * @param jsonContent
	 * @return Return the boolean true/false
	 */
	public static boolean isValidJson(String jsonContent) {
		try {
			JsonElement jsonElement = JsonParser.parseString(jsonContent);
			return jsonElement != null;
		} catch (JsonSyntaxException e) {
			logger.debug(e.getMessage());
		}
		return false;
	}

	/**
	 *  Convert String to Map
	 * @param content
	 * @return Return the Map 
	 * @throws JsonProcessingException
	 */
	public static Map<String, Object> convertToMap(String content) throws JsonProcessingException {
		return objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {
		});
	}
	

	/**
	 * Convert Map to String
	 * @param configMap
	 * @return Return the string value from config map
	 * @throws JsonProcessingException
	 */
	public static String convertToString(Map<String, Object> configMap) throws JsonProcessingException {
		return objectMapper.writeValueAsString(configMap);
	}
}
