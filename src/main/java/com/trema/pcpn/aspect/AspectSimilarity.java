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

import com.trema.pcpn.util.DataUtilities;

public class AspectSimilarity {
	
	public void insertEntitiesInDB(String paraEntitiesFile, Connection con) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(paraEntitiesFile)));
			String line = br.readLine();
			while(line!=null) {
				String[] elements = line.split(" ");
				if(elements.length>1) {
					String paraID = elements[0];
					PreparedStatement preparedStatement = con.prepareStatement("select * from paraent where paraid = ?");
					preparedStatement.setString(1, paraID);
					ResultSet resultSet = preparedStatement.executeQuery();
					if(!resultSet.next()) {
						String entString = "";
						for(int i=1; i<elements.length; i++) {
							entString+=elements[i];
						}
						preparedStatement = con.prepareStatement("insert into paraent values (?,?)");
						preparedStatement.setString(1, paraID);
						preparedStatement.setString(2, entString);
						preparedStatement.executeUpdate();
					}
				}
				line = br.readLine();
			}
			br.close();
		} catch (IOException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public ArrayList<String> findCommonEntities(String paraID1, String paraID2) {
		ArrayList<String> commonEntities = new ArrayList<String>();
		
		return commonEntities;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
