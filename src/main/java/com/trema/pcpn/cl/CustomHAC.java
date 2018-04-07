package com.trema.pcpn.cl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;


public class CustomHAC {
	
	public Properties prop;
	public String pageID;
	public int secNo;
	HashMap<String, double[]> paraVecMap;
	ArrayList<String> secids;
	ArrayList<String> paraids;
	
	public CustomHAC(Properties p, String pID, ArrayList<String> paraIDs, ArrayList<String> sectionIDs, HashMap<String, double[]> vecs){
		this.prop = p;
		this.pageID = pID;
		this.secNo = sectionIDs.size();
		this.paraVecMap = vecs;
		this.secids = sectionIDs;
		this.paraids = paraIDs;
	}
	
	public ArrayList<ArrayList<String>> cluster(){
		
		// Initialization //
		HashMap<String, ArrayList<String>> clusters = new HashMap<String, ArrayList<String>>();
		HashMap<HashSet<String>, Double> clusterPairData = new HashMap<HashSet<String>, Double>();
		int noClusters = 0;
		Integer clusterID = 1;
		for(String p:this.paraids){
			ArrayList<String> paraList = new ArrayList<String>();
			paraList.add(p);
			clusters.put("c"+clusterID, paraList);
			clusterID++;
		}
		noClusters = clusters.size();
		for(int i=0; i<this.paraids.size()-1; i++){
			for(int j=i+1; j<this.paraids.size(); j++){
				String p1 = this.paraids.get(i);
				String p2 = this.paraids.get(j);
				String c1 = "", c2 = "";
				for(String c:clusters.keySet()){
					if(p1.equals(clusters.get(c).get(0))){
						c1 = c;
						break;
					}
				}
				for(String c:clusters.keySet()){
					if(p2.equals(clusters.get(c).get(0))){
						c2 = c;
						break;
					}
				}
				HashSet<String> clusterPairKey = new HashSet<String>();
				clusterPairKey.add(c1);
				clusterPairKey.add(c2);
				clusterPairData.put(clusterPairKey, this.getDotProduct(this.paraVecMap.get(p1), this.paraVecMap.get(p2)));
			}
		}
		// ------------ //
		
		while(noClusters>this.secNo){
			HashSet<String> clusterPairMax = null;
			double maxScore = -Double.MAX_VALUE;
			for(HashSet<String> cp:clusterPairData.keySet()){
				double score = clusterPairData.get(cp);
				if(score>maxScore){
					clusterPairMax = cp;
					maxScore = score;
				}
			}
			Iterator<String> it = clusterPairMax.iterator();
			String mergeC1 = it.next();
			String mergeC2 = it.next();
			String mergedC = mergeC1+mergeC2;
			
			// merge two clusters mergeC1 and mergeC2
			for(String cid:clusters.keySet()){
				if(cid.equals(mergeC1) || cid.equals(mergeC2))
					continue;
				HashSet<String> cxz = new HashSet<String>();
				HashSet<String> cyz = new HashSet<String>();
				HashSet<String> cxyz = new HashSet<String>();
				cxz.add(mergeC1);cxz.add(cid);
				cyz.add(mergeC2);cyz.add(cid);
				cxyz.add(mergedC); cxyz.add(cid);
				clusterPairData.put(cxyz, maxScore);
				clusterPairData.remove(cxz);
				clusterPairData.remove(cyz);
			}
			HashSet<String> cxy = new HashSet<String>();
			cxy.add(mergeC1);cxy.add(mergeC2);
			clusterPairData.remove(cxy);
			
			ArrayList<String> mergedParas = clusters.get(mergeC1);
			mergedParas.addAll(clusters.get(mergeC2));
			clusters.put(mergedC, mergedParas);
			clusters.remove(mergeC1);
			clusters.remove(mergeC2);
			
			noClusters--;
		}
		ArrayList<ArrayList<String>> listOfClusters = new ArrayList<ArrayList<String>>();
		for(String c:clusters.keySet())
			listOfClusters.add(clusters.get(c));
		/*
		ParaMapper pm = new ParaMapper(this.prop, listOfClusters, this.secids);
		pm.map();
		*/
		return listOfClusters;
	}
	
	private double getDotProduct(double[] a, double[] b){
		double val = 0;
		for(int i=0; i<a.length; i++)
			val+=a[i]*b[i];
		val = val/a.length;
		return val;
	}

}
