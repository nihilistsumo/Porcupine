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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

import com.trema.pcpn.cl.ClusteringMetrics;
import com.trema.pcpn.util.DataUtilities;

public class ClusterDataWriter {
	
	public void writeClusterData(Properties p, ArrayList<String> orderedPageIDs) {
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(p.getProperty("out-dir")+"/"+p.getProperty("cluster-out"))));
			HashMap<String, ArrayList<ArrayList<String>>> resultPageClusters = (HashMap<String, ArrayList<ArrayList<String>>>)ois.readObject();
			ois.close();
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(p.getProperty("out-dir")+"/"+p.getProperty("cluster-out")+"-info")));
			IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(p.getProperty("index-dir")).toPath()))));
			Analyzer analyzer = new StandardAnalyzer();
			QueryParser qp = new QueryParser("paraid", analyzer);
			Document paraDoc;
			for(String page:orderedPageIDs) {
				ArrayList<ArrayList<String>> gtClusters = DataUtilities.getGTClusters(page, p.getProperty("data-dir")+"/"+p.getProperty("top-qrels"));
				bw.write("===================================================================================\n");
				bw.write(page+"\n");
				bw.write("===================================================================================\n");
				ArrayList<ArrayList<String>> cl = resultPageClusters.get(page);
				ClusteringMetrics cm = new ClusteringMetrics(gtClusters, cl, true);
				bw.write("Adjusted RAND index = "+cm.getAdjRAND()+"\n");
				bw.write("fMeasure = "+cm.fMeasure()+"\n");
				ArrayList<ArrayList<String>> tp = cm.getTPpairs();
				bw.write("True para pairs retrieved\n");
				bw.write("+++++++++++++++++++++++++++\n");
				for(ArrayList<String> pair:tp) {
					bw.write(pair.get(0)+" "+pair.get(1)+"\n");
				}
				bw.write("+++++++++++++++++++++++++++\n\n");
				int clusterNo = 1;
				for(ArrayList<String> c:cl) {
					bw.write("--------------------------------------------------------------------------------------\n");
					bw.write("\nCluster "+clusterNo+":\n");
					for(String para:c) {
						bw.write("\nParagraph ID: "+para+"\n");
						paraDoc = is.doc(is.search(qp.parse(para), 1).scoreDocs[0].doc);
						bw.write(paraDoc.get("parabody")+"\n");
					}
					clusterNo++;
					bw.write("--------------------------------------------------------------------------------------\n");
				}
				bw.write("\n\n\n");
			}
			bw.close();
		} catch (IOException | ClassNotFoundException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream(new File("project.properties")));
			ClusterDataWriter cdw = new ClusterDataWriter();
			ArrayList<String> pageList = new ArrayList<String>();
			pageList.add("enwiki:Freshwater%20environmental%20quality%20parameters");
			pageList.add("enwiki:Human%20rights");
			pageList.add("enwiki:Water%20resource%20management");
			pageList.add("enwiki:Atmospheric%20sciences");
			cdw.writeClusterData(prop, pageList);
			//cdw.writeClusterData(prop, orderedPageIDs);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
