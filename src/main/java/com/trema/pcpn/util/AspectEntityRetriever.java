package com.trema.pcpn.util;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.FSDirectory;

public class AspectEntityRetriever {
	
	public String retrieveEntitiesFromAspText(String aspText, IndexSearcher is, Connection con) throws ParseException, IOException, SQLException {
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
		//System.out.println("Entites: "+ent);
		return ent.trim();
	}
	
	public void retrieveEntitiesFromAspect(Properties prop, String aspIndexPath, String indexPath) throws IOException, ClassNotFoundException, SQLException, ParseException {
		IndexReader reader = DirectoryReader.open(FSDirectory.open((new File(aspIndexPath).toPath())));
		IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexPath).toPath()))));
		Connection con = DataUtilities.getDBConnection(prop.getProperty("dbip"), prop.getProperty("db"), "paraent", prop.getProperty("dbuser"), prop.getProperty("dbpwd"));
		Connection conAspEnt = DataUtilities.getDBConnection(prop.getProperty("dbip"), prop.getProperty("db"), "aspent", prop.getProperty("dbuser"), prop.getProperty("dbpwd"));
		PreparedStatement preparedStatement;
		System.out.println("Starting...\n");
		for (int i=0; i<reader.maxDoc(); i++) {
			Document doc = reader.document(i);
			String aspID = doc.getField("Id").stringValue();
			preparedStatement = con.prepareStatement("select * from aspent where aspid = ?");
			preparedStatement.setString(1, aspID);
			ResultSet resultSet = preparedStatement.executeQuery();
			if(!resultSet.next()) {
				String aspText = doc.getField("Text").stringValue();
				String ent = this.retrieveEntitiesFromAspText(aspText, is, con);
				preparedStatement = con.prepareStatement("insert into aspent values (?,?)");
				preparedStatement.setString(1, aspID);
				preparedStatement.setString(2, ent);
				preparedStatement.executeUpdate();
				if(i%10000==0)
					System.out.print(".");
			}
		}
	}

}
