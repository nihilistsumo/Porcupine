package com.trema.pcpn.aspect;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.json.simple.JSONObject;

import com.trema.pcpn.aspect.ParagraphToAspectVec.SparseAspectVector;

public class AspectVecGenerator {
	
	public void processParagraphs(String artQrels, String indexDirPath, String aspIndexDirPath, String outputFile) throws IOException, ParseException {
		IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirPath).toPath()))));
		IndexSearcher aspIs = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(aspIndexDirPath).toPath()))));
		//Gson gson = new Gson();
		QueryParser qpID = new QueryParser("Id", new StandardAnalyzer());
		
		ParagraphToAspectVec p2av = new ParagraphToAspectVec();
		BufferedReader br = new BufferedReader(new FileReader(new File(artQrels)));
		String line = br.readLine();
		HashMap<String, SparseAspectVector> paraAspVecMap = new HashMap<String, SparseAspectVector>();
		int count = 0;
		while(line!=null) {
			String paraID = line.split(" ")[2];
			String paraText = is.doc(is.search(qpID.parse(paraID), 1).scoreDocs[0].doc).get("Text");
			SparseAspectVector aspVec = p2av.getAspectVec(paraText, aspIs, (int)aspIs.collectionStatistics("Id").docCount());
			paraAspVecMap.put("para:"+paraID, aspVec);
			
			System.out.print(".");
			count++;
			if(count>=25) {
				System.out.print("+\n");
				count = 0; 
			}
			
			line = br.readLine();
		}
		
		JSONObject paraVecMap = new JSONObject();
		for(String paraID:paraAspVecMap.keySet()) {
			SparseAspectVector aspVec = paraAspVecMap.get(paraID);
			JSONObject vec = new JSONObject();
			for(int i=0; i<ParagraphToAspectVec.REAL_ASPVEC_SIZE; i++)
				vec.put("asp:"+aspVec.getAspDocID(i), aspVec.get(i));
			paraVecMap.put(paraID, vec);
		}
		FileWriter fw = new FileWriter(outputFile);
		fw.write(paraVecMap.toJSONString());
		fw.flush();
		fw.close();
		br.close();
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			String artQrels = "/home/sumanta/Documents/Porcupine-data/Porcupine-results/aspect-vector-results/train.pages.cbor-article-part.qrels";
			String indexDirPath = "/media/sumanta/Seagate Backup Plus Drive/indexes/paragraph-corpus-paragraph-index/paragraph.lucene";
			String aspIndexDirPath = "/media/sumanta/Seagate Backup Plus Drive/indexes/aspect.lucene";
			String outputFile = "/home/sumanta/Documents/Porcupine-data/Porcupine-results/aspect-vector-results/para-asp-vecs";
			AspectVecGenerator aspVecGen = new AspectVecGenerator();
			aspVecGen.processParagraphs(artQrels, indexDirPath, aspIndexDirPath, outputFile);
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
