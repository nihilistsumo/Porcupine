package com.trema.pcpn.aspect;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;

public class ParasimAspectSimJob implements Runnable {
	
	public ParasimAspectSimJob(String keyPara, String retPara, HashMap<String, TopDocs> paraAspects, Connection con, IndexSearcher aspectIs, IndexSearcher is, IndexSearcher isNoStops, 
			int retAspNo, String pageID, HashMap<String, HashMap<String, HashMap<String, Double>>> scoresMap) {
		// TODO Auto-generated constructor stub
		try {
			TopDocs retAspectsKeyPara = paraAspects.get(keyPara);
			TopDocs retAspectsRetPara = paraAspects.get(retPara);

			HashMap<String, HashMap<String, Double>> retParaScores = new HashMap<String, HashMap<String, Double>>();
			AspectSimilarity aspSim = new AspectSimilarity();
			double aspRelScore = aspSim.aspectRelationScore(retAspectsKeyPara, retAspectsRetPara, is, aspectIs, con, "ent", "na");
			double aspTextScore = aspSim.aspectRelationScore(retAspectsKeyPara, retAspectsRetPara, is, aspectIs, con, "asptext", "na");
			double aspLeadScore = aspSim.aspectRelationScore(retAspectsKeyPara, retAspectsRetPara, is, aspectIs, con, "asplead", "na");
			double aspectMatchRatio = aspSim.aspectMatchRatio(retAspectsKeyPara.scoreDocs, retAspectsRetPara.scoreDocs);
			double entMatchRatio = aspSim.entityMatchRatio(keyPara, retPara, con, "na");

			HashMap<String, Double> featureScores = new HashMap<String, Double>();
			featureScores.put("asprel", aspRelScore);
			featureScores.put("asptext", aspTextScore);
			featureScores.put("asplead", aspLeadScore);
			featureScores.put("aspmatch", aspectMatchRatio);
			featureScores.put("entmatch", entMatchRatio);

			retParaScores.put(retPara, featureScores);
			scoresMap.put(pageID+"_"+keyPara, retParaScores);
			System.out.print(".");
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
