package com.theater;
/*
 * Authors:
 * Puneet Bansal, Sadaf H Syed
 */
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * Reads configuration parameters from a config file.
 */
public class PropertyReader {
	
	private Properties config;

    public PropertyReader(String configFilePath) {
		try {
			config = new Properties();
			config.load(new FileInputStream(configFilePath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
    public int getServerPort(int serverInstance) {
    	return Integer.parseInt(config.getProperty("port" + serverInstance));
    }
    public String getServerHost(int serverInstance) {
    	return config.getProperty("server" + serverInstance);
    }
    public Integer getTotalSeats() {
    	return Integer.parseInt(config.getProperty("c"));
    }

    public Integer getServerCount() {
    	return (Integer.parseInt(config.getProperty("n")))+1;
    }
    
    public Long getTimeout() {
    	return Long.parseLong(config.getProperty("timeout"));
    }
}
