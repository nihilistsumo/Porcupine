package com.trema.pcpn.debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

public class ParaSimQrelsExaminer {
	
	public void examine(Properties p) throws IOException {
		HashMap<String, ArrayList<String>> qrels = new HashMap<String, ArrayList<String>>();
		BufferedReader br = new BufferedReader(new FileReader(new File(p.getProperty("out-dir"))+"/"+p.getProperty("parapair-qrels")));
		String line = br.readLine();
		String q,r;
		while(line!=null) {
			q = line.split(" ")[0];
			r = line.split(" ")[2];
			line = br.readLine();
			if(qrels.containsKey(q)) {
				qrels.get(q).add(r);
			} 
			else {
				ArrayList<String> paralist = new ArrayList<String>();
				paralist.add(r);
				qrels.put(q, paralist);
			}
		}
		for(String qpara:qrels.keySet()) {
			ArrayList<String> relParas = qrels.get(qpara);
			ArrayList<String> seen = new ArrayList<String>();
			for(String rel:relParas) {
				if(seen.contains(rel))
					System.out.println("Duplicate para ID "+rel+" for paraquery "+qpara);
				seen.add(rel);
			}
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			Properties p = new Properties();
			p.load(new FileInputStream(new File("project.properties")));
			ParaSimQrelsExaminer ex = new ParaSimQrelsExaminer();
			ex.examine(p);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
