package com.trema.pcpn.cl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

import org.apache.lucene.queryparser.classic.ParseException;

import com.trema.pcpn.util.DataUtilities;

import cc.mallet.cluster.Clustering;
import cc.mallet.cluster.KMeans;
import cc.mallet.pipe.Array2FeatureVector;
import cc.mallet.pipe.Input2CharSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2Label;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.NormalizedDotProductMetric;

public class CustomKMeans {
	
	private Properties prop;
	private String pageID;
	private int secNo;
	private HashMap<String, double[]> paraVecMap;
	private ArrayList<String> secids;
	
	public CustomKMeans(Properties p, String pID, ArrayList<String> sectionIDs, HashMap<String, double[]> vecs){
		this.prop = p;
		this.pageID = pID;
		this.secNo = sectionIDs.size();
		this.paraVecMap = vecs;
		this.secids = sectionIDs;
	}
	
	public ArrayList<ArrayList<String>> cluster(int numClusters, boolean print){
		ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
		InstanceList instanceList = new InstanceList(new SerialPipes(Arrays.asList(new Target2Label(), new Array2FeatureVector())));
		for(String paraid : this.paraVecMap.keySet()){
			Instance i = new Instance(this.paraVecMap.get(paraid), paraid, paraid, null);
			instanceList.addThruPipe(i);
		}
		KMeans kmeans = new KMeans(instanceList.getPipe(), numClusters, new NormalizedDotProductMetric(), KMeans.EMPTY_DROP);
		Clustering clusterData = kmeans.cluster(instanceList);
		result = formatClusterData(clusterData);
		if(print)
			this.printClusters(result);
		return result;
	}
	
	public ArrayList<ArrayList<String>> cluster(boolean print){
		return this.cluster(this.secids.size(), print);
	}
	
	private ArrayList<ArrayList<String>> formatClusterData(Clustering rawClusterData){
		ArrayList<ArrayList<String>> finalClusterData = new ArrayList<ArrayList<String>>();
		for(InstanceList iList : rawClusterData.getClusters()){
			ArrayList<String> paraIDList = new ArrayList<String>();
			for(Instance i : iList)
				paraIDList.add(i.getName().toString());
			finalClusterData.add(paraIDList);
		}
		return finalClusterData;
	}
	
	private void printClusters(ArrayList<ArrayList<String>> result){
		int count = 1;
		for(ArrayList<String> cl:result){
			System.out.print("Cluster "+count+" size "+cl.size()+": ");
			for(String paraid:cl)
				System.out.print(paraid+" ");
			System.out.println();
		}
	}
	
	public static void main(String[] args){
		Properties p = new Properties();
		try {
			p.load(new FileInputStream(new File("project.properties")));
			HashMap<String, ArrayList<String>> retPageParaMap = DataUtilities.getPageParaMapFromRunfile(
					p.getProperty("out-dir")+"/"+p.getProperty("trec-runfile"));
			for(String page:retPageParaMap.keySet()){
				System.out.println(page+" started");
				//DataUtilities.getParaVecMap(p, retPageParaMap.get(page), DataUtilities.readGloveFile(p));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
