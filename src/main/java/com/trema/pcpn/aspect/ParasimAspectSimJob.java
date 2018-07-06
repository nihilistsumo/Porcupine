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
	
	public ParasimAspectSimJob(ArrayList<String> retParaList, Connection con, IndexSearcher aspectIs, IndexSearcher is, IndexSearcher isNoStops, 
			int retAspNo, HashMap<String, HashMap<String, HashMap<String, Double>>> scoresMap) {
		// TODO Auto-generated constructor stub
		try {
			for(int i=0;i<retParaList.size()-1;i++) {
				String keyPara = retParaList.get(i);
				QueryParser qpID = new QueryParser("paraid", new StandardAnalyzer());
				QueryParser qpAspText = new QueryParser("Text", new StandardAnalyzer());
				AspectSimilarity aspSim = new AspectSimilarity();
				
				String queryString = isNoStops.doc(isNoStops.search(qpID.parse(keyPara), 1).scoreDocs[0].doc).get("parabody");
				BooleanQuery.setMaxClauseCount(65536);
				Query q = qpAspText.parse(QueryParser.escape(queryString));
				TopDocs retAspectsKeyPara = aspectIs.search(q, retAspNo);
				
				HashMap<String, HashMap<String, Double>> retParaScores = new HashMap<String, HashMap<String, Double>>();
				for(int j=i+1; j<retParaList.size(); j++) {
					String retPara = retParaList.get(j);
					String retQueryString = isNoStops.doc(isNoStops.search(qpID.parse(retPara), 1).scoreDocs[0].doc).get("parabody");
					//BooleanQuery.setMaxClauseCount(65536);
					Query retQ = qpAspText.parse(QueryParser.escape(retQueryString));
					TopDocs retAspectsRetPara = aspectIs.search(retQ, retAspNo);
					
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
				}
				scoresMap.put(keyPara, retParaScores);
			}
		} catch (IOException | ParseException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

}
