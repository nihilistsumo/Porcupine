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
		//HashMap<String, ArrayList<String>> pageSecMap = DataUtilities.getArticleToplevelSecMap(p.getProperty("data-dir")+"/"+p.getProperty("outline"));
		/*
		HashMap<String, ArrayList<String>> pageParaMapRunFile = DataUtilities.getPageParaMapFromRunfile(
				p.getProperty("out-dir")+"/"+p.getProperty("trec-runfile"));
		*/
		
		IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(p.getProperty("index-dir")).toPath()))));
		HashMap<String, ArrayList<String>> parasimQrelsMap = DataUtilities.getGTMapQrels(parasimQrelsPath);
		
		SimilarityCalculator sc = new SimilarityCalculator();
		ILexicalDatabase db = new NictWordNet();
		HashMap<String, double[]> gloveVecs = DataUtilities.readGloveFile(p);
		int vecSize = gloveVecs.get("the").length;
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fetFileOut)));
		Analyzer analyzer = new StandardAnalyzer();
		//QueryParser qp = new QueryParser("paraid", analyzer);
		ArrayList<String> allParas = new ArrayList<String>(parasimQrelsMap.keySet());
		HashMap<String, double[]> paraVecMap = DataUtilities.getParaW2VVecMap(p, allParas, gloveVecs, vecSize);
		//int count = 0;
		//System.out.println(parasInPage.size()*(parasInPage.size()-1)+" paragraph pairs are to be calculated in page "+page);
		/*
		HashMap<String, String> paraIDTextMap = new HashMap<String, String>();
		for(String paraID:allParas)
			paraIDTextMap.put(paraID, is.doc(is.search(qp.parse(paraID), 1).scoreDocs[0].doc).get("parabody"));
		*/
		//System.out.println("Total paras: "+parasimQrelsMap.size());
		SubsampleForRlib sample = new SubsampleForRlib();
		HashMap<String, ArrayList<String>> paraMap = sample.subSample(p.getProperty("data-dir")+"/"+p.getProperty("top-qrels"), p.getProperty("data-dir")+"/"+p.getProperty("art-qrels"));
		System.out.println("No. of queries to be included in rlib fet file: "+paraMap.size());
		StreamSupport.stream(paraMap.keySet().spliterator(), true).forEach(keyPara -> {
			try {
				ArrayList<String> simParas = parasimQrelsMap.get(keyPara);
				QueryParser qp = new QueryParser("paraid", analyzer);
				for(String candPara:paraMap.get(keyPara)) {
					String p1Text = is.doc(is.search(qp.parse(keyPara), 1).scoreDocs[0].doc).get("parabody");
					String p2Text = is.doc(is.search(qp.parse(candPara), 1).scoreDocs[0].doc).get("parabody");
					String fetLine = "";
					if(simParas.contains(candPara))
						fetLine+="1 qid:"+keyPara;
					else
						fetLine+="0 qid:"+keyPara;
					fetLine+=" 1:"+sc.calculateWordnetSimilarity(db, p1Text, p2Text, "ji")+
							" 2:"+sc.calculateWordnetSimilarity(db, p1Text, p2Text, "pat")+
							" 3:"+sc.calculateWordnetSimilarity(db, p1Text, p2Text, "wu")+
							" 4:"+sc.calculateWordnetSimilarity(db, p1Text, p2Text, "lin")+
							" 5:"+sc.calculateW2VCosineSimilarity(paraVecMap.get(keyPara), paraVecMap.get(candPara))+
							" #"+candPara;
					//System.out.println(fetLine);
					try {
						bw.write(fetLine+"\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				System.out.println(keyPara+" done");
			} catch (IOException | ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		bw.close();
	}
	
	private String removeStopWords(String inputText) {
		String processedText = "";
		String[] inputTokens = inputText.split(" ");
		for(String token:inputTokens) {
			if(!DataUtilities.stopwords.contains(token))
				processedText+=token+" ";
		}
		return processedText.trim();
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

