package com.trema.pcpn.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public class ClusterToRunfileWriter {
	
	public void writeRunFile(Properties p) {
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(p.getProperty("out-dir")+"/"+p.getProperty("cluster-out"))));
			HashMap<String, ArrayList<ArrayList<String>>> resultPageClusters = (HashMap<String, ArrayList<ArrayList<String>>>)ois.readObject();
			ois.close();
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(p.getProperty("out-dir")+"/"+p.getProperty("cluster-out")+"-cluster-run")));
			HashMap<String, ArrayList<String>> outrunFileMap = new HashMap<String, ArrayList<String>>();
			for(String page:resultPageClusters.keySet()) {
				for(ArrayList<String> cl:resultPageClusters.get(page)) {
					for(String para1:cl) {
						for(String para2:cl) {
							if(!para1.equals(para2)) {
								if(outrunFileMap.containsKey(para1) && !outrunFileMap.get(para1).contains(para2)) {
									outrunFileMap.get(para1).add(para2);
								}
								else {
									ArrayList<String> paralist = new ArrayList<String>();
									paralist.add(para2);
									outrunFileMap.put(para1, paralist);
								}
							}
						}
					}
				}
			}
			for(String qp:outrunFileMap.keySet()) {
				ArrayList<String> seenParaIDs = new ArrayList<String>();
				for(String para:outrunFileMap.get(qp)) {
					if(!seenParaIDs.contains(para)) {
						bw.write(qp+" Q0 "+para+" 0 1 CLUSTER-RUN\n");
						seenParaIDs.add(para);
					}
				}	
			}
			bw.close();
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream(new File("project.properties")));
			ClusterToRunfileWriter crfw = new ClusterToRunfileWriter();
			crfw.writeRunFile(prop);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
