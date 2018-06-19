package com.trema.pcpn.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.stream.StreamSupport;

import org.apache.lucene.queryparser.classic.ParseException;

import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.CborFileTypeException;
import edu.unh.cs.treccar_v2.read_data.CborRuntimeException;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;

public class ParaW2VConverter {
	
	public void convert(Properties prop, String parafilePath, String ip, String db, String table, String dbUser, String dbPwd) {
		try {
			Class.forName("org.postgresql.Driver");
			// DriverManager.getConnection("jdbc:postgresql://localhost:5432/testdb", "postgres", "123");
			Connection connect = DriverManager.getConnection("jdbc:postgresql://"+ip+"/"+db, dbUser, dbPwd);
			Statement statement = connect.createStatement();
			
			FileInputStream fis = new FileInputStream(new File(parafilePath));
			final Iterator<Data.Paragraph> paragraphIterator = DeserializeData.iterParagraphs(fis);
			final Iterable<Data.Paragraph> it = ()->paragraphIterator;
			HashMap<String, double[]> gloveVecs = DataUtilities.readGloveFile(prop);
			int vecSize = gloveVecs.get("the").length;
			int i=0;
			StreamSupport.stream(it.spliterator(), true).forEach(p->{
				try {
					String paraID = p.getParaId();
					PreparedStatement preparedStatement = connect.prepareStatement("select * from wordvecs where paraid = ?");
					preparedStatement.setString(1, paraID);
				    ResultSet resultSet = preparedStatement.executeQuery();
				    if(!resultSet.next()) {
						String[] paraTokens = removeStopWords(p.getTextOnly().toLowerCase()).split(" ");
						double[] vec = DataUtilities.getParaW2VVec(prop, p.getParaId(), gloveVecs, vecSize);
						Object[] vecObjArray = new Object[vec.length];
						for(int j=0; j<vec.length; j++)
							vecObjArray[j] = vec[j];
						//ByteArrayOutputStream baos = new ByteArrayOutputStream();
					    //ObjectOutputStream oos = new ObjectOutputStream(baos);
					    //oos.writeObject(vec);
						//byte[] vecBytes = baos.toByteArray();
						preparedStatement = connect.prepareStatement("insert into wordvecs values (?,?)");
						preparedStatement.setString(1, paraID);
						//ByteArrayInputStream bais = new ByteArrayInputStream(vecBytes);
						//preparedStatement.setBinaryStream(2, bais);
						Array vecArray = connect.createArrayOf("float8", vecObjArray);
						preparedStatement.setArray(2, vecArray);
						preparedStatement.executeUpdate();
						System.out.println(paraID+" done");
				    }
				} catch (IOException | ParseException | SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
		} catch (CborRuntimeException | CborFileTypeException | IOException | ClassNotFoundException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private String removeStopWords(String inputText) {
		String processedText = "";
		String[] inputTokens = inputText.split(" ");
		for(String token:inputTokens) {
			if(!DataUtilities.stopwords.contains(token))
				processedText+=token+" ";
		}
		return processedText.trim();
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			Properties prop = new Properties();
			prop.load(new FileInputStream(new File("project.properties")));
			ParaW2VConverter pw2v = new ParaW2VConverter();
			//pw2v.convert(prop, "/home/sumanta/Documents/Mongoose-data/trec-data/benchmarkY1-train/dedup.articles-paragraphs.cbor");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
