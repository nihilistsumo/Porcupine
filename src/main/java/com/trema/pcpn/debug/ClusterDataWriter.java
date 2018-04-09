package com.trema.pcpn.debug;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public class ClusterDataWriter {
	
	public void writeClusterData(Properties p, ArrayList<String> orderedPageIDs) {
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(p.getProperty("out-dir")+"/"+p.getProperty("cluster-out"))));
			HashMap<String, ArrayList<ArrayList<String>>> resultPageClusters = (HashMap<String, ArrayList<ArrayList<String>>>)ois.readObject();
			ois.close();
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(p.getProperty("cluster-out")+"-info")));
			for(String page:orderedPageIDs) {
				bw.write("===================================================================================\n");
				bw.write(page+"\n");
				bw.write("===================================================================================\n");
				ArrayList<ArrayList<String>> cl = resultPageClusters.get(page);
				int clusterNo = 1;
				for(ArrayList<String> c:cl) {
					bw.write("--------------------------------------------------------------------------------------\n");
					bw.write("\nCluster "+clusterNo+":\n");
					for(String para:c) {
						bw.write("\nParagraph ID: "+para+"\n");
					}
					bw.write("--------------------------------------------------------------------------------------\n");
				}
				bw.write("\n\n\n");
			}
		} catch (IOException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
