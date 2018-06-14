package com.trema.pcpn.parasimutil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
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

public class ParasimFetFileWriter {
	
	public void writeFetFile(Properties p, String fetFileOut, String candRunFilePath, String parasimQrelsPath, boolean withTruePage) throws IOException {
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
		//StreamSupport.stream(pageParaMap.keySet().spliterator(), true).forEach(page -> {
		for(String page:pageParaMap.keySet()) {
			try {
				Analyzer analyzer = new StandardAnalyzer();
				QueryParser qp = new QueryParser("paraid", analyzer);
				ArrayList<String> parasInPage = pageParaMap.get(page);
				HashMap<String, double[]> paraVecMap = DataUtilities.getParaW2VVecMap(p, parasInPage, gloveVecs, vecSize);
				//int count = 0;
				System.out.println(parasInPage.size()*(parasInPage.size()-1)+" paragraph pairs are to be calculated in page "+page);
				HashMap<String, String> paraIDTextMap = new HashMap<String, String>();
				for(String paraID:parasInPage)
					paraIDTextMap.put(paraID, is.doc(is.search(qp.parse(paraID), 1).scoreDocs[0].doc).get("parabody"));
				StreamSupport.stream(parasInPage.spliterator(), true).forEach(p1 -> {
				//for(String p1:parasInPage) {
					//for(String p2:parasInPage) {
					StreamSupport.stream(parasInPage.spliterator(), true).forEach(p2 -> {
						try {
							if(!p1.equals(p2)) {
								String p1Text = paraIDTextMap.get(p1);
								String p2Text = paraIDTextMap.get(p2);
								String fetLine = "";
								ArrayList<String> relParas = parasimQrelsMap.get(p1);
								if(relParas!=null && relParas.contains(p2))
									fetLine = "1 qid:"+p1;
								else
									fetLine = "0 qid:"+p1;
								fetLine+=" 1:"+sc.calculateWordnetSimilarity(db, p1Text, p2Text, "ji")+
										" 2:"+sc.calculateWordnetSimilarity(db, p1Text, p2Text, "pat")+
										" 3:"+sc.calculateWordnetSimilarity(db, p1Text, p2Text, "wu")+
										" 4:"+sc.calculateWordnetSimilarity(db, p1Text, p2Text, "lin")+
										" 5:"+sc.calculateW2VCosineSimilarity(paraVecMap.get(p1), paraVecMap.get(p2))+
										" #"+p2;
								//System.out.println(fetLine);
								bw.write(fetLine+"\n");
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						/*
						count++;
						if(count%1000==0)
							System.out.println("1000 paragraph pairs done in page "+page);
						*/
					});
				});
				System.out.println("Page: "+page+" done\n");
			} catch (IOException | ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		bw.close();
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			Properties p = new Properties();
			p.load(new FileInputStream(new File("project.properties")));
			ParasimFetFileWriter pfw = new ParasimFetFileWriter();
			pfw.writeFetFile(p, "/home/sumanta/Documents/Porcupine-data/Porcupine-results/rlib/parasim-rlib-fet", 
					"/home/sumanta/Documents/Mongoose-data/Mongoose-results/page-runs-basic-sim-and-fixed/lucene-basic/train/cv-comb-run", 
					"/home/sumanta/Documents/Porcupine-data/Porcupine-results/qrels/simpara-train-art-for-cluster.qrels", false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
