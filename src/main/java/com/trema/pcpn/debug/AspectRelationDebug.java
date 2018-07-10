package com.trema.pcpn.debug;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
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
import org.apache.lucene.store.FSDirectory;

import com.trema.pcpn.aspect.AspectSimilarity;
import com.trema.pcpn.parasimutil.SubsampleForRlib;
import com.trema.pcpn.util.DataUtilities;
import com.trema.prcpn.similarity.ParaSimSanityCheck;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;

public class AspectRelationDebug {
	
	public void debug(Properties prop, String indexDirAspPath, String indexDirPath, String indexDirNoStops, String topQrelsPath, String artQrelsPath, int retAspNo) {
		try {
			IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirPath).toPath()))));
			IndexSearcher isNoStops = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirNoStops).toPath()))));
			IndexSearcher aspectIs = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirAspPath).toPath()))));
			QueryParser qpID = new QueryParser("paraid", new StandardAnalyzer());
			QueryParser qpAspText = new QueryParser("Text", new StandardAnalyzer());
			Random rand = new Random();
			SubsampleForRlib sampler = new SubsampleForRlib();
			HashMap<String, ArrayList<String>> sample = sampler.subSample(topQrelsPath, artQrelsPath);
			ILexicalDatabase db = new NictWordNet();
			Connection con = DataUtilities.getDBConnection(prop.getProperty("dbip"), prop.getProperty("db"), "paraent", prop.getProperty("dbuser"), prop.getProperty("dbpwd"));
			AspectSimilarity aspSim = new AspectSimilarity();
			for(String keyPara:sample.keySet()) {
				String queryString = isNoStops.doc(isNoStops.search(qpID.parse(keyPara), 1).scoreDocs[0].doc).get("parabody");
				BooleanQuery.setMaxClauseCount(65536);
				Query q = qpAspText.parse(QueryParser.escape(queryString));
				TopDocs retAspectsKeyPara = aspectIs.search(q, retAspNo);
				ArrayList<String> retParas = sample.get(keyPara);
				String relPara = retParas.get(0);
				System.out.println("\nKey "+keyPara+": "+is.doc(is.search(qpID.parse(keyPara), 1).scoreDocs[0].doc).get("parabody"));
				for(String ret:retParas) {
					if(ret.equalsIgnoreCase(relPara))
						System.out.println("\nRelevant "+ret+": "+is.doc(is.search(qpID.parse(ret), 1).scoreDocs[0].doc).get("parabody"));
					else
						System.out.println("\nNon-relevant "+ret+": "+is.doc(is.search(qpID.parse(ret), 1).scoreDocs[0].doc).get("parabody"));
				}
				System.out.println("\n\n");
				//this.printAspects(keyPara, retAspectsKeyPara.scoreDocs, aspectIs, con);
				Collections.shuffle(retParas);
				for(String retPara:retParas) {
					String retQueryString = isNoStops.doc(isNoStops.search(qpID.parse(retPara), 1).scoreDocs[0].doc).get("parabody");
					//BooleanQuery.setMaxClauseCount(65536);
					Query retQ = qpAspText.parse(QueryParser.escape(retQueryString));
					TopDocs retAspectsRetPara = aspectIs.search(retQ, retAspNo);
					
					System.out.println("\n\n");
					String rel = "Non-relevant";
					if(retPara.equalsIgnoreCase(relPara))
						rel = "Relevant";
					System.out.println(rel+" para "+retPara+" Aspects:");
					//this.printAspects(retPara, retAspectsRetPara.scoreDocs, aspectIs, con);
					
					double aspRelScore = aspSim.aspectRelationScore(retAspectsKeyPara, retAspectsRetPara, is, aspectIs, con, "ent", "na");
					double aspTextScore = aspSim.aspectRelationScore(retAspectsKeyPara, retAspectsRetPara, is, aspectIs, con, "asptext", "na");
					double aspLeadScore = aspSim.aspectRelationScore(retAspectsKeyPara, retAspectsRetPara, is, aspectIs, con, "asplead", "na");
					double aspectMatchRatio = aspSim.aspectMatchRatio(retAspectsKeyPara.scoreDocs, retAspectsRetPara.scoreDocs);
					double entMatchRatio = aspSim.entityMatchRatio(keyPara, retPara, con, "na");
					System.out.println("\nAspect relation score = "+aspRelScore);
					System.out.println("Aspect text score = "+aspTextScore);
					System.out.println("Aspect lead score = "+aspLeadScore);
					System.out.println("Aspect match ratio = "+aspectMatchRatio);
					System.out.println("Entity match ratio = "+entMatchRatio);
				}
			}
		} catch (IOException | ClassNotFoundException | SQLException | ParseException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void printAspects(String paraID, ScoreDoc[] aspects, IndexSearcher aspectIs, Connection con) {
		AspectSimilarity aspSim = new AspectSimilarity();
		int rank = 1;
		System.out.println("Retrieved aspects");
		System.out.println("-----------------\n");
		for(ScoreDoc asp:aspects) {
			try {
				Document aspDoc = aspectIs.doc(asp.doc);
				//String heading = aspDoc.getField("Headings").stringValue();
				String id = aspDoc.getField("Id").stringValue();
				String aspParas = aspDoc.getField("ParasInSection").stringValue();
				//String leadText = aspDoc.getField("LeadText").stringValue();
				//String text = aspDoc.getField("Text").stringValue();
				//String title = aspDoc.getField("Title").stringValue();
				
				String[] entitiesInAsp = aspSim.retrieveEntitiesFromAspParas(aspParas, con);
				//System.out.println(rank+". ID: "+id+"\nHeading: "+heading+"\nTitle: "+title+"\nLead Text: "+leadText+"\n\nText: "+text+"\n");
				System.out.println(rank+". Aspect ID: "+id);
				for(String ent:entitiesInAsp) {
					System.out.print(ent+" ");
				}
				System.out.println();
				rank++;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			Properties prop = new Properties();
			prop.load(new FileInputStream(new File("project.properties")));
			AspectRelationDebug debug = new AspectRelationDebug();
			String indexDirAsp = args[0];
			String indexDir = args[1];
			String indexDirNoStops = args[2];
			String topQrelsPath = args[3];
			String artQrelsPath = args[4];
			int retAspNo = Integer.parseInt(args[5]);
			debug.debug(prop, indexDirAsp, indexDir, indexDirNoStops, topQrelsPath, artQrelsPath, retAspNo);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
