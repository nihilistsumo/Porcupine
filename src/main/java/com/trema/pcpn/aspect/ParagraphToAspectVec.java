package com.trema.pcpn.aspect;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.search.ScoreDoc;

public class ParagraphToAspectVec {
	
	// REAL_ASPVEC_SIZE is the number of dimensions for which we calculate aspect rank score from the index
	// Rest of the dimensions in the aspvec is filled up by x where 0 \leq x \leq min, min = min(aspect rank scores)
	public static final int REAL_ASPVEC_SIZE = 10000;
	
	public LinkedHashMap<String, Float> getAspectVec(String paraText, IndexSearcher aspectIs) throws IOException, ParseException {
		//long vecLen = aspectIs.collectionStatistics("Id").docCount();
		LinkedHashMap<String, Float> aspVec = new LinkedHashMap<String, Float>();
		QueryParser qp = new QueryParser("Text", new StandardAnalyzer());
		BooleanQuery.setMaxClauseCount(65536);
		Query q = qp.parse(QueryParser.escape(paraText));
		TopDocs retAspectsPara = aspectIs.search(q, REAL_ASPVEC_SIZE);
		for(ScoreDoc asp:retAspectsPara.scoreDocs) {
			Document aspDoc = aspectIs.doc(asp.doc);
			aspVec.put(aspDoc.get("Id"), asp.score);
		}
		return normalizeVec(aspVec);
	}
	
	public SparseAspectVector getAspectVec(String paraText, IndexSearcher aspectIs, int vecLen) throws IOException, ParseException {
		QueryParser qp = new QueryParser("Text", new StandardAnalyzer());
		BooleanQuery.setMaxClauseCount(65536);
		Query q = qp.parse(QueryParser.escape(paraText));
		TopDocs retAspectsPara = aspectIs.search(q, REAL_ASPVEC_SIZE);
		SparseAspectVector aspVec = new SparseAspectVector(REAL_ASPVEC_SIZE);
		int i=0;
		// aspVec arrays will have the aspect rank scores sorted; aspVec.values[0] will have the highest rank score
		for(ScoreDoc asp:retAspectsPara.scoreDocs) {
			aspVec.set(i, asp.doc, asp.score);
			i++;
		}
		aspVec.normalize();
		return aspVec;
	}
	
	public class SparseAspectVector {
		private int[] indexes; // stores aspect docid
		private float[] values; // asp doc ID -> asp rank score; indexes[i] -> values[i]
		private boolean isNormalized;
		
		public SparseAspectVector(int maxNonzeroLen) {
			this.indexes = new int[maxNonzeroLen];
			this.values = new float[maxNonzeroLen];
			this.isNormalized = false;
			for(int i=0; i<this.indexes.length; i++)
				this.indexes[i] = -1; // this means it's corresponding values entry is not valid
		}
		
		// gets ith element of vector value
		public float get(int i) {
			return this.values[i];
		}
		
		// gets ith 
		public int getAspDocID(int i) {
			return this.indexes[i];
		}
		
		public void set(int i, int aspDocID, float val) {
			this.indexes[i] = aspDocID;
			this.values[i] = val;
			if(this.isNormalized)
				this.isNormalized = false;
		}
		
		public void normalize() {
			float sumScore = 0;
			for(int i=0; i<this.values.length; i++)
				sumScore+=this.values[i];
			for(int i=0; i<this.values.length; i++)
				this.values[i]/=sumScore;
			this.isNormalized = true;
		}
		
		public boolean isNormalized() {
			return isNormalized;
		}
	}
	
	private LinkedHashMap<String, Float> normalizeVec(LinkedHashMap<String, Float> vec) {
		float sumScore = 0;
		for(String k:vec.keySet())
			sumScore+=vec.get(k);
		for(String k:vec.keySet())
			vec.put(k, vec.get(k)/sumScore);
		return vec;
	}
	
	private float[] normalizeVec(float[] vec) {
		float sumScore = 0;
		for(int i=0; i<vec.length; i++)
			sumScore+=vec[i];
		for(int i=0; i<vec.length; i++)
			vec[i]/=sumScore;
		return vec;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			String paraID = "cfa1823654140fbdd08a8b6b910b28b31ec85ce5";
			String indexDirPath = "/media/sumanta/Seagate Backup Plus Drive/indexes/paragraph-corpus-paragraph-index/paragraph.lucene";
			String aspIndexDirPath = "/media/sumanta/Seagate Backup Plus Drive/indexes/aspect.lucene";
			IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirPath).toPath()))));
			IndexSearcher aspIs = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(aspIndexDirPath).toPath()))));
			QueryParser qpID = new QueryParser("Id", new StandardAnalyzer());
			String paraText = is.doc(is.search(qpID.parse(paraID), 1).scoreDocs[0].doc).get("Text");
			
			ParagraphToAspectVec p2av = new ParagraphToAspectVec();
			//LinkedHashMap<String, Float> aspVec = p2av.getAspectVec(paraText, aspIs);
			SparseAspectVector aspVec = p2av.getAspectVec(paraText, aspIs, (int)aspIs.collectionStatistics("Id").docCount());
			for(int i=0; i<10; i++)
				System.out.println(aspVec.get(i));
			System.out.println("done");
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
