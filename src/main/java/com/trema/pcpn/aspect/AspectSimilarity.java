package com.trema.pcpn.aspect;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;

import com.trema.pcpn.util.DataUtilities;

public class AspectSimilarity {
	
	public void insertEntitiesInDB(String paraEntitiesFile, Connection con) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(paraEntitiesFile)));
			String line = br.readLine();
			System.out.println("Starting...");
			int j=0;
			while(line!=null) {
				String[] elements = line.split("\t");
				if(elements.length>1) {
					String paraID = elements[0];
					PreparedStatement preparedStatement = con.prepareStatement("select * from paraent where paraid = ?");
					preparedStatement.setString(1, paraID);
					ResultSet resultSet = preparedStatement.executeQuery();
					if(!resultSet.next()) {
						String entString = elements[1];
						preparedStatement = con.prepareStatement("insert into paraent values (?,?)");
						preparedStatement.setString(1, paraID);
						preparedStatement.setString(2, entString);
						try {
							preparedStatement.executeUpdate();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							System.out.println("\ntried to process: "+line);
							e.printStackTrace();
						}
					}
				}
				j++;
				if (j % 10000 == 0) {
	                System.out.print('.');
	            }
				line = br.readLine();
			}
			System.out.println("\nDone");
			br.close();
		} catch (IOException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public double aspectEntityRelationScore(ScoreDoc[] keyAspects, ScoreDoc[] retAspects, IndexSearcher is, IndexSearcher aspIs, Connection con, String option) throws IOException, ParseException, SQLException {
		double score = 0;
		for(ScoreDoc keyAsp:keyAspects) {
			Document keyAspDoc = aspIs.doc(keyAsp.doc);
			String keyAspText = keyAspDoc.getField("Text").stringValue();
			String[] keyAspEntities = this.retrieveEntitiesFromAspText(keyAspText, is, con);
			for(ScoreDoc retAsp:retAspects) {
				Document retAspDoc = aspIs.doc(retAsp.doc);
				String retAspText = retAspDoc.getField("Text").stringValue();
				String[] retAspEntities = this.retrieveEntitiesFromAspText(retAspText, is, con);
				double currEntSimScore = this.entitySimilarityScore(Arrays.asList(keyAspEntities), Arrays.asList(retAspEntities), option);
				score+=currEntSimScore*keyAsp.score*retAsp.score;
			}
		}
		return score;
	}
	
	public String[] retrieveEntitiesFromAspText(String aspText, IndexSearcher is, Connection con) throws ParseException, IOException, SQLException {
		String ent = "";
		String[] aspParas = aspText.split("\n\n\n")[0].split("\n");
		QueryParser qp = new QueryParser("parabody", new StandardAnalyzer());
		//BooleanQuery.setMaxClauseCount(65536);
		
		for(String aspPara:aspParas) {
			if(aspPara.endsWith(".")) {
				Query q = qp.parse(QueryParser.escape(aspPara));
				//Query q = qp.parse(aspPara);
				Document paraDoc = is.doc(is.search(q, 1).scoreDocs[0].doc);
				String paraText = paraDoc.get("parabody");
				if(paraText.equalsIgnoreCase(aspPara)) {
					String paraID = paraDoc.get("paraid");
					PreparedStatement preparedStatement = con.prepareStatement("select ent from paraent where paraid = ?");
					preparedStatement.setString(1, paraID);
					ResultSet resultSet = preparedStatement.executeQuery();
					if(resultSet.next())
						ent += " "+resultSet.getString(1);
				}
			}
		}
		System.out.println("Entites: "+ent);
		return ent.trim().split(" ");
	}
	
	public double entitySimilarityScore(List<String> entListKey, List<String> entListRet, String option) {
		double score = 0;
		for(String ent:entListKey) {
			if(entListRet.contains(ent))
				score+=1.0;
		}
		score/=entListKey.size();
		return score;
	}
	
	public double aspectMatchRatio(ScoreDoc[] keyAspects, ScoreDoc[] retAspects) {
		int match = 0;
		for(ScoreDoc d:keyAspects) {
			for(ScoreDoc retD:retAspects) {
				if(d.doc==retD.doc)
					match++;
			}
		}
		return (double)match/keyAspects.length;
	}
	
	public ArrayList<String> findCommonEntities(String paraID1, String paraID2, Connection con, String print) {
		ArrayList<String> commonEntities = new ArrayList<String>();
		try {
			PreparedStatement preparedStatement = con.prepareStatement("select ent from paraent where paraid = ?");
			preparedStatement.setString(1, paraID1);
			ResultSet resultSet = preparedStatement.executeQuery();
			if(!resultSet.next()) {
				if(print.equalsIgnoreCase("print"))
					System.out.println("No entities linked in Keypara "+paraID1);
				return commonEntities;
			}
			String ent1 = resultSet.getString(1);
			if(print.equalsIgnoreCase("print")) {
				System.out.println("\nKeypara "+paraID1+" entities: "+ent1);
			}
			preparedStatement.setString(1, paraID2);
			resultSet = preparedStatement.executeQuery();
			if(!resultSet.next()) {
				if(print.equalsIgnoreCase("print"))
					System.out.println("No entities linked in ret para "+paraID2);
				return commonEntities;
			}
			String ent2 = resultSet.getString(1);
			if(print.equalsIgnoreCase("print")) {
				System.out.println("Ret para "+paraID2+" entities: "+ent2);
			}
			List<String> entList1 = Arrays.asList(ent1.split(" "));
			List<String> entList2 = Arrays.asList(ent2.split(" "));
			for(String e:entList1) {
				if(entList2.contains(e))
					commonEntities.add(e);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return commonEntities;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
