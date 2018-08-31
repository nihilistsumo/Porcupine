package com.trema.pcpn.aspect;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import com.trema.pcpn.util.DataUtilities;
import com.trema.pcpn.util.MapUtil;

public class ParasimAspectRunfileWriter {
	
	public void writeRunFiles(HashMap<String, ArrayList<String>> truePagePara, String outputDirPath, Connection con, IndexSearcher aspectIs, IndexSearcher is, IndexSearcher isNoStops, 
			int retAspNo, String features, String table) {
		try {
			String[] aspectFeatures = features.split(":");
			HashMap<String, BufferedWriter> writers = new HashMap<String, BufferedWriter>();
			for(String fet:aspectFeatures) {
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputDirPath+"/parasim-true-page-para-train--"+fet+"-run")));
				writers.put(fet, bw);
			}
			
			ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, Double>>> scoresMap = new ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, Double>>>();
			int p = 0;
			for(String page:truePagePara.keySet()) {
				ArrayList<String> retParaList = truePagePara.get(page);
				HashMap<String, TopDocs> paraAspectMap = new HashMap<String, TopDocs>();
				System.out.print("Retrieving aspects for "+retParaList.size()+" paras in "+page);
				StreamSupport.stream(retParaList.spliterator(), true).forEach(para -> {
					try {
						QueryParser qpID = new QueryParser("paraid", new StandardAnalyzer());
						QueryParser qpAspText = new QueryParser("Text", new StandardAnalyzer());
						AspectSimilarity aspSim = new AspectSimilarity();
						String queryString = isNoStops.doc(isNoStops.search(qpID.parse(para), 1).scoreDocs[0].doc).get("parabody");
						BooleanQuery.setMaxClauseCount(65536);
						Query q = qpAspText.parse(QueryParser.escape(queryString));
						TopDocs retAspectsPara = aspectIs.search(q, retAspNo);
						paraAspectMap.put(para, retAspectsPara);
						System.out.print(".");
					} catch (IOException | ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
				System.out.println();
				System.out.println("Calculating feature scores");
				for(String keyPara:retParaList) {
					ExecutorService exec = Executors.newFixedThreadPool(10);
					for(String retPara:retParaList) {
						if(!retPara.equalsIgnoreCase(keyPara)) {
							Runnable parasimThread = new ParasimAspectSimJob(keyPara, retPara, paraAspectMap, con, aspectIs, is, isNoStops, retAspNo, page, scoresMap, table);
							exec.execute(parasimThread);
						}
					}
					exec.shutdown();
					exec.awaitTermination(7, TimeUnit.DAYS);
					System.out.println("+");
				}
				p++;
				System.out.println(page+" is complete. "+(truePagePara.keySet().size()-p)+" pages remaining...");
			}
			
			System.out.println("Producing run files..");
			for(String query:scoresMap.keySet()) {
				ConcurrentHashMap<String, ConcurrentHashMap<String, Double>> retScores = scoresMap.get(query);
				for(String fet:aspectFeatures) {
					Map<String, Double> unsortedScores = new HashMap<String, Double>();
					for(String retPara: retScores.keySet())
						unsortedScores.put(retPara, retScores.get(retPara).get(fet));
					Map<String, Double> sortedScores = MapUtil.sortByValue(unsortedScores);
					int i=1;
					for (Map.Entry<String, Double> entry : sortedScores.entrySet()) {
						writers.get(fet).write(query.replace("_", ":")+" Q0 "+entry.getKey()+" "+i+" "+entry.getValue()+" PARASIM-"+fet+"\n");
						i++;
					}	
				}
			}
			for(String fet:aspectFeatures)
				writers.get(fet).close();
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void run(Properties prop, String artQrelsPath, String isPath, String isNoStopPath, String aspIsPath, int retAspNo, String outputDirPath) {
		try {
			String features = prop.getProperty("asp-features");
			IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(isPath).toPath()))));
			IndexSearcher isNoStops = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(isNoStopPath).toPath()))));
			IndexSearcher aspectIs = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(aspIsPath).toPath()))));
			Connection con = DataUtilities.getDBConnection(prop.getProperty("dbip"), prop.getProperty("db"), prop.getProperty("dbtable"), prop.getProperty("dbuser"), prop.getProperty("dbpwd"));
			HashMap<String, ArrayList<String>> truePageParaMap = DataUtilities.getTrueArticleParasMapFromPath(artQrelsPath);
			this.writeRunFiles(truePageParaMap, outputDirPath, con, aspectIs, is, isNoStops, retAspNo, features, prop.getProperty("dbtable"));
		} catch (ClassNotFoundException | IOException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void run(Properties prop, String artQrelsPath, String isPath, String isNoStopPath, String aspIsPath, int retAspNo, String outputDirPath, ArrayList<String> onlyThesePages) {
		try {
			String features = prop.getProperty("asp-features");
			IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(isPath).toPath()))));
			IndexSearcher isNoStops = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(isNoStopPath).toPath()))));
			IndexSearcher aspectIs = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(aspIsPath).toPath()))));
			Connection con = DataUtilities.getDBConnection(prop.getProperty("dbip"), prop.getProperty("db"), prop.getProperty("dbtable"), prop.getProperty("dbuser"), prop.getProperty("dbpwd"));
			HashMap<String, ArrayList<String>> truePageParaMap = DataUtilities.getTrueArticleParasMapFromPath(artQrelsPath);
			HashMap<String, ArrayList<String>> shortPageParaMap = new HashMap<String, ArrayList<String>>();
			for(String page:truePageParaMap.keySet()) {
				if(onlyThesePages.contains(page))
					shortPageParaMap.put(page, truePageParaMap.get(page));
			}
			this.writeRunFiles(shortPageParaMap, outputDirPath, con, aspectIs, is, isNoStops, retAspNo, features, prop.getProperty("dbtable"));
		} catch (ClassNotFoundException | IOException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
