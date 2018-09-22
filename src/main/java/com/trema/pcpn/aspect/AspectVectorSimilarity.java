package com.trema.pcpn.aspect;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class AspectVectorSimilarity {
	
	private String[] aspVecJSONfiles;
	private JSONObject jsonCache;
	private int jsonCacheIndex = 0;
	
	public AspectVectorSimilarity(String aspVecJSONdirPath) {
		try {
			File jsonDir = new File(aspVecJSONdirPath);
			File[] jsonFiles = jsonDir.listFiles();
			aspVecJSONfiles = new String[jsonFiles.length];
			for(int i=0; i<aspVecJSONfiles.length; i++)
				aspVecJSONfiles[i] = jsonFiles[i].getName();
			JSONParser parser = new JSONParser();
			jsonCache = (JSONObject) parser.parse(new FileReader(aspVecJSONdirPath+"/"+aspVecJSONfiles[jsonCacheIndex]));
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public double getCosineSimilarity(String para1, String para2) {
		double score = 0;
		
		return score;
	}

	public String[] getAspVecJSONfiles() {
		return aspVecJSONfiles;
	}

	public JSONObject getJsonCache() {
		return jsonCache;
	}

	public int getJsonCacheIndex() {
		return jsonCacheIndex;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
