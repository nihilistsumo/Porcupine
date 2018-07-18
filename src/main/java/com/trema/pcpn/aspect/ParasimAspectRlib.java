package com.trema.pcpn.aspect;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException; 
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import com.trema.prcpn.similarity.ParaSimRankerAspect;

public class ParasimAspectRlib {
	
	public ArrayList<HashSet<String>> split(String titlesPath) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(new File(titlesPath)));
		HashSet<String> titleSet1 = new HashSet<String>();
		HashSet<String> titleSet2 = new HashSet<String>();
		String line = br.readLine();
		boolean set1 = true;
		while(line!=null) {
			if(set1)
				titleSet1.add(line.replaceAll(" ", "%20"));
			else
				titleSet2.add(line.replaceAll(" ", "%20"));
			set1 = !set1;
			line = br.readLine();
		}
		br.close();
		ArrayList<HashSet<String>> splitSets = new ArrayList<HashSet<String>>();
		splitSets.add(titleSet1);
		splitSets.add(titleSet2);
		return splitSets;
	}
	
	public void writeRlibFetFileForTrain(HashMap<String, ArrayList<String>> trainCandSet, HashMap<String, ArrayList<String>> parasimQrels, String fetFileOutputPath, Connection con, 
			IndexSearcher aspectIs, IndexSearcher is, IndexSearcher isNoStops, int retAspNo, String features) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fetFileOutputPath)));
			
			HashMap<String, HashMap<String, HashMap<String, Double>>> scoresMap = new HashMap<String, HashMap<String, HashMap<String, Double>>>();
			System.out.println("Calculating feature scores...");
			int p = 0;
			for(String page:trainCandSet.keySet()) {
				ArrayList<String> retParaList = trainCandSet.get(page);
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
					ExecutorService exec = Executors.newCachedThreadPool();
					for(int j=i+1; j<retParaList.size(); j++) {
						String retPara = retParaList.get(j);
						Runnable parasimThread = new ParasimAspectSimJob(keyPara, retPara, paraAspectMap, con, aspectIs, is, isNoStops, retAspNo, page, scoresMap);
						exec.execute(parasimThread);
					}
					exec.shutdown();
					exec.awaitTermination(7, TimeUnit.DAYS);
					System.out.println(".");
				}
				p++;
				System.out.println(page+" is complete. "+(trainCandSet.keySet().size()-p)+" pages remaining...");
			}
			System.out.println();
			System.out.println("Feature scores calculated.\nWriting feature scores...");
			for(String pageAndKey:scoresMap.keySet()) {
				HashMap<String, HashMap<String, Double>> scoreMap = scoresMap.get(pageAndKey);
				HashMap<String, Double> maxScores = this.getMaxScores(scoreMap, features);
				String pageID = pageAndKey.split("_")[0];
				String keyPara = pageAndKey.split("_")[1];
				ArrayList<String> relParas = parasimQrels.get(pageID+":"+keyPara);
				for(String retPara:scoreMap.keySet()) {
					HashMap<String, Double> scores = scoreMap.get(retPara);
					String fetLine = "";
					if(relParas!=null && relParas.contains(retPara))
						fetLine = "1 qid:"+pageID+":"+keyPara;
					else
						fetLine = "0 qid:"+pageID+":"+keyPara;
					int i = 1;
					for(String feature:features.split(":")) {
						if(maxScores.get(feature)<0.00000001)
							fetLine+=" "+i+":"+scores.get(feature);
						else
							fetLine+=" "+i+":"+scores.get(feature)/maxScores.get(feature);
						i++;
					}
					fetLine+=" #"+retPara;
					//System.out.println(fetLine);
					bw.write(fetLine+"\n");
				}
			}
			bw.close();
			System.out.println("Feature scores written in "+fetFileOutputPath);
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
	
	public void train(HashSet<String> trainTitles, String candSetRunFilePath, String artQrelsPath, String paraSimQrelsPath, String fetFileOutputPath, Connection con, IndexSearcher aspectIs, 
			IndexSearcher is, IndexSearcher isNoStops, String rlibPath, int retAspNo, String features, boolean withTruePagePara) {
		HashMap<String, ArrayList<String>> trainPageParaMap = new HashMap<String, ArrayList<String>>();
		HashMap<String, ArrayList<String>> candPageParaMap = DataUtilities.getPageParaMapFromRunfile(candSetRunFilePath);
		HashMap<String, ArrayList<String>> truePageParaMap = DataUtilities.getTrueArticleParasMapFromPath(artQrelsPath);
		HashMap<String, ArrayList<String>> pageParaMapFull;
		if(withTruePagePara) 
			pageParaMapFull = truePageParaMap;
		else
			pageParaMapFull = candPageParaMap;
		for(String page:pageParaMapFull.keySet()) {
			if(trainTitles.contains(page.split(":")[1])) {
				trainPageParaMap.put(page, pageParaMapFull.get(page));
			}
		}
		HashMap<String, ArrayList<String>> paraSimQrels = DataUtilities.getGTMapQrels(paraSimQrelsPath);
		this.writeRlibFetFileForTrain(trainPageParaMap, paraSimQrels, fetFileOutputPath, con, aspectIs, is, isNoStops, retAspNo, features);
		
		try {
			String command = "java -jar "+rlibPath+" -train "+fetFileOutputPath+" -ranker 4 -metric2t MAP -save "+fetFileOutputPath+"-model";
			System.out.println("Now running rlib with the following command:\n"+command);
			Process p = Runtime.getRuntime().exec(command);
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = "";			
			while ((line = reader.readLine())!= null) {
				System.out.println(line);
			}
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void rank(Properties prop, HashSet<String> titlesSet, String indexDirPath, String indexDirNoStops, String indexDirAspect, String candRunFilePath, 
			String articleQrelsPath, String outRunPath, String rlibModelPath, Connection con, String method, int retAspNo) {
		try {
			ParaSimRankerAspect aspectRanker = new ParaSimRankerAspect();
			aspectRanker.rank(prop, titlesSet, indexDirPath, indexDirNoStops, indexDirAspect, candRunFilePath, articleQrelsPath, outRunPath, rlibModelPath, con, method, retAspNo);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void run2foldCV(Properties prop, String titlesPath, String candSetRunFilePath, String artQrelsPath, String paraSimQrelsPath, String fetFileOutputDir, String aspIsPath, 
			String isPath, String isNoStopPath, String rlibPath, int retAspNo, String withTruePagePara) {
		try {
			ArrayList<HashSet<String>> splittedDataset = this.split(titlesPath);
			HashSet<String> titlesSet1 = splittedDataset.get(0);
			HashSet<String> titlesSet2 = splittedDataset.get(1);
			String features = prop.getProperty("asp-features");
			IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(isPath).toPath()))));
			IndexSearcher isNoStops = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(isNoStopPath).toPath()))));
			IndexSearcher aspectIs = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(aspIsPath).toPath()))));
			Connection con = DataUtilities.getDBConnection(prop.getProperty("dbip"), prop.getProperty("db"), "paraent", prop.getProperty("dbuser"), prop.getProperty("dbpwd"));
			boolean truePagePara = false;
			if(withTruePagePara.equalsIgnoreCase("true"))
				truePagePara = true;
			
			/*
			HashSet<String> tinySet = new HashSet<String>();
			tinySet.add("Chocolate%20chip");
			tinySet.add("Contingent%20work");
			*/
			
			System.out.println("Starting training...");
			this.train(titlesSet1, candSetRunFilePath, artQrelsPath, paraSimQrelsPath, fetFileOutputDir+"/train1-fet", con, aspectIs, is, isNoStops, rlibPath, retAspNo, features, truePagePara);
			this.train(titlesSet2, candSetRunFilePath, artQrelsPath, paraSimQrelsPath, fetFileOutputDir+"/train2-fet", con, aspectIs, is, isNoStops, rlibPath, retAspNo, features, truePagePara);
			System.out.println("Training complete\n");
			
			System.out.println("Startig ranking using trained models...");
			this.rank(prop, titlesSet2, isPath, isNoStopPath, aspIsPath, candSetRunFilePath, artQrelsPath, fetFileOutputDir+"/set2-run", fetFileOutputDir+"/train1-fet-model", con, withTruePagePara, retAspNo);
			this.rank(prop, titlesSet1, isPath, isNoStopPath, aspIsPath, candSetRunFilePath, artQrelsPath, fetFileOutputDir+"/set1-run", fetFileOutputDir+"/train2-fet-model", con, withTruePagePara, retAspNo);
			System.out.println("Ranking complete\n");
			//java -jar ~/Softwares/RankLib-2.1-patched.jar -train parasim-rlib-fet-part -ranker 4 -metric2t MAP -save parasim-rlib-fet-part-model
			
		} catch (IOException | ClassNotFoundException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
