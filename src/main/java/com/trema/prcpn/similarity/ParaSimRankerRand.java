package com.trema.prcpn.similarity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.stream.StreamSupport;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import com.trema.pcpn.util.DataUtilities;

public class ParaSimRankerRand {
	
	public void rank(String candRunFilePath, String articleQrelsPath, String outRunPath, String withTruePagePara) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outRunPath)));
		Random rand = new Random();
		HashMap<String, ArrayList<String>> candPageParaMap = DataUtilities.getPageParaMapFromRunfile(candRunFilePath);
		HashMap<String, ArrayList<String>> truePageParaMap = DataUtilities.getTrueArticleParasMapFromPath(articleQrelsPath);
		HashMap<String, ArrayList<String>> pageParaMap = candPageParaMap;
		if(withTruePagePara.equalsIgnoreCase("true"))
			pageParaMap = truePageParaMap;
		for(String pageID:pageParaMap.keySet()) {
			ArrayList<String> retParaIDs = pageParaMap.get(pageID);
			StreamSupport.stream(retParaIDs.spliterator(), true).forEach(keyPara -> { 
				if(truePageParaMap.get(pageID).contains(keyPara)) {
					try {
						HashMap<String, Double> retrievedResult = new HashMap<String, Double>();
						for(String retPara:retParaIDs) {
							if(!retPara.equals(keyPara)) {
								double currScore = rand.nextDouble();
								retrievedResult.put(retPara, currScore);
							}
						}
						
						for(String para:retrievedResult.keySet()) {
							bw.write(pageID+":"+keyPara+" Q0 "+para+" 0 "+retrievedResult.get(para)+" RANDOM-MAP\n");
						}
						System.out.print(".");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			System.out.println();
			System.out.println(pageID+" is done\n");
		}
		bw.close();
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
	}

}
