package com.trema.prcpn.similarity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.stream.StreamSupport;

import com.trema.pcpn.util.DataUtilities;

public class ParaSimRankerRand {
	
	public void rank(String candRunFilePath, String outRunPath, int retNo) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outRunPath)));
		HashMap<String, ArrayList<String>> pageParaMap = DataUtilities.getPageParaMapFromRunfile(candRunFilePath);
		for(String pageID:pageParaMap.keySet()) {
			ArrayList<String> queryParaIDs = pageParaMap.get(pageID);
			StreamSupport.stream(queryParaIDs.spliterator(), true).forEach(keyPara -> { 
				try {
					HashMap<String, Float> retrievedResult = new HashMap<String, Float>();
					int count = 0;
					Random rand = new Random();
					while (count < retNo) {
						String retParaID = queryParaIDs.get(rand.nextInt(queryParaIDs.size()));
						if(!retParaID.equals(keyPara)) {
							retrievedResult.put(retParaID, 1.0f/count);
							count++;
						}
					}
					for(String para:retrievedResult.keySet()) {
						bw.write(pageID+":"+keyPara+" Q0 "+para+" 0 "+retrievedResult.get(para)+" RAND-MAP\n");
					}
					//System.out.print("Done Para: "+keyPara+"\r");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
			System.out.println();
			System.out.println(pageID+" is done\n");
		}
		//System.out.println("Total no. of queries: "+allQueries.size());
		bw.close();
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			ParaSimRankerRand psrand = new ParaSimRankerRand();
			psrand.rank("/home/sumanta/Documents/Mongoose-data/Mongoose-results/page-runs-basic-sim-and-fixed/lucene-basic/train/cv-comb-run", 
					"/home/sumanta/Documents/Porcupine-data/Porcupine-results/sim-run-files/parasim-rand", 200);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
