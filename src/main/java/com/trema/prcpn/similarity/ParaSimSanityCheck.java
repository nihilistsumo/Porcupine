package com.trema.prcpn.similarity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;
import java.util.stream.StreamSupport;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.store.FSDirectory;

import com.trema.pcpn.parasimutil.SubsampleForRlib;
import com.trema.pcpn.util.DataUtilities;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;

public class ParaSimSanityCheck {
	
	// methods to be tried are separated by :
	public void check(Properties prop, String methods, String wnMethods, String indexDirPath, String indexDirNoStops, String topQrelsPath, String artQrelsPath, int keyNo) throws IOException, ParseException {
		IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirPath).toPath()))));
		IndexSearcher isNoStops = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirNoStops).toPath()))));
		SubsampleForRlib sampler = new SubsampleForRlib();
		HashMap<String, ArrayList<String>> sample = sampler.subSample(topQrelsPath, artQrelsPath);
		
		
		ArrayList<String> qparaset = new ArrayList<String>(sample.keySet());
		ArrayList<String> qSoFar = new ArrayList<String>();
		Random rand = new Random();
		for(int i=0; i<keyNo; i++) {
			String keyPara = "";
			keyPara = qparaset.get(rand.nextInt(qparaset.size()));
			while(qSoFar.contains(keyPara))
				keyPara = qparaset.get(rand.nextInt(qparaset.size()));
			qSoFar.add(keyPara);
		}
		
		// Random code
		System.out.println("Random baseline started");
		HashMap<String, String> sampleRet1 = new HashMap<String, String>();
		HashMap<String, String> sampleQrels1 = new HashMap<String, String>();
		StreamSupport.stream(qSoFar.spliterator(), true).forEach(keyPara -> {
			ArrayList<String> retParas = sample.get(keyPara);
			String rel = retParas.get(0);
			sampleQrels1.put(keyPara, rel);
			Collections.shuffle(retParas);
			sampleRet1.put(keyPara, retParas.get(rand.nextInt(3)));
			//System.out.print("Para "+keyPara+" done\r");
		});
		int correctCount1 = 0;
		for(String q:sampleRet1.keySet()) {
			if(sampleRet1.get(q).equalsIgnoreCase(sampleQrels1.get(q)))
				correctCount1++;
		}
		System.out.println("Random baseline Precision@1 = "+(double)correctCount1/sampleQrels1.size());
		
		
		// Lucene sim code
		for(String method:methods.split(":")) {
			if(method.equalsIgnoreCase("bm25"))
				is.setSimilarity(new BM25Similarity());
			else if(method.equalsIgnoreCase("bool"))
				is.setSimilarity(new BooleanSimilarity());
			else if(method.equalsIgnoreCase("classic"))
				is.setSimilarity(new ClassicSimilarity());
			else if(method.equalsIgnoreCase("lmds"))
				is.setSimilarity(new LMDirichletSimilarity());
			HashMap<String, String> sampleRet = new HashMap<String, String>();
			HashMap<String, String> sampleQrels = new HashMap<String, String>();
			System.out.println(method+" started");
			StreamSupport.stream(qSoFar.spliterator(), true).forEach(keyPara -> {
				try {
					ArrayList<String> retParas = sample.get(keyPara);
					String rel = retParas.get(0);
					sampleQrels.put(keyPara, rel);
					QueryParser qpID = new QueryParser("paraid", new StandardAnalyzer());
					QueryParser qp = new QueryParser("parabody", new StandardAnalyzer());
					String queryString = isNoStops.doc(isNoStops.search(qpID.parse(keyPara), 1).scoreDocs[0].doc).get("parabody");
					BooleanQuery.setMaxClauseCount(65536);
					Query q = qp.parse(QueryParser.escape(queryString));
					TopDocs tds = is.search(q, 100);
					ScoreDoc[] retDocs = tds.scoreDocs;
					for (int j = 0; j < retDocs.length; j++) {
						Document d = is.doc(retDocs[j].doc);
						String retParaID = d.getField("paraid").stringValue();
						if(!retParaID.equals(keyPara) && retParas.contains(retParaID)) {
							sampleRet.put(keyPara, retParaID);
							break;
						}
					}
					//System.out.print("Para "+keyPara+" done\r");
				} catch (IOException | ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
			int correctCount = 0;
			for(String q:sampleRet.keySet()) {
				if(sampleRet.get(q).equalsIgnoreCase(sampleQrels.get(q)))
					correctCount++;
			}
			System.out.println(method+" Precision@1 = "+(double)correctCount/sampleQrels.size());
		}
		
		// Wordnet code
		for(String wnMethod:wnMethods.split(":")) {
			HashMap<String, String> sampleQrels = new HashMap<String, String>();
			HashMap<String, String> sampleRet = new HashMap<String, String>();
			System.out.println(wnMethod+" started");
			ILexicalDatabase db = new NictWordNet();
			SimilarityCalculator sc = new SimilarityCalculator();
			StreamSupport.stream(qSoFar.spliterator(), true).forEach(keyPara -> {
				try {
					ArrayList<String> retParas = sample.get(keyPara);
					String rel = retParas.get(0);
					sampleQrels.put(keyPara, rel);
					QueryParser qpID = new QueryParser("paraid", new StandardAnalyzer());
					QueryParser qp = new QueryParser("parabody", new StandardAnalyzer());
					String topRet = "";
					double topScore = 0;
					Collections.shuffle(retParas);
					for(String ret:retParas) {
						String p1text = isNoStops.doc(isNoStops.search(qpID.parse(keyPara), 1).scoreDocs[0].doc).get("parabody");
						String p2text = isNoStops.doc(isNoStops.search(qpID.parse(ret), 1).scoreDocs[0].doc).get("parabody");
						double currScore = sc.calculateWordnetSimilarity(db, p1text, p2text, wnMethod);
						if(currScore>topScore) {
							topRet = ret;
							topScore = currScore;
						}
					}
					sampleRet.put(keyPara, topRet);
				} catch (IOException | ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//System.out.print("Para "+keyPara+" done\r");
			});
			int correctCount = 0;
			for(String q:sampleRet.keySet()) {
				if(sampleRet.get(q).equalsIgnoreCase(sampleQrels.get(q)))
					correctCount++;
			}
			System.out.println(wnMethod+" Precision@1 = "+(double)correctCount/sampleQrels.size());
		}
		
		// W2V code
		HashMap<String, String> sampleQrels = new HashMap<String, String>();
		HashMap<String, String> sampleRet = new HashMap<String, String>();
		System.out.println("w2v started");
		HashMap<String, double[]> gloveVecs = DataUtilities.readGloveFile(prop);
		int vecSize = gloveVecs.get("the").length;
		StreamSupport.stream(qSoFar.spliterator(), true).forEach(keyPara -> {
			try {
				ArrayList<String> retParas = sample.get(keyPara);
				String rel = retParas.get(0);
				sampleQrels.put(keyPara, rel);
				String topRet = "";
				double topScore = 0;
				Collections.shuffle(retParas);
				for(String ret:retParas) {
					double currScore = DataUtilities.getDotProduct(DataUtilities.getParaW2VVec(prop, keyPara, gloveVecs, vecSize), DataUtilities.getParaW2VVec(prop, ret, gloveVecs, vecSize));
					if(currScore>topScore) {
						topRet = ret;
						topScore = currScore;
					}
				}
				sampleRet.put(keyPara, topRet);
			} catch (IOException | ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//System.out.print("Para "+keyPara+" done\r");
		});
		int correctCount = 0;
		for(String q:sampleRet.keySet()) {
			if(sampleRet.get(q).equalsIgnoreCase(sampleQrels.get(q)))
				correctCount++;
		}
		System.out.println("w2v Precision@1 = "+(double)correctCount/sampleQrels.size());
	}

}
