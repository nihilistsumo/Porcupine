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

import org.apache.lucene.queryparser.classic.ParseException;

import com.trema.pcpn.cl.ClusteringMetrics;
import com.trema.pcpn.cl.CustomHAC;
import com.trema.pcpn.cl.CustomKMeans;
import com.trema.pcpn.cl.RandomClustering;


public class PorcupineHelper {
	
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
		double rand, fmeasure, meanRand = 0, meanF = 0;
		int count = 0;
		for(String pageid:candClusters.keySet()){
			ClusteringMetrics cm = new ClusteringMetrics(DataUtilities.getGTClusters(
					pageid, p.getProperty("data-dir")+"/"+p.getProperty("top-qrels")), candClusters.get(pageid), false);
			rand = cm.getAdjRAND();
			fmeasure = cm.fMeasure();
			meanRand+=rand;
			meanF+=fmeasure;
			count++;
			System.out.println(pageid+": Adj RAND = "+rand+", fmeasure = "+fmeasure);
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
