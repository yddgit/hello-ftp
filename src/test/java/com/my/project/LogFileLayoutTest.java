package com.my.project;

import org.apache.log4j.Logger;

public class LogFileLayoutTest {
	private static final Logger logger = Logger.getLogger("file"); 
	public static void main(String[] args) {
		for(int i=0; i<10; i++) {
			logger.info("\"fileCount\":" + i);
		}
	}
}
