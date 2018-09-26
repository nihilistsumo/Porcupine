package com.trema.pcpn.aspect;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class AspectVectorSimilarity {
	
	private String aspVecJSONdir;
	private String[] aspVecJSONfiles;
	private JSONObject jsonCache;
	private int jsonCacheIndex = 0;
	
	public AspectVectorSimilarity(String aspVecJSONdirPath) {
		try {
			aspVecJSONdir = aspVecJSONdirPath;
			File jsonDir = new File(aspVecJSONdirPath);
			File[] jsonFiles = jsonDir.listFiles();
			aspVecJSONfiles = new String[jsonFiles.length];
			for(int i=0; i<aspVecJSONfiles.length; i++)
				aspVecJSONfiles[i] = jsonFiles[i].getName();
			JSONParser parser = new JSONParser();
			jsonCache = (JSONObject) parser.parse(new FileReader(aspVecJSONdirPath+"/"+aspVecJSONfiles[jsonCacheIndex]));
			/*
			temp = (JSONObject) jsonCache.get("7c27f1f104669e97b25328d7f180d824bcb9d692");
			int count = 0;
			for(Object k:temp.keySet()) {
				System.out.println("Key: "+Integer.parseInt(k.toString())+", Value: "+(double)temp.get(k));
				count++;
				if(count>9)
					break;
			}
			*/
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public double getCosineSimilarity(String p1, String p2) {
		double score = 0;
		String para1 = "para:"+p1;
		String para2 = "para:"+p2;
		if(!jsonCache.containsKey(para1))
			this.searchKey(para1);
		JSONObject para1VecObj = (JSONObject) jsonCache.get(para1);
		double modA = this.lengthVec(para1VecObj);
		if(!jsonCache.containsKey(para2))
			this.searchKey(para2);
		JSONObject para2VecObj = (JSONObject) jsonCache.get(para2);
		double modB = this.lengthVec(para2VecObj);
		double aDotB = 0; // a.b
		for(Object k1:para1VecObj.keySet()) {
			if(para2VecObj.containsKey("asp:"+k1.toString()))
				aDotB+=(double)para1VecObj.get("asp:"+k1.toString())*(double)para2VecObj.get("asp:"+k1.toString());
		}
		if(modA<Double.MIN_VALUE || modB<Double.MIN_VALUE)
			return score;
		score = aDotB/(modA*modB);
		return score;
	}
	
	private double lengthVec(JSONObject vecObj) {
		double sumOfSquares = 0;
		for(Object key:vecObj.keySet()) {
			if(key.toString().equalsIgnoreCase("asp:-1"))
				continue;
			sumOfSquares+=Math.pow((double)vecObj.get(key.toString()), 2);
		}
		return Math.sqrt(sumOfSquares);
	}
	
	private void searchKey(String key) {
		JSONParser parser = new JSONParser();
		boolean foundKey = false;
		for(int i=0; i<aspVecJSONfiles.length; i++) {
			if(i!=jsonCacheIndex) {
				try {
					jsonCache = (JSONObject) parser.parse(new FileReader(aspVecJSONdir+"/"+aspVecJSONfiles[i]));
					if(jsonCache.containsKey(key)) {
						jsonCacheIndex = i;
						foundKey = true;
						break;
					}
				} catch (IOException | ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		if(!foundKey)
			throw (new NullPointerException("Key: "+key+" not found in any of the json objects in "+aspVecJSONdir));
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
