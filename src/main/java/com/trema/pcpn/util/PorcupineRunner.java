package com.trema.pcpn.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.lucene.queryparser.classic.ParseException;

public class PorcupineRunner {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			Properties prop = new Properties();
			prop.load(new FileInputStream(new File("project.properties")));
			PorcupineHelper ph = new PorcupineHelper();
			String cmd = prop.getProperty("mode");
			if(cmd.equalsIgnoreCase("twvhac")){
				ph.runHACClustering(prop, true, "w2v");
			}
			else if(cmd.equalsIgnoreCase("ttmhac")){
				ph.runHACClustering(prop, true, "tm");
			}
			else if(cmd.equalsIgnoreCase("ttfhac")){
				ph.runHACClustering(prop, true, "tfidf");
			}
			else if(cmd.equalsIgnoreCase("twvkm")){
				ph.runKMeansClustering(prop, true, "w2v");
			}
			else if(cmd.equalsIgnoreCase("ttmkm")){
				ph.runKMeansClustering(prop, true, "tm");
			}
			else if(cmd.equalsIgnoreCase("ttfkm")){
				ph.runKMeansClustering(prop, true, "tfidf");
			}
			else if(cmd.equalsIgnoreCase("cltxt")){
				ph.convertClusterDataToText(prop);
			}
			else if(cmd.equalsIgnoreCase("mc")){
				ph.runClusteringMeasure(prop);
			}
		} catch (IOException | ClassNotFoundException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
