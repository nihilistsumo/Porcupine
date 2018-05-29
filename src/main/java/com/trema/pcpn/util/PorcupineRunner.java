package com.trema.pcpn.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.lucene.queryparser.classic.ParseException;

import com.trema.pcpn.parasim.ParasimFetFileWriter;
import com.trema.pcpn.parasim.ParasimFetFileWriterShort;
import com.trema.prcpn.similarity.ParaSimRanker;

public class PorcupineRunner {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			Properties prop = new Properties();
			prop.load(new FileInputStream(new File("project.properties")));
			PorcupineHelper ph = new PorcupineHelper();
			String cmd = prop.getProperty("mode");
			boolean truePage = false;
			/*
			if(cmd.charAt(0)=='t')
				truePage = true;
			if(cmd.endsWith("wvhac")){
				ph.runHACClustering(prop, truePage, "w2v");
			}
			else if(cmd.endsWith("tmhac")){
				ph.runHACClustering(prop, truePage, "tm");
			}
			else if(cmd.endsWith("tfhac")){
				ph.runHACClustering(prop, truePage, "tfidf");
			}
			else if(cmd.endsWith("wvkm")){
				ph.runKMeansClustering(prop, truePage, "w2v");
			}
			else if(cmd.endsWith("tmkm")){
				ph.runKMeansClustering(prop, truePage, "tm");
			}
			else if(cmd.endsWith("tfkm")){
				ph.runKMeansClustering(prop, truePage, "tfidf");
			}
			else if(cmd.endsWith("rand")){
				ph.runRandomClustering(prop, truePage);
			}
			else if(cmd.equalsIgnoreCase("cltxt")){
				ph.convertClusterDataToText(prop);
			}
			else if(cmd.equalsIgnoreCase("mc")){
				ph.runClusteringMeasure(prop);
			}
			else if(cmd.equalsIgnoreCase("sim")){
				ph.runSimilarityRanker(prop, truePage, "wnwu");
			}
			*/
			if(args[0].equalsIgnoreCase("fet")) {
				ParasimFetFileWriterShort pfw = new ParasimFetFileWriterShort();
				String fetFileOut = args[1];
				String candRunFilePath = args[2];
				String parasimQrelsPath = args[3];
				boolean withTruePage;
				if(args[4].equalsIgnoreCase("true"))
					withTruePage = true;
				else
					withTruePage = false;
				pfw.writeFetFile(prop, fetFileOut, candRunFilePath, parasimQrelsPath, withTruePage);
			}
			else if(args[0].equalsIgnoreCase("r")) {
				ParaSimRanker psr = new ParaSimRanker();
				String method = args[1];
				String runFileOut = args[2];
				String candRunFilePath = args[3];
				boolean withTruePage = false;
				if(args[4].equalsIgnoreCase("true"))
					withTruePage = true;
				psr.rank(prop, method, runFileOut, candRunFilePath, withTruePage);
			}
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
