package com.trema.pcpn.parasim;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;
import java.util.stream.StreamSupport;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

import com.trema.pcpn.util.DataUtilities;
import com.trema.prcpn.similarity.SimilarityCalculator;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;

public class ParasimFetFileWriterShort {
	
	public void writeFetFile(Properties p, String fetFileOut, String candRunFilePath, String parasimQrelsPath, boolean withTruePage) throws IOException, ParseException {
		//String[] methods = methodString.split(" ");
		HashMap<String, HashMap<String, Double>> paraPairRun = new HashMap<String, HashMap<String, Double>>();
		//HashMap<String, ArrayList<String>> pageSecMap = DataUtilities.getArticleToplevelSecMap(p.getProperty("data-dir")+"/"+p.getProperty("outline"));
		/*
		HashMap<String, ArrayList<String>> pageParaMapRunFile = DataUtilities.getPageParaMapFromRunfile(
				p.getProperty("out-dir")+"/"+p.getProperty("trec-runfile"));
		*/
		
		IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(p.getProperty("index-dir")).toPath()))));
		HashMap<String, ArrayList<String>> parasimQrelsMap = DataUtilities.getGTMapQrels(parasimQrelsPath);
		HashMap<String, ArrayList<String>> pageParaMap;
		if(withTruePage)
			pageParaMap = DataUtilities.getGTMapQrels(p.getProperty("data-dir")+"/"+p.getProperty("art-qrels"));
		else
			pageParaMap = DataUtilities.getPageParaMapFromRunfile(candRunFilePath);
		SimilarityCalculator sc = new SimilarityCalculator();
		ILexicalDatabase db = new NictWordNet();
		HashMap<String, double[]> gloveVecs = DataUtilities.readGloveFile(p);
		int vecSize = gloveVecs.get("the").length;
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fetFileOut)));
		Analyzer analyzer = new StandardAnalyzer();
		QueryParser qp = new QueryParser("paraid", analyzer);
		ArrayList<String> allParas = new ArrayList<String>(parasimQrelsMap.keySet());
		Random rnd = new Random();
		HashMap<String, double[]> paraVecMap = DataUtilities.getParaW2VVecMap(p, allParas, gloveVecs, vecSize);
		//int count = 0;
		//System.out.println(parasInPage.size()*(parasInPage.size()-1)+" paragraph pairs are to be calculated in page "+page);
		HashMap<String, String> paraIDTextMap = new HashMap<String, String>();
		for(String paraID:allParas)
			paraIDTextMap.put(paraID, is.doc(is.search(qp.parse(paraID), 1).scoreDocs[0].doc).get("parabody"));
		System.out.println("Total paras: "+parasimQrelsMap.size());
		StreamSupport.stream(parasimQrelsMap.keySet().spliterator(), true).forEach(keyPara -> {
			ArrayList<String> simParas = parasimQrelsMap.get(keyPara);
			ArrayList<String> nonSimParas = new ArrayList<String>();
			int rndIndex = 0;
			//non-sim paras are those paragraphs which do not fall inside same top-level heading with keypara; they might be from different pages than keypara
			for(int i=0; i<2*simParas.size(); i++) {
				rndIndex = rnd.nextInt(allParas.size());
				while(simParas.contains(allParas.get(rndIndex)))
					rndIndex = rnd.nextInt(allParas.size());
				nonSimParas.add(allParas.get(rndIndex));
			}
			for(String simPara:simParas) {
				if(!keyPara.equals(simPara)) {
					String p1Text = paraIDTextMap.get(keyPara);
					String p2Text = paraIDTextMap.get(simPara);
					String fetLine = "1 qid:"+keyPara;
					fetLine+=" 1:"+sc.calculateWordnetSimilarity(db, p1Text, p2Text, "ji")+
							" 2:"+sc.calculateWordnetSimilarity(db, p1Text, p2Text, "pat")+
							" 3:"+sc.calculateWordnetSimilarity(db, p1Text, p2Text, "wu")+
							" 4:"+sc.calculateWordnetSimilarity(db, p1Text, p2Text, "lin")+
							" 5:"+sc.calculateW2VCosineSimilarity(paraVecMap.get(keyPara), paraVecMap.get(simPara))+
							" #"+simPara;
					//System.out.println(fetLine);
					try {
						bw.write(fetLine+"\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			//System.out.println("Writing relevant features complete for "+keyPara);
			for(String nonSimPara:nonSimParas) {
				if(!keyPara.equals(nonSimPara)) {
					String p1Text = paraIDTextMap.get(keyPara);
					String p2Text = paraIDTextMap.get(nonSimPara);
					String fetLine = "0 qid:"+keyPara;
					fetLine+=" 1:"+sc.calculateWordnetSimilarity(db, p1Text, p2Text, "ji")+
							" 2:"+sc.calculateWordnetSimilarity(db, p1Text, p2Text, "pat")+
							" 3:"+sc.calculateWordnetSimilarity(db, p1Text, p2Text, "wu")+
							" 4:"+sc.calculateWordnetSimilarity(db, p1Text, p2Text, "lin")+
							" 5:"+sc.calculateW2VCosineSimilarity(paraVecMap.get(keyPara), paraVecMap.get(nonSimPara))+
							" #"+nonSimPara;
					//System.out.println(fetLine);
					try {
						bw.write(fetLine+"\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			System.out.println("Writing features complete for "+keyPara);
		});
		bw.close();
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			Properties p = new Properties();
			p.load(new FileInputStream(new File("project.properties")));
			ParasimFetFileWriterShort pfw = new ParasimFetFileWriterShort();
			pfw.writeFetFile(p, "/home/sumanta/Documents/Porcupine-data/Porcupine-results/rlib/parasim-rlib-fet", 
					"/home/sumanta/Documents/Mongoose-data/Mongoose-results/page-runs-basic-sim-and-fixed/lucene-basic/train/cv-comb-run", 
					"/home/sumanta/Documents/Porcupine-data/Porcupine-results/qrels/simpara-train-art-for-cluster.qrels", false);
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

