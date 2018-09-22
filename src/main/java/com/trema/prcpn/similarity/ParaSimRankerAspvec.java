package com.trema.prcpn.similarity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.trema.pcpn.aspect.AspectVectorSimilarity;
import com.trema.pcpn.util.DataUtilities;
import com.trema.pcpn.util.MapUtil;

public class ParaSimRankerAspvec {
	
	public void rank(Properties prop, String aspVecJSONdirPath, String titlesFile, String articleQrelsPath, String outRunPath) throws IOException, InterruptedException {
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outRunPath)));
		AspectVectorSimilarity aspVecSim = new AspectVectorSimilarity(aspVecJSONdirPath);
		//HashMap<String, ArrayList<String>> candPageParaMap = DataUtilities.getPageParaMapFromRunfile(candRunFilePath);
		HashMap<String, ArrayList<String>> pageParaMap = DataUtilities.getTrueArticleParasMapFromPath(articleQrelsPath);
		//HashMap<String, ArrayList<String>> pageParaMap = candPageParaMap;
		//if(method.equalsIgnoreCase("true"))
		//	pageParaMap = truePageParaMap;
		
		BufferedReader br = new BufferedReader(new FileReader(new File(titlesFile)));
		String page = br.readLine();
		ArrayList<String> titlesSet = new ArrayList<String>();
		while(page!=null) {
			titlesSet.add("enwiki:"+page.replaceAll(" ", "%20"));
			page = br.readLine();
		}
		br.close();
		
		ConcurrentHashMap<String, ConcurrentHashMap<String, Double>> scoresMap = new ConcurrentHashMap<String, ConcurrentHashMap<String, Double>>();
		System.out.println("Calculating feature scores...");
		int p=0;
		for(String pageID:pageParaMap.keySet()) {
			if(titlesSet.contains(pageID.split(":")[1])) {
				ArrayList<String> retParaIDs = pageParaMap.get(pageID);
				System.out.println();
				System.out.println("Calculating feature scores");
				for(String keyPara:retParaIDs) {
					if(pageParaMap.get(pageID).contains(keyPara)) {
						for(String retPara:retParaIDs) {
							if(!keyPara.equals(retPara)) {
								// calculate similarity score from vector pairs of para IDs
							}
						}
						System.out.println(".");
					}
				}
				p++;
				System.out.println(pageID+" is complete. "+(titlesSet.size()-p)+" pages remaining...");
			}
		}
		System.out.println("Feature scores calculated.\nWriting run file...");
		
		
		for(String pageAndKey:scoresMap.keySet()) {
			Map<String, Double> scoreMap = MapUtil.sortByValue(scoresMap.get(pageAndKey));
			double score = 0;
			double maxScore = this.getMaxScore(scoreMap);
			String pageID = pageAndKey.split("_")[0];
			String keyPara = pageAndKey.split("_")[1];
			for(String retPara:scoreMap.keySet()) {
				if(maxScore>0.00000001)
					score = scoreMap.get(retPara)/maxScore;
				else
					score = scoreMap.get(retPara);
				String rfLine = "";
				
				rfLine = pageID+":"+keyPara+" Q0 "+retPara+" 0 "+score+" ASP-COMBINED";
				bw.write(rfLine+"\n");
			}
		}
		bw.close();
		System.out.println("Run file written in "+outRunPath);
		//System.out.println("Total no. of queries: "+allQueries.size());
	}
	
	public double getMaxScore(Map<String, Double> scoreMap) {
		double maxScore = 0;
		// This weird for loop is there because the scoreMap is already sorted
		// and I dont know a better way to get the first key, value pair out of a map
		for(String retPara:scoreMap.keySet()) {
			maxScore = scoreMap.get(retPara);
			break;
		}
		return maxScore;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
