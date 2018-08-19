package com.trema.pcpn.aspect;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
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
			
			HashMap<String, HashMap<String, HashMap<String, Double>>> scoresMap = new HashMap<String, HashMap<String, HashMap<String, Double>>>();
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
				for(int i=0;i<retParaList.size()-1;i++) {
					String keyPara = retParaList.get(i);
					ExecutorService exec = Executors.newFixedThreadPool(10);
					for(int j=i+1; j<retParaList.size(); j++) {
						String retPara = retParaList.get(j);
						Runnable parasimThread = new ParasimAspectSimJob(keyPara, retPara, paraAspectMap, con, aspectIs, is, isNoStops, retAspNo, page, scoresMap, table);
						exec.execute(parasimThread);
					}
					exec.shutdown();
					exec.awaitTermination(7, TimeUnit.DAYS);
					System.out.println(".");
				}
				p++;
				System.out.println(page+" is complete. "+(truePagePara.keySet().size()-p)+" pages remaining...");
			}
			
			System.out.println("Producing run files..");
			for(String query:scoresMap.keySet()) {
				HashMap<String, HashMap<String, Double>> retScores = scoresMap.get(query);
				for(String retPara: retScores.keySet()) {
					for(String fet:aspectFeatures) {
						writers.get(fet).write(query+" Q0 "+retPara+" 0 "+retScores.get(retPara).get(fet)+" PARASIM-"+fet+"\n");
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

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
