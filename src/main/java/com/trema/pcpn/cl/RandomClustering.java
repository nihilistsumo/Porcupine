package com.trema.pcpn.cl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;

public class RandomClustering {
	
	private Properties prop;
	private String pageID;
	private int secNo;
	private ArrayList<String> secids;
	private ArrayList<String> paraids;
	
	public RandomClustering(Properties p, String pID, ArrayList<String> paraIDs, ArrayList<String> sectionIDs){
		this.prop = p;
		this.pageID = pID;
		this.secNo = sectionIDs.size();
		this.secids = sectionIDs;
		this.paraids = paraIDs;
	}
	
	public ArrayList<ArrayList<String>> cluster(){
		Random r = new Random();
		ArrayList<ArrayList<String>> clusters = new ArrayList<ArrayList<String>>();
		ArrayList<String> paras = new ArrayList<String>();
		int paraInd, clInd;
		for(String p:this.paraids)
			paras.add(p);
		for(int i=0; i<this.secNo; i++) {
			ArrayList<String> cl = new ArrayList<String>();
			paraInd = r.nextInt(paras.size());
			cl.add(paras.get(paraInd));
			paras.remove(paraInd);
			clusters.add(new ArrayList<String>());
		}
		while(paras.size()>0) {
			paraInd = r.nextInt(paras.size());
			clInd = r.nextInt(this.secNo);
			clusters.get(clInd).add(paras.get(paraInd));
			paras.remove(paraInd);
		}
		return clusters;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
