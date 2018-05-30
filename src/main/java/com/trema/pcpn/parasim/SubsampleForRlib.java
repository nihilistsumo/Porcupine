package com.trema.pcpn.parasim;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

import com.trema.pcpn.util.DataUtilities;

public class SubsampleForRlib {
	
	public HashMap<String, ArrayList<String>> subSample(String qrelsPath, String artQrelsPath) {
		HashMap<String, ArrayList<String>> sample = new HashMap<String, ArrayList<String>>();
		Random rand = new Random();
		HashMap<String, ArrayList<String>> trueMap = DataUtilities.getGTMapQrels(qrelsPath);
		HashMap<String, ArrayList<String>> truePageMap = DataUtilities.getGTMapQrels(artQrelsPath);
		ArrayList<String> pages = new ArrayList<String>(truePageMap.keySet());
		ArrayList<String> secs = new ArrayList<String>(trueMap.keySet());
		for(String sec:trueMap.keySet()) {
			String currPage = sec.split("/")[0];
			ArrayList<String> parasInPage = truePageMap.get(currPage);
			ArrayList<String> parasInSec = trueMap.get(sec);
			if(parasInSec.size()>1) {
				int keyIndex = rand.nextInt(parasInSec.size());
				String keyPara = trueMap.get(sec).get(keyIndex);
				int keyRelIndex = rand.nextInt(parasInSec.size());
				while(keyIndex==keyRelIndex)
					keyRelIndex = rand.nextInt(parasInSec.size());
				String keyRelPara = parasInSec.get(keyRelIndex);
				ArrayList<String> candParas = new ArrayList<String>();
				candParas.add(keyRelPara);
				int keyNonrelPageIndex = rand.nextInt(parasInPage.size());
				while(parasInPage.get(keyNonrelPageIndex).equals(keyPara)||parasInPage.get(keyNonrelPageIndex).equals(keyRelPara))
					keyNonrelPageIndex = rand.nextInt(parasInPage.size());
				//String nonrelPage = pages.get(keyNonrelPageIndex);
				String keyNonrelPagePara = parasInPage.get(keyNonrelPageIndex);
				candParas.add(keyNonrelPagePara);
				int nonPageIndex = rand.nextInt(pages.size());
				while(pages.get(nonPageIndex).equals(currPage))
					nonPageIndex = rand.nextInt(pages.size());
				String nonRelPage = pages.get(nonPageIndex);
				int keyNonrelNonpageIndex = rand.nextInt(truePageMap.get(nonRelPage).size());
				candParas.add(truePageMap.get(nonRelPage).get(keyNonrelNonpageIndex));
				sample.put(keyPara, candParas);
			}
		}
		return sample;
	}
	
	public HashMap<String, ArrayList<String>> detailedSubSample(String qrelsPath, String artQrelsPath) {
		HashMap<String, ArrayList<String>> sample = new HashMap<String, ArrayList<String>>();
		Random rand = new Random();
		HashMap<String, ArrayList<String>> trueMap = DataUtilities.getGTMapQrels(qrelsPath);
		HashMap<String, ArrayList<String>> truePageMap = DataUtilities.getGTMapQrels(artQrelsPath);
		ArrayList<String> pages = new ArrayList<String>(truePageMap.keySet());
		ArrayList<String> secs = new ArrayList<String>(trueMap.keySet());
		for(String sec:trueMap.keySet()) {
			String currPage = sec.split("/")[0];
			ArrayList<String> parasInPage = truePageMap.get(currPage);
			ArrayList<String> parasInSec = trueMap.get(sec);
			if(parasInSec.size()>1) {
				int keyIndex = rand.nextInt(parasInSec.size());
				String keyPara = trueMap.get(sec).get(keyIndex);
				int keyRelIndex = rand.nextInt(parasInSec.size());
				while(keyIndex==keyRelIndex)
					keyRelIndex = rand.nextInt(parasInSec.size());
				String keyRelPara = parasInSec.get(keyRelIndex);
				ArrayList<String> candParas = new ArrayList<String>();
				candParas.add(keyRelPara);
				int keyNonrelPageIndex = rand.nextInt(parasInPage.size());
				while(parasInPage.get(keyNonrelPageIndex).equals(keyPara)||parasInPage.get(keyNonrelPageIndex).equals(keyRelPara))
					keyNonrelPageIndex = rand.nextInt(parasInPage.size());
				//String nonrelPage = pages.get(keyNonrelPageIndex);
				String keyNonrelPagePara = parasInPage.get(keyNonrelPageIndex);
				candParas.add(keyNonrelPagePara);
				int nonPageIndex = rand.nextInt(pages.size());
				while(pages.get(nonPageIndex).equals(currPage))
					nonPageIndex = rand.nextInt(pages.size());
				String nonRelPage = pages.get(nonPageIndex);
				int keyNonrelNonpageIndex = rand.nextInt(truePageMap.get(nonRelPage).size());
				candParas.add(truePageMap.get(nonRelPage).get(keyNonrelNonpageIndex));
				candParas.add(sec);
				candParas.add(nonRelPage);
				sample.put(keyPara, candParas);
			}
		}
		return sample;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			Properties p = new Properties();
			p.load(new FileInputStream(new File("project.properties")));
			SubsampleForRlib sample = new SubsampleForRlib();
			HashMap<String, ArrayList<String>> paraMap = sample.detailedSubSample(p.getProperty("data-dir")+"/"+p.getProperty("top-qrels"), p.getProperty("data-dir")+"/"+p.getProperty("art-qrels"));
			IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(p.getProperty("index-dir")).toPath()))));
			Analyzer analyzer = new StandardAnalyzer();
			QueryParser qp = new QueryParser("paraid", analyzer);
			int count = 0;
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File("/home/sumanta/Documents/Porcupine-data/Porcupine-results/debug-and-excel-sheets/simpara-report")));
			for(String keyPara:paraMap.keySet()) {
				ArrayList<String> cand = paraMap.get(keyPara);
				bw.write("Keypara: "+keyPara+" in top-level section: "+cand.get(3)+"\n");
				bw.write("-------------------------------------\n");
				bw.write(is.doc(is.search(qp.parse(keyPara), 1).scoreDocs[0].doc).get("parabody")+"\n");
				bw.write("\nRelpara: "+cand.get(0)+"\n");
				bw.write("-------------------------------------\n");
				bw.write(is.doc(is.search(qp.parse(cand.get(0)), 1).scoreDocs[0].doc).get("parabody")+"\n");
				bw.write("\nnonRelpara in page: "+cand.get(1)+"\n");
				bw.write("-------------------------------------\n");
				bw.write(is.doc(is.search(qp.parse(cand.get(1)), 1).scoreDocs[0].doc).get("parabody")+"\n");
				bw.write("\nnonRelpara outside page: "+cand.get(2)+" from page: "+cand.get(4)+"\n");
				bw.write("-------------------------------------\n");
				bw.write(is.doc(is.search(qp.parse(cand.get(2)), 1).scoreDocs[0].doc).get("parabody")+"\n\n\n\n");
				count++;
				if(count>=100)
					break;
			}
			bw.close();
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
