package com.trema.pcpn.debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.trema.pcpn.util.MapUtil;

public class TopicStats {
	
	public void printStats(String topicsFilePath) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(topicsFilePath)));
			Map<String, Integer> stats = new HashMap<String, Integer>();
			String line = br.readLine();
			HashSet<String> secInPage = new HashSet<String>();
			while(line!=null) {
				if(line.length()>0) {
					String topSection = line.split("/")[1].toLowerCase().replaceAll("[/,%20]", " ").replaceAll(" +", " ");
					secInPage.add(topSection);
					/*
					if(!stats.keySet().contains(topSection)) {
						stats.put(topSection, 1);
					}
					else {
						int freq = stats.get(topSection)+1;
						stats.put(topSection, freq);
					}
					*/
				}
				else {
					for(String sec:secInPage) {
						if(!stats.keySet().contains(sec)) {
							stats.put(sec, 1);
						}
						else {
							int freq = stats.get(sec)+1;
							stats.put(sec, freq);
						}
					}
					secInPage = new HashSet<String>();
				}
				line = br.readLine();
			}
			for(Map.Entry<String, Integer> entry : MapUtil.sortByValue(stats).entrySet())
				System.out.println(entry.getKey()+" "+entry.getValue());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		TopicStats t = new TopicStats();
		t.printStats("/home/sumanta/Documents/Mongoose-data/trec-data/benchmarkY1-train/topics");
	}

}