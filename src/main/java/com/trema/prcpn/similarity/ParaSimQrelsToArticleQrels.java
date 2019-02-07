package com.trema.prcpn.similarity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class ParaSimQrelsToArticleQrels {
	
	String inputQrels;
	String qrelsFilename;
	
	public ParaSimQrelsToArticleQrels(String infile, String outfile) {
		this.inputQrels = infile;
		this.qrelsFilename = outfile;
	}
	
	public void printQrels() throws IOException {
		HashMap<String, ArrayList<String>> inputQrels = new HashMap<String, ArrayList<String>>();
		String inputPath = this.inputQrels;
		BufferedReader br = new BufferedReader(new FileReader(new File(inputPath)));
		String line = br.readLine();
		String q, page, para;
		while(line != null){
			q = line.split(" ")[0];
			page = q.split(":")[0]+":"+q.split(":")[1];
			para = q.split(":")[2];
			if(inputQrels.keySet().contains(page)) {
				if(!inputQrels.get(page).contains(para))
					inputQrels.get(page).add(para);
			}
			else {
				ArrayList<String> paralist = new ArrayList<String>();
				paralist.add(para);
				inputQrels.put(page, paralist);
			}
			line = br.readLine();
		}
		br.close();
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(this.qrelsFilename)));
		for(String p:inputQrels.keySet()) {
			for(String pr:inputQrels.get(p)) {
				bw.write(p+" 0 "+pr+" 1\n");
			}
		}
		bw.close();
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			ParaSimQrelsToArticleQrels ob = new ParaSimQrelsToArticleQrels(args[0], args[1]);
			ob.printQrels();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
