package com.trema.pcpn.debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

import com.trema.pcpn.util.DataUtilities;

public class CompareBM25AspText {
	
	public void printDetails(String inputFile, String parasimQrels, String artQrels, String isPath) throws IOException, ParseException {
		IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(isPath).toPath()))));
		HashMap<String, ArrayList<String>> parasimQrelsData = DataUtilities.getGTMapQrels(parasimQrels);
		HashMap<String, ArrayList<String>> artQrelsData = DataUtilities.getGTMapQrels(artQrels);
		QueryParser qpID = new QueryParser("Id", new StandardAnalyzer());
		BufferedReader br = new BufferedReader(new FileReader(new File(inputFile)));
		String line = br.readLine();
		while(line!=null) {
			if(!line.startsWith("#")) {
				String[] parts = line.split(",");
				String query = parts[0];
				double bm25Score = Double.parseDouble(parts[1]);
				double aspTextScore = Double.parseDouble(parts[2]);
				String pageID = "enwiki:"+query.split(":")[1];
				String keyparaID = query.split(":")[2];
				String keyParaText = is.doc(is.search(qpID.parse(keyparaID), 1).scoreDocs[0].doc).get("Text");
				System.out.println(pageID+"\nKeypara ID: "+keyparaID+"\n"+keyParaText);
				System.out.println("BM25 score: "+bm25Score+", aspTextScore: "+aspTextScore+"\n");
				ArrayList<String> allParasInPage = artQrelsData.get(pageID);
				ArrayList<String> allRelParas = parasimQrelsData.get(query);
				System.out.println("\n-------------------------------------------------------------\n");
				for(String relPara:allRelParas) {
					System.out.println("Relevant Para ID: "+relPara);
					String paraText = is.doc(is.search(qpID.parse(relPara), 1).scoreDocs[0].doc).get("Text");
					System.out.println(paraText+"\n");
				}
				System.out.println("\n-------------------------------------------------------------\n");
				for(String para:allParasInPage) {
					if(!allRelParas.contains(para)) {
						System.out.println("Non-relevant Para ID: "+para);
						String paraText = is.doc(is.search(qpID.parse(para), 1).scoreDocs[0].doc).get("Text");
						System.out.println(paraText+"\n");
					}
				}
				System.out.println("\n\n==============================================================");
				System.out.println("==============================================================\n\n");
			}
			line = br.readLine();
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CompareBM25AspText cmp = new CompareBM25AspText();
		try {
			cmp.printDetails(args[0], args[1], args[2], args[3]);
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
