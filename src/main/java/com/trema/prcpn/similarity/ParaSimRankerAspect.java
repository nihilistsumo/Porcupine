package com.trema.prcpn.similarity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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

import com.trema.pcpn.aspect.AspectSimilarity;
import com.trema.pcpn.aspect.ParasimAspectSimJob;
import com.trema.pcpn.util.DataUtilities;

public class ParaSimRankerAspect {
	
	public void rank(Properties prop, HashSet<String> titlesSet, String indexDirPath, String indexDirNoStops, String indexDirAspect, String candRunFilePath, 
			String articleQrelsPath, String outRunPath, String rlibModelPath, Connection con, String method, int retAspNo) throws IOException {
		IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirPath).toPath()))));
		IndexSearcher isNoStops = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirNoStops).toPath()))));
		IndexSearcher isAsp = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirAspect).toPath()))));
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outRunPath)));
		double[] optW = this.getWeightVecFromRlibModel(rlibModelPath);
		String features = prop.getProperty("asp-features");
		
		HashMap<String, ArrayList<String>> candPageParaMap = DataUtilities.getPageParaMapFromRunfile(candRunFilePath);
		HashMap<String, ArrayList<String>> truePageParaMap = DataUtilities.getTrueArticleParasMapFromPath(articleQrelsPath);
		HashMap<String, ArrayList<String>> pageParaMap = candPageParaMap;
		if(method.equalsIgnoreCase("true"))
			pageParaMap = truePageParaMap;
		
		HashMap<String, HashMap<String, HashMap<String, Double>>> scoresMap = new HashMap<String, HashMap<String, HashMap<String, Double>>>();
		System.out.println("Calculating feature scores...");
		int p=0;
		for(String pageID:pageParaMap.keySet()) {
			if(titlesSet.contains(pageID.split(":")[1])) {
				ArrayList<String> retParaIDs = pageParaMap.get(pageID);
				
				HashMap<String, TopDocs> paraAspectMap = new HashMap<String, TopDocs>();
				System.out.print("Retrieving aspects for "+retParaIDs.size()+" paras in "+pageID);
				StreamSupport.stream(retParaIDs.spliterator(), true).forEach(para -> {
					try {
						QueryParser qpID = new QueryParser("paraid", new StandardAnalyzer());
						QueryParser qpAspText = new QueryParser("Text", new StandardAnalyzer());
						AspectSimilarity aspSim = new AspectSimilarity();
						String queryString = isNoStops.doc(isNoStops.search(qpID.parse(para), 1).scoreDocs[0].doc).get("parabody");
						BooleanQuery.setMaxClauseCount(65536);
						Query q = qpAspText.parse(QueryParser.escape(queryString));
						TopDocs retAspectsPara = isAsp.search(q, retAspNo);
						paraAspectMap.put(para, retAspectsPara);
						System.out.print(".");
					} catch (IOException | ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
				
				System.out.println();
				System.out.println("Calculating feature scores");
				try {
					for(String keyPara:retParaIDs) {
						if(truePageParaMap.get(pageID).contains(keyPara)) {
							ExecutorService exec = Executors.newCachedThreadPool();
							for(String retPara:retParaIDs) {
								if(!keyPara.equals(retPara)) {
									Runnable parasimThread = new ParasimAspectSimJob(keyPara, retPara, paraAspectMap, con, isAsp, is, isNoStops, retAspNo, pageID, scoresMap);
									exec.execute(parasimThread);
								}
							}
							exec.shutdown();
							exec.awaitTermination(7, TimeUnit.DAYS);
							System.out.println(".");
						}
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				p++;
				System.out.println(pageID+" is complete. "+(pageParaMap.keySet().size()-p)+" pages remaining...");
				System.out.println("Feature scores calculated.\nWriting run file...");
				
				for(String pageAndKey:scoresMap.keySet()) {
					HashMap<String, HashMap<String, Double>> scoreMap = scoresMap.get(pageAndKey);
					HashMap<String, Double> maxScores = this.getMaxScores(scoreMap, features);
					String page = pageAndKey.split("_")[0];
					String keyPara = pageAndKey.split("_")[1];
					for(String retPara:scoreMap.keySet()) {
						HashMap<String, Double> scores = scoreMap.get(retPara);
						double score = 0;
						String rfLine = "";
						String[] featureArr = features.split(":");
						for(int f=0; f<featureArr.length; f++) {
							double fetScore = scores.get(featureArr[f]);
							if(maxScores.get(featureArr[f])>0.00000001)
								fetScore = fetScore/maxScores.get(featureArr[f]);
							score = score+fetScore*optW[f];
						}
						rfLine = page+":"+keyPara+" Q0 "+retPara+" 0 "+score+" ASP-COMBINED";
						bw.write(rfLine+"\n");
					}
				}
				bw.close();
				System.out.println("Run file written in "+outRunPath);
			}
		}
		//System.out.println("Total no. of queries: "+allQueries.size());
		bw.close();
	}
	
	public HashMap<String, Double> getMaxScores(HashMap<String, HashMap<String, Double>> scoreMap, String features) {
		HashMap<String, Double> maxScores = new HashMap<String, Double>();
		for(String feature:features.split(":")) {
			double max = 0;
			double currScore = 0;
			for(String retPara:scoreMap.keySet()) {
				currScore = scoreMap.get(retPara).get(feature);
				if(currScore>max)
					max = currScore;
			}
			maxScores.put(feature, max);
		}
		return maxScores;
	}
	
	public double[] getWeightVecFromRlibModel(String modelFilePath) throws IOException{
		double[] weightVec;
		BufferedReader br = new BufferedReader(new FileReader(new File(modelFilePath)));
		String line = br.readLine();
		while(line!=null && line.startsWith("#"))
			line = br.readLine();
		String[] values = line.split(" ");
		weightVec = new double[values.length];
		for(int i=0; i<values.length; i++)
			weightVec[i] = Double.parseDouble(values[i].split(":")[1]);
		return weightVec;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
