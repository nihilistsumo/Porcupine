package com.trema.prcpn.similarity;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
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

import com.trema.pcpn.aspect.AspectSimilarity;
import com.trema.pcpn.parasimutil.SubsampleForRlib;
import com.trema.pcpn.util.DataUtilities;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;

public class ParaSimSanityCheck {
	
	public String retrieveParaRandom(ArrayList<String> retParas) {
		Random rand = new Random();
		return retParas.get(rand.nextInt(retParas.size()));
	}
	
	public String retrieveParaLucene(String keyPara, ArrayList<String> retParas, IndexSearcher is, IndexSearcher isNoStops) throws IOException, ParseException {
		Random rand = new Random();
		QueryParser qpID = new QueryParser("paraid", new StandardAnalyzer());
		QueryParser qp = new QueryParser("parabody", new StandardAnalyzer());
		String topRet = "";
		String queryString = isNoStops.doc(isNoStops.search(qpID.parse(keyPara), 1).scoreDocs[0].doc).get("parabody");
		BooleanQuery.setMaxClauseCount(65536);
		Query q = qp.parse(QueryParser.escape(queryString));
		double topScore = 0;
		for(String ret:retParas) {
			int retDocID = isNoStops.search(qpID.parse(ret), 1).scoreDocs[0].doc;
			double currScore = is.explain(q, retDocID).getValue();
			if(currScore>topScore) {
				topRet = ret;
				topScore = currScore;
			}
		}
		if(topRet.equals("")) {
			System.out.print(".");
			topRet = retParas.get(rand.nextInt(retParas.size()));
		}
		return topRet;
	}
	
	public String retrieveParaWordnet(String keyPara, ArrayList<String> retParas, String wnMethod, ILexicalDatabase db, IndexSearcher isNoStops, SimilarityCalculator sc) throws IOException, ParseException {
		Random rand = new Random();
		QueryParser qpID = new QueryParser("paraid", new StandardAnalyzer());
		String topRet = "";
		double topScore = 0;
		for(String ret:retParas) {
			String p1text = isNoStops.doc(isNoStops.search(qpID.parse(keyPara), 1).scoreDocs[0].doc).get("parabody");
			String p2text = isNoStops.doc(isNoStops.search(qpID.parse(ret), 1).scoreDocs[0].doc).get("parabody");
			double currScore = sc.calculateWordnetSimilarity(db, p1text, p2text, wnMethod);
			if(currScore>topScore) {
				topRet = ret;
				topScore = currScore;
			}
		}
		if(topRet.equals("")) {
			System.out.print(".");
			topRet = retParas.get(rand.nextInt(retParas.size()));
		}
		return topRet;
	}
	
	public String retrieveParaW2V(String keyPara, ArrayList<String> retParas, HashMap<String, double[]> gloveVecs, int vecSize, Properties prop) throws IOException, ParseException {
		Random rand = new Random();
		String topRet = "";
		double topScore = 0;
		for(String ret:retParas) {
			double currScore = DataUtilities.getDotProduct(DataUtilities.getParaW2VVec(prop, keyPara, gloveVecs, vecSize), DataUtilities.getParaW2VVec(prop, ret, gloveVecs, vecSize));
			if(currScore>topScore) {
				topRet = ret;
				topScore = currScore;
			}
		}
		if(topRet.equals("")) {
			System.out.print(".");
			topRet = retParas.get(rand.nextInt(retParas.size()));
		}
		return topRet;
	}
	
	public String retrieveParaEntity(String keyPara, ArrayList<String> retParas, Connection con, HashMap<String, String> qrels, String printEntities) {
		Random rand = new Random();
		AspectSimilarity aspSim = new AspectSimilarity();
		String topRet = "";
		int topScore = 0;
		for(String ret:retParas) {
			int currScore = aspSim.findCommonEntities(keyPara, ret, con, printEntities).size();
			if(currScore>topScore) {
				topRet = ret;
				topScore = currScore;
			}
		}
		if(topRet.equals("")) {
			System.out.print(".");
			topRet = retParas.get(rand.nextInt(retParas.size()));
		}
		return topRet;
	}
	
	public String retrieveParaAspect(String keyPara, ArrayList<String> retParas, IndexSearcher isNoStops, IndexSearcher aspectIs, HashMap<String, String> qrels, int retAspNo, String printAspects) throws IOException, ParseException {
		Random rand = new Random();
		QueryParser qpID = new QueryParser("paraid", new StandardAnalyzer());
		QueryParser qpAspText = new QueryParser("Text", new StandardAnalyzer());
		String topRet = "";
		double topScore = 0;
		String queryString = isNoStops.doc(isNoStops.search(qpID.parse(keyPara), 1).scoreDocs[0].doc).get("parabody");
		if(printAspects.equalsIgnoreCase("print")) {
			System.out.println("Keypara: "+keyPara);
			System.out.println("Query String: "+queryString+"\n");
		}
		BooleanQuery.setMaxClauseCount(65536);
		Query q = qpAspText.parse(QueryParser.escape(queryString));
		//TopDocs tdsKeypara = aspectIs.search(q, 100);
		ScoreDoc[] retAspectsKeyPara = aspectIs.search(q, retAspNo).scoreDocs;
		if(printAspects.equalsIgnoreCase("print")) {
			System.out.println("Aspects of key "+keyPara);
			System.out.println("--------------\n");
			this.printAspects(keyPara, retAspectsKeyPara, aspectIs, false);
		}
		for(String ret:retParas) {
			//int retDocID = aspectIs.search(qpID.parse(ret), 1).scoreDocs[0].doc;
			//double currScore = aspectIs.explain(q, retDocID).getValue();
			queryString = isNoStops.doc(isNoStops.search(qpID.parse(ret), 1).scoreDocs[0].doc).get("parabody");
			BooleanQuery.setMaxClauseCount(65536);
			q = qpAspText.parse(QueryParser.escape(queryString));
			ScoreDoc[] retAspectsRetPara = aspectIs.search(q, retAspNo).scoreDocs;
			
			double currScore = this.calculateAspectSimilarity(retAspectsKeyPara, retAspectsRetPara);
			if(printAspects.equalsIgnoreCase("print")) {
				System.out.println("Para ID: "+ret);
				System.out.println("Aspect similrity score with keypara = "+currScore);
				this.printAspects(ret, retAspectsRetPara, aspectIs, ret.equalsIgnoreCase(qrels.get(keyPara)));
				System.out.println("\n\n");
			}
			if(currScore>topScore) {
				topRet = ret;
				topScore = currScore;
			}
		}
		if(topRet.equals("")) {
			System.out.print(".");
			topRet = retParas.get(rand.nextInt(retParas.size()));
		}
		return topRet;
	}
	
	private double calculateAspectSimilarity(ScoreDoc[] keyAspects, ScoreDoc[] retAspects) {
		int match = 0;
		for(ScoreDoc d:keyAspects) {
			for(ScoreDoc retD:retAspects) {
				if(d.doc==retD.doc)
					match++;
			}
		}
		return (double)match/keyAspects.length;
	}
	
	private void printAspects(String paraID, ScoreDoc[] aspects, IndexSearcher aspectIs, boolean isRel) {
		int rank = 1;
		System.out.println("Retrieved aspects");
		if(isRel)
			System.out.println("This is relevant");
		System.out.println("-----------------\n");
		for(ScoreDoc asp:aspects) {
			try {
				Document aspDoc = aspectIs.doc(asp.doc);
				String heading = aspDoc.getField("Headings").stringValue();
				String id = aspDoc.getField("Id").stringValue();
				String leadText = aspDoc.getField("LeadText").stringValue();
				String text = aspDoc.getField("Text").stringValue();
				String title = aspDoc.getField("Title").stringValue();
				
				System.out.println(rank+". ID: "+id+"\nHeading: "+heading+"\nTitle: "+title+"\nLead Text: "+leadText+"\n\nText: "+text+"\n");
				rank++;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	// methods to be tried are separated by :
	public void check(Properties prop, String methods, String indexDirAspPath, String indexDirPath, String indexDirNoStops, String topQrelsPath, String artQrelsPath, int keyNo, int retAspNo, String print) throws IOException, ParseException, ClassNotFoundException, SQLException {
		IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirPath).toPath()))));
		IndexSearcher isNoStops = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirNoStops).toPath()))));
		IndexSearcher aspectIs = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirAspPath).toPath()))));
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
		ILexicalDatabase db = new NictWordNet();
		Connection con = DataUtilities.getDBConnection(prop.getProperty("dbip"), prop.getProperty("db"), "paraent", prop.getProperty("dbuser"), prop.getProperty("dbpwd"));
		// loop code
		for(String method:methods.split(":")) {
			HashMap<String, String> sampleRet1 = new HashMap<String, String>();
			HashMap<String, String> sampleQrels1 = new HashMap<String, String>();
			System.out.println(method+" started");
			StreamSupport.stream(qSoFar.spliterator(), false).forEach(keyPara -> {
			//for(String keyPara:qSoFar) {
				ArrayList<String> retParas = sample.get(keyPara);
				String rel = retParas.get(0);
				sampleQrels1.put(keyPara, rel);
				Collections.shuffle(retParas);
				try {
					if(method.equals("rand"))
						sampleRet1.put(keyPara, this.retrieveParaRandom(retParas));
					else if(method.equals("bm25")) {
						is.setSimilarity(new BM25Similarity());
						sampleRet1.put(keyPara, this.retrieveParaLucene(keyPara, retParas, is, isNoStops));
					}
					else if(method.equals("bool")) {
						is.setSimilarity(new BooleanSimilarity());
						sampleRet1.put(keyPara, this.retrieveParaLucene(keyPara, retParas, is, isNoStops));
					}
					else if(method.equals("classic")) {
						is.setSimilarity(new ClassicSimilarity());
						sampleRet1.put(keyPara, this.retrieveParaLucene(keyPara, retParas, is, isNoStops));
					}
					else if(method.equals("lmds")) {
						is.setSimilarity(new LMDirichletSimilarity());
						sampleRet1.put(keyPara, this.retrieveParaLucene(keyPara, retParas, is, isNoStops));
					}
					else if(method.equals("wnji")) {
						SimilarityCalculator sc = new SimilarityCalculator();
						sampleRet1.put(keyPara, this.retrieveParaWordnet(keyPara, retParas, "ji", db, isNoStops, sc));
					}
					else if(method.equals("wnpat")) {
						SimilarityCalculator sc = new SimilarityCalculator();
						sampleRet1.put(keyPara, this.retrieveParaWordnet(keyPara, retParas, "pat", db, isNoStops, sc));
					}
					else if(method.equals("wnwu")) {
						SimilarityCalculator sc = new SimilarityCalculator();
						sampleRet1.put(keyPara, this.retrieveParaWordnet(keyPara, retParas, "wu", db, isNoStops, sc));
					}
					else if(method.equals("wnlin")) {
						SimilarityCalculator sc = new SimilarityCalculator();
						sampleRet1.put(keyPara, this.retrieveParaWordnet(keyPara, retParas, "lin", db, isNoStops, sc));
					}
					else if(method.equals("w2v")) {
						HashMap<String, double[]> gloveVecs = DataUtilities.readGloveFile(prop);
						sampleRet1.put(keyPara, this.retrieveParaW2V(keyPara, retParas, gloveVecs, gloveVecs.get("the").length, prop));
					}
					else if(method.equals("asp")) {
						sampleRet1.put(keyPara, this.retrieveParaAspect(keyPara, retParas, isNoStops, aspectIs, sampleQrels1, retAspNo, print));
					}
					else if(method.equals("ent")) {
						sampleRet1.put(keyPara, this.retrieveParaEntity(keyPara, retParas, con, sampleQrels1, print));
					}
				} catch (IOException | ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//else if(method.equals("bm25"))
				//System.out.print("Para "+keyPara+" done\r");
			});
			int correctCount1 = 0;
			for(String q:sampleRet1.keySet()) {
				//System.out.println("key = "+q+", ret = "+sampleRet1.get(q)+", rel = "+sampleQrels1.get(q));
				if(sampleRet1.get(q).equalsIgnoreCase(sampleQrels1.get(q)))
					correctCount1++;
			}
			System.out.println("\n"+method+" Precision@1 = "+(double)correctCount1/sampleQrels1.size());
		}
		
		
		/*
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
			//System.out.println("key = "+q+", ret = "+sampleRet1.get(q)+", rel = "+sampleQrels1.get(q));
			if(sampleRet1.get(q).equalsIgnoreCase(sampleQrels1.get(q)))
				correctCount1++;
		}
		System.out.println("Random baseline Precision@1 = "+(double)correctCount1/sampleQrels1.size());
		
		// Aspect code
		System.out.println("Aspect method started");
		HashMap<String, String> sampleRet = new HashMap<String, String>();
		HashMap<String, String> sampleQrels = new HashMap<String, String>();
		StreamSupport.stream(qSoFar.spliterator(), true).forEach(keyPara -> {
			try {
				ArrayList<String> retParas = sample.get(keyPara);
				String rel = retParas.get(0);
				Collections.shuffle(retParas);
				sampleQrels.put(keyPara, rel);
				
			} catch (IOException | ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		
		
		
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
					Collections.shuffle(retParas);
					sampleQrels.put(keyPara, rel);
					QueryParser qpID = new QueryParser("paraid", new StandardAnalyzer());
					QueryParser qp = new QueryParser("parabody", new StandardAnalyzer());
					String queryString = isNoStops.doc(isNoStops.search(qpID.parse(keyPara), 1).scoreDocs[0].doc).get("parabody");
					BooleanQuery.setMaxClauseCount(65536);
					Query q = qp.parse(QueryParser.escape(queryString));
					
					TopDocs tds = is.search(q, 10000);
					ScoreDoc[] retDocs = tds.scoreDocs;
					for (int j = 0; j < retDocs.length; j++) {
						Document d = is.doc(retDocs[j].doc);
						String retParaID = d.getField("paraid").stringValue();
						if(!retParaID.equals(keyPara) && retParas.contains(retParaID)) {
							sampleRet.put(keyPara, retParaID);
							break;
						}
					}
					
					String topRet = "";
					double topScore = 0;
					for(String ret:retParas) {
						int retDocID = isNoStops.search(qpID.parse(ret), 1).scoreDocs[0].doc;
						double currScore = is.explain(q, retDocID).getValue();
						if(currScore>topScore) {
							topRet = ret;
							topScore = currScore;
						}
					}
					if(topRet.equals("")) {
						//System.out.println("rand");
						topRet = retParas.get(rand.nextInt(retParas.size()));
					}
					sampleRet.put(keyPara, topRet);
					//System.out.print("Para "+keyPara+" done\r");
				} catch (IOException | ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
			int correctCount = 0;
			for(String q:sampleRet.keySet()) {
				//System.out.println("key = "+q+", ret = "+sampleRet.get(q)+", rel = "+sampleQrels.get(q));
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
					if(topRet.equals(""))
						topRet = retParas.get(rand.nextInt(retParas.size()));
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
		*/
	}

}
