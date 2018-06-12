package com.trema.pcpn.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.stream.StreamSupport;

import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.CborFileTypeException;
import edu.unh.cs.treccar_v2.read_data.CborRuntimeException;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;

public class ParaW2VConverter {
	
	public void convert(Properties prop, String parafilePath) {
		try {
			FileInputStream fis = new FileInputStream(new File(parafilePath));
			final Iterator<Data.Paragraph> paragraphIterator = DeserializeData.iterParagraphs(fis);
			final Iterable<Data.Paragraph> it = ()->paragraphIterator;
			HashMap<String, double[]> gloveVecs = DataUtilities.readGloveFile(prop);
			int i=0;
			StreamSupport.stream(it.spliterator(), true).forEach(p->{
				String[] paraTokens = removeStopWords(p.getTextOnly().toLowerCase()).split(" ");
				
			});
		} catch (CborRuntimeException | CborFileTypeException | IOException e) {
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

	}

}
