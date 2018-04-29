package com.trema.pcpn.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.stream.StreamSupport;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

import com.trema.pcpn.cl.ClusteringMetrics;
import com.trema.pcpn.cl.CustomHAC;
import com.trema.pcpn.cl.CustomKMeans;
import com.trema.pcpn.cl.RandomClustering;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.JiangConrath;
import edu.cmu.lti.ws4j.impl.Lin;
import edu.cmu.lti.ws4j.impl.Path;
import edu.cmu.lti.ws4j.impl.WuPalmer;


public class PorcupineHelper {
	
	public void runSimilarityRanker(Properties p, boolean withTruePage, String type) throws IOException, ParseException, ClassNotFoundException{
		HashMap<String, HashMap<String, Double>> paraPairRun = new HashMap<String, HashMap<String, Double>>();
		HashMap<String, ArrayList<String>> pageSecMap = DataUtilities.getArticleToplevelSecMap(p.getProperty("data-dir")+"/"+p.getProperty("outline"));
		/*
		HashMap<String, ArrayList<String>> pageParaMapRunFile = DataUtilities.getPageParaMapFromRunfile(
				p.getProperty("out-dir")+"/"+p.getProperty("trec-runfile"));
		*/
		
		
		HashMap<String, ArrayList<String>> pageParaMap;
		if(withTruePage)
			pageParaMap = DataUtilities.getGTMapQrels(p.getProperty("data-dir")+"/"+p.getProperty("art-qrels"));
		else
			pageParaMap = DataUtilities.getPageParaMapFromRunfile(p.getProperty("out-dir")+"/"+p.getProperty("trec-runfile"));
		
		if (!type.startsWith("wn")) {
			HashMap<String, double[]> gloveVecs = DataUtilities.readGloveFile(p);
			int vecSize = gloveVecs.get("the").length;
			StreamSupport.stream(pageSecMap.keySet().spliterator(), true).forEach(page -> {
				try {
					ArrayList<String> paraIDsInPage = pageParaMap.get(page);
					HashMap<String, double[]> paraVecMap;
					if (type.equalsIgnoreCase("w2v"))// word2vec
						paraVecMap = DataUtilities.getParaW2VVecMap(p, paraIDsInPage, gloveVecs, vecSize);
					else if (type.equalsIgnoreCase("tm"))//topic model
						paraVecMap = DataUtilities.getParaTMVecMap(p, paraIDsInPage);
					else if (type.equalsIgnoreCase("rnd"))//random vector
						paraVecMap = DataUtilities.getParaRandomVecMap(p, paraIDsInPage);
					else//tfidf
						paraVecMap = DataUtilities.getParaTFIDFVecMap(p, paraIDsInPage);
					
					for(String p1:paraVecMap.keySet()) {
						for(String p2:paraVecMap.keySet()) {
							if(!p1.equals(p2)) {
								if(paraPairRun.containsKey(p1)) {
									if(!paraPairRun.get(p1).containsKey(p2))
										paraPairRun.get(p1).put(p2, DataUtilities.getDotProduct(paraVecMap.get(p1), paraVecMap.get(p2)));
								}
								else {
									HashMap<String, Double> newMap = new HashMap<String, Double>();
									newMap.put(p2, DataUtilities.getDotProduct(paraVecMap.get(p1), paraVecMap.get(p2)));
									paraPairRun.put(p1, newMap);
								}
							}
						}
					}
					System.out.println(page+" done");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			});
		}
		else {
			IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(p.getProperty("index-dir")).toPath()))));
			ILexicalDatabase db = new NictWordNet();
			StreamSupport.stream(pageSecMap.keySet().spliterator(), true).forEach(page -> {
				try {
					ArrayList<String> paraIDsInPage = pageParaMap.get(page);
					System.out.println("Started "+page+" with "+paraIDsInPage.size()+" paras");
					Analyzer analyzer = new StandardAnalyzer();
					QueryParser qp = new QueryParser("paraid", analyzer);
					//calculate wordnet similarity between all para pairs
					String p1Text, p2Text;
					int c = 1;
					for(String p1:paraIDsInPage) {
						for(String p2:paraIDsInPage) {
							if(!p1.equals(p2)) {
								p1Text = is.doc(is.search(qp.parse(p1), 1).scoreDocs[0].doc).get("parabody");
								p2Text = is.doc(is.search(qp.parse(p2), 1).scoreDocs[0].doc).get("parabody");
								if(paraPairRun.containsKey(p1)) {
									if(!paraPairRun.get(p1).containsKey(p2))
										paraPairRun.get(p1).put(p2, this.getSimilarityScore(analyzer, p1Text, p2Text, type, db));
								}
								else {
									HashMap<String, Double> newMap = new HashMap<String, Double>();
									newMap.put(p2, this.getSimilarityScore(analyzer, p1Text, p2Text, type, db));
									paraPairRun.put(p1, newMap);
								}
								//System.out.println("Simscore calculated for "+p1+" : "+p2);
								//System.out.println(c+"para Pairs");
								c++;
							}
						}
					}
					System.out.println(page+" done");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
		}
		BufferedWriter bw = new BufferedWriter(new FileWriter(
				new File(p.getProperty("out-dir")+"/"+p.getProperty("sim-rank-out"))));
		for(String p1:paraPairRun.keySet()) {
			for(String p2:paraPairRun.get(p1).keySet()) {
				bw.write(p1+" Q0 "+p2+" 0 "+paraPairRun.get(p1).get(p2)+" SIMRANK\n");
			}
		}
		bw.close();
	}
	
	public double getSimilarityScore(Analyzer a, String text1, String text2, String type, ILexicalDatabase db) {
		double score = 0;
		RelatednessCalculator rc = null;
		//Jiang-Conrath, Path, Wu-Palmer, Lin
		if(type.endsWith("ji"))
			rc = new JiangConrath(db);
		else if(type.endsWith("path"))
			rc = new Path(db);
		else if(type.endsWith("wu"))
			rc = new WuPalmer(db);
		else if(type.endsWith("lin"))
			rc = new Lin(db);
		else {
			System.out.println("No such wordnet similarity!");
			System.exit(0);
		}
		ArrayList<String> text1list = DataUtilities.getTopFrequentTokens(a, text1, 10);
		ArrayList<String> text2list = DataUtilities.getTopFrequentTokens(a, text2, 10);
		
		int count = 0;
		for(String w1:text1list) {
			for(String w2:text2list) {
				if(!w1.equalsIgnoreCase(w2)) {
					score+=rc.calcRelatednessOfWords(w1, w2);
					count++;
				}
				else {
					score+=1.0;
					count++;
				}
			}
		}
		if(count<1)
			return 0;
		else
			return score/count;
	}
	
	public void convertClusterDataToText(Properties p) throws FileNotFoundException, IOException, ClassNotFoundException{
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(p.getProperty("out-dir")+"/"+p.getProperty("cluster-out"))));
		HashMap<String, ArrayList<ArrayList<String>>> resultPageClusters = (HashMap<String, ArrayList<ArrayList<String>>>) ois.readObject();
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(p.getProperty("out-dir")+"/"+p.getProperty("cluster-out-txt"))));
		for(String page:resultPageClusters.keySet()){
			bw.write("PAGE:"+page+"\n");
			ArrayList<ArrayList<String>> clusters = resultPageClusters.get(page);
			for(ArrayList<String> cl:clusters){
				for(String para:cl){
					bw.write(para+" ");
				}
				bw.write("\n");
			}
		}
		ois.close();
		bw.close();
	}
	
	public void runClusteringMeasure(Properties p) throws FileNotFoundException, IOException, ClassNotFoundException{
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(
				p.getProperty("out-dir")+"/"+p.getProperty("cluster-out"))));
		HashMap<String, ArrayList<ArrayList<String>>> candClusters = (HashMap<String, ArrayList<ArrayList<String>>>) ois.readObject();
		ois.close();
		double rand, fmeasure, meanRand = 0, meanF = 0;
		int count = 0;
		System.out.println("PAGE-ID ADJ-RAND FMEASURE");
		for(String pageid:candClusters.keySet()){
			ClusteringMetrics cm = new ClusteringMetrics(DataUtilities.getGTClusters(
					pageid, p.getProperty("data-dir")+"/"+p.getProperty("top-qrels")), candClusters.get(pageid), false);
			rand = cm.getAdjRAND();
			fmeasure = cm.fMeasure();
			meanRand+=rand;
			meanF+=fmeasure;
			count++;
			System.out.println(pageid+" "+rand+" "+fmeasure);
		}
		meanRand/=count;
		meanF/=count;
		System.out.println("Mean Adj RAND = "+meanRand+", mean fmeasure = "+meanF);
	}
	
	public void runRandomClustering(Properties p, boolean withTruePage) throws FileNotFoundException, IOException {
		HashMap<String, ArrayList<ArrayList<String>>> resultPageClusters = new HashMap<String, ArrayList<ArrayList<String>>>();
		HashMap<String, ArrayList<String>> pageSecMap = DataUtilities.getArticleToplevelSecMap(p.getProperty("data-dir")+"/"+p.getProperty("outline"));
		
		HashMap<String, ArrayList<String>> pageParaMap;
		if(withTruePage)
			pageParaMap = DataUtilities.getGTMapQrels(p.getProperty("data-dir")+"/"+p.getProperty("art-qrels"));
		else
			pageParaMap = DataUtilities.getPageParaMapFromRunfile(p.getProperty("out-dir")+"/"+p.getProperty("trec-runfile"));
		
		StreamSupport.stream(pageSecMap.keySet().spliterator(), true).forEach(page -> { 
			try {
				ArrayList<String> paraIDsInPage = pageParaMap.get(page); 
				//ArrayList<String> paraIDsInPage = pageParaMapArtQrels.get(page);
				ArrayList<String> secIDsInPage = pageSecMap.get(page);
				//ArrayList<ParaPairData> ppdList = similarityData.get(page);
				RandomClustering rc = new RandomClustering(p, page, paraIDsInPage, secIDsInPage);
				resultPageClusters.put(page, rc.cluster());
				System.out.println("Clustering done for "+page);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		});
		
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
				new File(p.getProperty("out-dir")+"/"+p.getProperty("cluster-out"))));
		oos.writeObject(resultPageClusters);
		oos.close();
	}
	
	// properties file, withTruePage - whether use true page para map or pagerun file as candidate set
	// vecType - which method to use to get vectors representing each para
	public void runHACClustering(Properties p, boolean withTruePage, String vecType) throws IOException, ParseException, ClassNotFoundException{
		HashMap<String, ArrayList<ArrayList<String>>> resultPageClusters = new HashMap<String, ArrayList<ArrayList<String>>>();
		HashMap<String, ArrayList<String>> pageSecMap = DataUtilities.getArticleToplevelSecMap(p.getProperty("data-dir")+"/"+p.getProperty("outline"));
		/*
		HashMap<String, ArrayList<String>> pageParaMapRunFile = DataUtilities.getPageParaMapFromRunfile(
				p.getProperty("out-dir")+"/"+p.getProperty("trec-runfile"));
		*/
		HashMap<String, double[]> gloveVecs = DataUtilities.readGloveFile(p);
		int vecSize = gloveVecs.get("the").length;
		HashMap<String, ArrayList<String>> pageParaMap;
		if(withTruePage)
			pageParaMap = DataUtilities.getGTMapQrels(p.getProperty("data-dir")+"/"+p.getProperty("art-qrels"));
		else
			pageParaMap = DataUtilities.getPageParaMapFromRunfile(p.getProperty("out-dir")+"/"+p.getProperty("trec-runfile"));
		
		StreamSupport.stream(pageSecMap.keySet().spliterator(), true).forEach(page -> { 
			try {
				ArrayList<String> paraIDsInPage = pageParaMap.get(page); 
				//ArrayList<String> paraIDsInPage = pageParaMapArtQrels.get(page);
				ArrayList<String> secIDsInPage = pageSecMap.get(page);
				//ArrayList<ParaPairData> ppdList = similarityData.get(page);
				HashMap<String, double[]> paraVecMap;
				if(vecType.equalsIgnoreCase("w2v"))// word2vec
					paraVecMap = DataUtilities.getParaW2VVecMap(p, paraIDsInPage, gloveVecs, vecSize);
				else if(vecType.equalsIgnoreCase("tm"))//topic model
					paraVecMap = DataUtilities.getParaTMVecMap(p, paraIDsInPage);
				else//tfidf
					paraVecMap = DataUtilities.getParaTFIDFVecMap(p, paraIDsInPage);
				CustomHAC hac = new CustomHAC(p, page, paraIDsInPage, secIDsInPage, paraVecMap);
				resultPageClusters.put(page, hac.cluster());
				System.out.println("Clustering done for "+page);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		});
		
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
				new File(p.getProperty("out-dir")+"/"+p.getProperty("cluster-out"))));
		oos.writeObject(resultPageClusters);
		oos.close();
	}
	
	// properties file, withTruePage - whether use true page para map or pagerun file as candidate set
	// vecType - which method to use to get vectors representing each para
	public void runKMeansClustering(Properties p, boolean withTruePage, String vecType) throws IOException, ParseException{
		HashMap<String, ArrayList<ArrayList<String>>> resultPageClusters = new HashMap<String, ArrayList<ArrayList<String>>>();
		HashMap<String, ArrayList<String>> pageSecMap = DataUtilities.getArticleToplevelSecMap(
				p.getProperty("data-dir")+"/"+p.getProperty("outline"));
		
		HashMap<String, ArrayList<String>> pageParaMap;
		if(withTruePage)
			pageParaMap = DataUtilities.getGTMapQrels(p.getProperty("data-dir")+"/"+p.getProperty("art-qrels"));
		else
			pageParaMap = DataUtilities.getPageParaMapFromRunfile(p.getProperty("out-dir")+"/"+p.getProperty("trec-runfile"));
		HashMap<String, double[]> gloveVecs = DataUtilities.readGloveFile(p);
		int vecSize = gloveVecs.entrySet().iterator().next().getValue().length;
		
		StreamSupport.stream(pageSecMap.keySet().spliterator(), true).forEach(page -> { 
			try {
				ArrayList<String> paraIDsInPage = pageParaMap.get(page);
				//ArrayList<String> paraIDsInPage = pageParaMapArtQrels.get(page);
				ArrayList<String> secIDsInPage = pageSecMap.get(page);
				//ArrayList<ParaPairData> ppdList = similarityData.get(page);
				HashMap<String, double[]> paraVecMap;
				if(vecType.equalsIgnoreCase("w2v"))// word2vec
					paraVecMap = DataUtilities.getParaW2VVecMap(p, paraIDsInPage, gloveVecs, vecSize);
				else if(vecType.equalsIgnoreCase("tm"))//topic model
					paraVecMap = DataUtilities.getParaTMVecMap(p, paraIDsInPage);
				else//tfidf
					paraVecMap = DataUtilities.getParaTFIDFVecMap(p, paraIDsInPage);
				CustomKMeans kmeans = new CustomKMeans(p, page, secIDsInPage, paraVecMap);
				if(paraIDsInPage.size()<secIDsInPage.size())
					resultPageClusters.put(page, kmeans.cluster(paraIDsInPage.size(), false));
				else
					resultPageClusters.put(page, kmeans.cluster(secIDsInPage.size(), false));
				System.out.println("Clustering done for "+page);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		});
		
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
				new File(p.getProperty("out-dir")+"/"+p.getProperty("cluster-out"))));
		oos.writeObject(resultPageClusters);
		oos.close();
	}

}
