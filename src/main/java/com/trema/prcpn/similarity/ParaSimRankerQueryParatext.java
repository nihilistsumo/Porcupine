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
import java.util.Map;
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
import com.trema.pcpn.util.MapUtil;

import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;

public class ParaSimRankerQueryParatext {
	
	public void rank(String indexDirPath, String candRunFilePath, String articleQrelsPath, String outRunPath, String method, String withTruePagePara) throws IOException {
		IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirPath).toPath()))));
		//IndexSearcher isNoStops = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirNoStops).toPath()))));
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
						QueryParser qpID = new QueryParser("Id", new StandardAnalyzer());
						QueryParser qp = new QueryParser("Text", new StandardAnalyzer());
						Map<String, Double> retrievedResult = new HashMap<String, Double>();
						String queryString = is.doc(is.search(qpID.parse(keyPara), 1).scoreDocs[0].doc).get("Text");
						BooleanQuery.setMaxClauseCount(65536);
						Query q = qp.parse(QueryParser.escape(queryString));
						for(String retPara:retParaIDs) {
							if(!retPara.equals(keyPara)) {
								int retDocID = is.search(qpID.parse(retPara), 1).scoreDocs[0].doc;
								double currScore = is.explain(q, retDocID).getValue();
								retrievedResult.put(retPara, currScore);
							}
						}

						Map<String, Double> sortedResults = MapUtil.sortByValue(retrievedResult);
						int rank = 1;
						for(Map.Entry<String, Double> entry : sortedResults.entrySet()) {
							bw.write(pageID+":"+keyPara+" Q0 "+entry.getKey()+" "+rank+" "+entry.getValue()+" "+method.toUpperCase()+"-MAP\n");
							rank++;
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
