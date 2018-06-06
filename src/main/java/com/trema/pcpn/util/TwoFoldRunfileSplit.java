package com.trema.pcpn.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

public class TwoFoldRunfileSplit {
	
	public void startProcess(String runfilesDir, String titlesPath, String outputDir1, String outputDir2) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(titlesPath)));
			HashSet<String> titleSet1 = new HashSet<String>();
			HashSet<String> titleSet2 = new HashSet<String>();
			String line = br.readLine();
			boolean set1 = true;
			while(line!=null) {
				if(set1)
					titleSet1.add(line.replaceAll(" ", "%20"));
				else
					titleSet2.add(line.replaceAll(" ", "%20"));
				set1 = !set1;
				line = br.readLine();
			}
			br.close();
			
			File folderOfRunfiles = new File(runfilesDir);
			File[] runfiles = folderOfRunfiles.listFiles();
			Arrays.sort(runfiles);
			for(File rf:runfiles) {
				BufferedWriter bw1 = new BufferedWriter(new FileWriter(new File(outputDir1+"/set1"+rf.getName())));
				BufferedWriter bw2 = new BufferedWriter(new FileWriter(new File(outputDir2+"/set2"+rf.getName())));
				BufferedReader brRun = new BufferedReader(new FileReader(rf));
				String rfline = brRun.readLine();
				while(rfline!=null) {
					if(titleSet1.contains(rfline.split(" ")[0].split(":")[1]))
						bw1.write(rfline+"\n");
					else
						bw2.write(rfline+"\n");
					rfline = brRun.readLine();
				}
				bw1.close();
				bw2.close();
				brRun.close();
			}
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		TwoFoldRunfileSplit split = new TwoFoldRunfileSplit();
		split.startProcess("/home/sumanta/Documents/Porcupine-data/dummy", 
				"/home/sumanta/Documents/Mongoose-data/trec-data/benchmarkY1-train/titles", 
				"/home/sumanta/Documents/Porcupine-data/dummy-out/set1",
				"/home/sumanta/Documents/Porcupine-data/dummy-out/set2");
	}

}
