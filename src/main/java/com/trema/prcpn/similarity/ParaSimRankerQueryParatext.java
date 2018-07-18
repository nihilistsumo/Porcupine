package com.trema.prcpn.similarity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.stream.StreamSupport;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.store.FSDirectory;

import com.trema.pcpn.util.DataUtilities;

import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;

public class ParaSimRankerQueryParatext {
	
	public void rank(String indexDirPath, String indexDirNoStops, String candRunFilePath, String articleQrelsPath, String outRunPath, String method, String withTruePagePara) throws IOException {
		IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirPath).toPath()))));
		IndexSearcher isNoStops = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirNoStops).toPath()))));
		if(method.equalsIgnoreCase("bm25"))
			is.setSimilarity(new BM25Similarity());
		else if(method.equalsIgnoreCase("bool"))
			is.setSimilarity(new BooleanSimilarity());
		else if(method.equalsIgnoreCase("classic"))
			is.setSimilarity(new ClassicSimilarity());
		else if(method.equalsIgnoreCase("lmds"))
			is.setSimilarity(new LMDirichletSimilarity());
		ArrayList<String> allQueries = new ArrayList<String>();
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outRunPath)));
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
						QueryParser qpID = new QueryParser("paraid", new StandardAnalyzer());
						QueryParser qp = new QueryParser("parabody", new StandardAnalyzer());
						HashMap<String, Double> retrievedResult = new HashMap<String, Double>();
						String queryString = isNoStops.doc(isNoStops.search(qpID.parse(keyPara), 1).scoreDocs[0].doc).get("parabody");
						BooleanQuery.setMaxClauseCount(65536);
						Query q = qp.parse(QueryParser.escape(queryString));
						for(String retPara:retParaIDs) {
							if(!retPara.equals(keyPara)) {
								int retDocID = isNoStops.search(qpID.parse(retPara), 1).scoreDocs[0].doc;
								double currScore = is.explain(q, retDocID).getValue();
								retrievedResult.put(retPara, currScore);
							}
						}
						
						/*
						TopDocs tds = is.search(q, retNo*5);
						ScoreDoc[] retDocs = tds.scoreDocs;
						int count = 0;
						for (int i = 0; i < retDocs.length; i++) {
							Document d = is.doc(retDocs[i].doc);
							String retParaID = d.getField("paraid").stringValue();
							if(!retParaID.equals(keyPara) && retParaIDs.contains(retParaID)) {
								retrievedResult.put(d.getField("paraid").stringValue(), tds.scoreDocs[i].score);
								count++;
							}
							if(count>=retNo)
								break;
						}
						*/
						
						for(String para:retrievedResult.keySet()) {
							bw.write(pageID+":"+keyPara+" Q0 "+para+" 0 "+retrievedResult.get(para)+" "+method.toUpperCase()+"-MAP\n");
						}
						System.out.print(".");
					} catch (IOException | ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			System.out.println();
			System.out.println(pageID+" is done\n");
		}
		//System.out.println("Total no. of queries: "+allQueries.size());
		bw.close();
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
