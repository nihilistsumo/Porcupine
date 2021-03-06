package com.trema.pcpn.aspect;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;

public class ParasimAspectSimJob implements Runnable {
	
	public ParasimAspectSimJob(String keyPara, String retPara, HashMap<String, TopDocs> paraAspects, Connection con, IndexSearcher aspectIs, IndexSearcher is, IndexSearcher isNoStops, 
			int retAspNo, String pageID, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, Double>>> scoresMap, String table) {
		// TODO Auto-generated constructor stub
		try {
			if(scoresMap.containsKey(pageID+"_"+retPara) && scoresMap.get(pageID+"_"+retPara).containsKey(keyPara)) {
				if(!scoresMap.containsKey(pageID+"_"+keyPara)) {
					ConcurrentHashMap<String, ConcurrentHashMap<String, Double>> retParaScores = new ConcurrentHashMap<String, ConcurrentHashMap<String, Double>>();
					retParaScores.put(retPara, scoresMap.get(pageID+"_"+retPara).get(keyPara));
					scoresMap.put(pageID+"_"+keyPara, retParaScores);
				}
				else
					scoresMap.get(pageID+"_"+keyPara).put(retPara, scoresMap.get(pageID+"_"+retPara).get(keyPara));
				System.out.print(".");
			}
			else {
				/*
				if(keyPara.equalsIgnoreCase("46fd59de2909ece56f52284188694c3a52dbcbe7") || retPara.equalsIgnoreCase("46fd59de2909ece56f52284188694c3a52dbcbe7"))
					System.out.println("debug");
				*/
				TopDocs retAspectsKeyPara = paraAspects.get(keyPara);
				TopDocs retAspectsRetPara = paraAspects.get(retPara);
	
				ConcurrentHashMap<String, Double> featureScores = new ConcurrentHashMap<String, Double>();
				if(Float.isNaN(retAspectsKeyPara.getMaxScore()) || Float.isNaN(retAspectsRetPara.getMaxScore())) {
					featureScores.put("asprel", 0.0);
					featureScores.put("asptext", 0.0);
					featureScores.put("asplead", 0.0);
					featureScores.put("aspmatch", 0.0);
					featureScores.put("entmatch", 0.0);
				}
				else {
					AspectSimilarity aspSim = new AspectSimilarity();
					double[] aspScore = aspSim.aspectRelationScore(retAspectsKeyPara, retAspectsRetPara, is, aspectIs, con, "na", table);
					double aspectMatchRatio = aspSim.aspectMatchRatio(retAspectsKeyPara.scoreDocs, retAspectsRetPara.scoreDocs);
					double entMatchRatio = aspSim.entityMatchRatio(keyPara, retPara, con, "na", table);
		
					featureScores.put("asprel", aspScore[0]);
					featureScores.put("asptext", aspScore[1]);
					featureScores.put("asplead", aspScore[2]);
					featureScores.put("aspmatch", aspectMatchRatio);
					featureScores.put("entmatch", entMatchRatio);
				}
				
				if(!scoresMap.containsKey(pageID+"_"+keyPara)) {
					ConcurrentHashMap<String, ConcurrentHashMap<String, Double>> retParaScores = new ConcurrentHashMap<String, ConcurrentHashMap<String, Double>>();
					retParaScores.put(retPara, featureScores);
					scoresMap.put(pageID+"_"+keyPara, retParaScores);
				}
				else
					scoresMap.get(pageID+"_"+keyPara).put(retPara, featureScores);
				System.out.print(".");
			}
		} catch (IOException | ParseException | SQLException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
