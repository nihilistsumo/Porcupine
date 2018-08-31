package com.trema.pcpn.debug;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;

public class ParatextQueries {
	
	public Query getQueryFromParaID(String paraID, IndexSearcher is) throws IOException, ParseException {
		//QueryParser qpID = new QueryParser("Id", new StandardAnalyzer());
		QueryParser qpID = new QueryParser("paraid", new StandardAnalyzer());
		//QueryParser qp = new QueryParser("Text", new StandardAnalyzer());
		QueryParser qp = new QueryParser("parabody", new StandardAnalyzer());
		//String queryString = is.doc(is.search(qpID.parse(paraID), 1).scoreDocs[0].doc).get("Text");
		String queryString = is.doc(is.search(qpID.parse(paraID), 1).scoreDocs[0].doc).get("parabody");
		BooleanQuery.setMaxClauseCount(65536);
		Query q = qp.parse(QueryParser.escape(queryString));
		return q;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			String keyPara = "1707507e26d9b7d2fb11fb98f4f7db47a410b0ee";
			String retPara = "1bede7ed64240400da5cb5b5df6b90b9e7c489b9";
			//String indexDirPath = "/media/sumanta/Seagate Backup Plus Drive/indexes/paragraph-corpus-paragraph-index/paragraph.lucene";
			String indexDirPath = "/media/sumanta/Seagate Backup Plus Drive/indexes/index-rmv-stp";
			IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirPath).toPath()))));
			is.setSimilarity(new BM25Similarity());
			//QueryParser qpID = new QueryParser("Id", new StandardAnalyzer());
			QueryParser qpID = new QueryParser("paraid", new StandardAnalyzer());
			ParatextQueries ptQuery = new ParatextQueries();
			Query q = ptQuery.getQueryFromParaID(keyPara, is);
			for(String queryTerm:q.toString().split(" "))
				System.out.print(queryTerm.split(":")[1]+" ");
			System.out.println("\n"+is.explain(q, is.search(qpID.parse(retPara), 1).scoreDocs[0].doc));
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
