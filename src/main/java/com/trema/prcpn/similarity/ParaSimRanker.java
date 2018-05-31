package com.trema.prcpn.similarity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;

public class ParaSimRanker {
	
	public void rank(Properties p, String method, String runFileOut, String candRunFilePath, boolean withTruePage) throws IOException, ParseException {
		HashMap<String, HashMap<String, Double>> paraPairRun = new HashMap<String, HashMap<String, Double>>();
		HashMap<String, ArrayList<String>> pageParaMap;
		ArrayList<String> allParas;
		if(withTruePage)
			pageParaMap = DataUtilities.getGTMapQrels(p.getProperty("data-dir")+"/"+p.getProperty("art-qrels"));
		else
			pageParaMap = DataUtilities.getPageParaMapFromRunfile(candRunFilePath);
		IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(p.getProperty("index-dir")).toPath()))));
		SimilarityCalculator sc = new SimilarityCalculator();
		ILexicalDatabase db = new NictWordNet();
		HashMap<String, double[]> gloveVecs = DataUtilities.readGloveFile(p);
		int vecSize = gloveVecs.get("the").length;
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(runFileOut)));
		Analyzer analyzer = new StandardAnalyzer();
		
		//HashMap<String, String> paraIDTextMap = new HashMap<String, String>();
		HashSet<String> uniqueParas = new HashSet<String>();
		for(String page:pageParaMap.keySet())
			uniqueParas.addAll(pageParaMap.get(page));
		allParas = new ArrayList<String>(uniqueParas);
		//StreamSupport.stream(pageParaMap.keySet().spliterator(), true).forEach(page -> {
		int pageCount = 0;
		for(String page:pageParaMap.keySet()) {
			ArrayList<String> parasInPage = pageParaMap.get(page);
			//StreamSupport.stream(parasInPage.spliterator(), true).forEach(para1 -> {
			for(int j=0; j<parasInPage.size(); j++) {
				String para1 = parasInPage.get(j);
				StreamSupport.stream(parasInPage.spliterator(), true).forEach(para2 -> {
					try {
						double simScore = 0;
						//String p1text = paraIDTextMap.get(para1);
						//String p2text = paraIDTextMap.get(para2);
						QueryParser qp = new QueryParser("paraid", analyzer);
						String p1text = is.doc(is.search(qp.parse(para1), 1).scoreDocs[0].doc).get("parabody");
						String p2text = is.doc(is.search(qp.parse(para2), 1).scoreDocs[0].doc).get("parabody");
						if(method.equals("ji")||method.equals("pat")||method.equals("wu")||method.equals("lin"))
							simScore = sc.calculateWordnetSimilarity(db, p1text, p2text, method);
						else 
							simScore = sc.calculateW2VCosineSimilarity(DataUtilities.getParaW2VVec(p, para1, gloveVecs, vecSize), DataUtilities.getParaW2VVec(p, para2, gloveVecs, vecSize));
						bw.write(para1+" Q0 "+para2+" 0 "+simScore+" PARASIM-"+method+"\n");
						System.out.print(para2+" is done\r");
						//System.out.println(para1+" Q0 "+para2+" 0 "+simScore+" PARASIM-"+method);
					} catch (IOException | ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
				System.out.print("All similarities for "+para1+" is calculated, "+(parasInPage.size()-(j+1))+" to go\r");
			}
			pageCount++;
			System.out.println(pageCount+": "+page+" is done, "+(pageParaMap.size()-pageCount)+" pages to go");
		}
		bw.close();
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}