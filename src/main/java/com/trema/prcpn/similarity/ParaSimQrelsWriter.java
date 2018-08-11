package com.trema.prcpn.similarity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public class ParaSimQrelsWriter {
	
	String inputQrels;
	String qrelsFilename;
	
	public ParaSimQrelsWriter(String infile, String outfile){
		this.inputQrels = infile;
		this.qrelsFilename = outfile;
	}
	
	public void printQrels() throws IOException{
		HashMap<String, ArrayList<String>> inputQrels = new HashMap<String, ArrayList<String>>();
		String inputPath = this.inputQrels;
		BufferedReader br = new BufferedReader(new FileReader(new File(inputPath)));
		String line = br.readLine();
		String q,para;
		while(line != null){
			q = line.split(" ")[0];
			para = line.split(" ")[2];
			if(inputQrels.keySet().contains(q)) {
				inputQrels.get(q).add(para);
			}
			else{
				ArrayList<String> paralist = new ArrayList<String>();
				paralist.add(para);
				inputQrels.put(q, paralist);
			}
			line = br.readLine();
		}
		br.close();
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(this.qrelsFilename)));
		ArrayList<String> plist;
		HashMap<String, ArrayList<String>> paraQrels = new HashMap<String, ArrayList<String>>();
		for(String qid:inputQrels.keySet()){
			plist = inputQrels.get(qid);
			for(String pid:plist){
				if(paraQrels.keySet().contains(qid.split("/")[0]+":"+pid)) {
					for(String simpid:plist){
						if(!pid.equals(simpid) && !paraQrels.get(qid.split("/")[0]+":"+pid).contains(simpid))
							paraQrels.get(qid.split("/")[0]+":"+pid).add(simpid);
					}
				}
				else {
					ArrayList<String> simlist = new ArrayList(plist);
					simlist.remove(pid);
					paraQrels.put(qid.split("/")[0]+":"+pid, simlist);
				}
			}
		}
		for(String qpara:paraQrels.keySet()) {
			for(String relpara:paraQrels.get(qpara))
				bw.write(qpara+" 0 "+relpara+" 1\n");
		}
		bw.close();
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			String inputPath = args[0];
			String outputPath = args[1];
			//ParaSimQrelsWriter pw = new ParaSimQrelsWriter(prop, "/home/sumanta/Documents/Porcupine-data/Porcupine-results/qrels/fold-4-simpara-train-tree-for-cluster.qrels", "tree");
			ParaSimQrelsWriter pw = new ParaSimQrelsWriter(inputPath, outputPath);
			pw.printQrels();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
