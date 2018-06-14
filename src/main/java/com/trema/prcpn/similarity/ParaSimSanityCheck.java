package com.trema.prcpn.similarity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

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

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;

public class ParaSimSanityCheck {
	
	public void checkWordnet(String method, String indexDirPath, String indexDirNoStops, String topQrelsPath, String artQrelsPath, String parasimQrelsPath, int keyNo) throws IOException, ParseException {
		//IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirPath).toPath()))));
		IndexSearcher isNoStops = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirNoStops).toPath()))));
		QueryParser qpID = new QueryParser("paraid", new StandardAnalyzer());
		QueryParser qp = new QueryParser("parabody", new StandardAnalyzer());
		SimilarityCalculator sc = new SimilarityCalculator();
		SubsampleForRlib sampler = new SubsampleForRlib();
		HashMap<String, ArrayList<String>> sample = sampler.subSample(topQrelsPath, artQrelsPath);
		HashMap<String, String> sampleQrels = new HashMap<String, String>();
		HashMap<String, String> sampleRet = new HashMap<String, String>();
		ArrayList<String> qparaset = new ArrayList<String>(sample.keySet());
		ArrayList<String> qSoFar = new ArrayList<String>();
		ILexicalDatabase db = new NictWordNet();
		Random rand = new Random();
		for(int i=0; i<keyNo; i++) {
			String keyPara = "";
			keyPara = qparaset.get(rand.nextInt(qparaset.size()));
			while(qSoFar.contains(keyPara))
				keyPara = qparaset.get(rand.nextInt(qparaset.size()));
			qSoFar.add(keyPara);
			ArrayList<String> retParas = sample.get(keyPara);
			String rel = retParas.get(0);
			sampleQrels.put(keyPara, rel);
			String topRet = "";
			double topScore = 0;
			for(String ret:retParas) {
				String p1text = isNoStops.doc(isNoStops.search(qpID.parse(keyPara), 1).scoreDocs[0].doc).get("parabody");
				String p2text = isNoStops.doc(isNoStops.search(qpID.parse(ret), 1).scoreDocs[0].doc).get("parabody");
				double currScore = sc.calculateWordnetSimilarity(db, p1text, p2text, method);
				if(currScore>topScore) {
					topRet = ret;
					topScore = currScore;
				}
			}
			sampleRet.put(keyPara, topRet);
		}
		int correctCount = 0;
		for(String q:sampleRet.keySet()) {
			if(sampleRet.get(q).equalsIgnoreCase(sampleQrels.get(q)))
				correctCount++;
		}
		System.out.println("Precision@1 = "+(double)correctCount/sampleQrels.size());
	}
	
	// For lucene ret models
	public void check(String method, String indexDirPath, String indexDirNoStops, String topQrelsPath, String artQrelsPath, String parasimQrelsPath, int keyNo) throws IOException, ParseException {
		IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirPath).toPath()))));
		IndexSearcher isNoStops = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirNoStops).toPath()))));
		if(method.equalsIgnoreCase("bm25"))
			is.setSimilarity(new BM25Similarity());
		else if(method.equalsIgnoreCase("bool"))
			is.setSimilarity(new BooleanSimilarity());
		else if(method.equalsIgnoreCase("classic"))
			is.setSimilarity(new ClassicSimilarity());
		else if(method.equalsIgnoreCase("lmds"))
			is.setSimilarity(new LMDirichletSimilarity());
		SubsampleForRlib sampler = new SubsampleForRlib();
		HashMap<String, ArrayList<String>> sample = sampler.subSample(topQrelsPath, artQrelsPath);
		HashMap<String, String> sampleQrels = new HashMap<String, String>();
		HashMap<String, String> sampleRet = new HashMap<String, String>();
		ArrayList<String> qparaset = new ArrayList<String>(sample.keySet());
		ArrayList<String> qSoFar = new ArrayList<String>();
		Random rand = new Random();
		for(int i=0; i<keyNo; i++) {
			String keyPara = "";
			keyPara = qparaset.get(rand.nextInt(qparaset.size()));
			while(qSoFar.contains(keyPara))
				keyPara = qparaset.get(rand.nextInt(qparaset.size()));
			qSoFar.add(keyPara);
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
		}
		int correctCount = 0;
		for(String q:sampleRet.keySet()) {
			if(sampleRet.get(q).equalsIgnoreCase(sampleQrels.get(q)))
				correctCount++;
		}
		System.out.println("Precision@1 = "+(double)correctCount/sampleQrels.size());
	}

}
