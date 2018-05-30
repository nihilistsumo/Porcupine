package com.trema.pcpn.parasim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import com.trema.pcpn.util.DataUtilities;

public class SubsampleForRlib {
	
	public HashMap<String, ArrayList<String>> subSample(String qrelsPath, String artQrelsPath) {
		HashMap<String, ArrayList<String>> sample = new HashMap<String, ArrayList<String>>();
		Random rand = new Random();
		HashMap<String, ArrayList<String>> trueMap = DataUtilities.getGTMapQrels(qrelsPath);
		HashMap<String, ArrayList<String>> truePageMap = DataUtilities.getGTMapQrels(artQrelsPath);
		ArrayList<String> pages = new ArrayList<String>(truePageMap.keySet());
		ArrayList<String> secs = new ArrayList<String>(trueMap.keySet());
		for(String sec:trueMap.keySet()) {
			String currPage = sec.split("/")[0];
			ArrayList<String> parasInPage = truePageMap.get(currPage);
			ArrayList<String> parasInSec = trueMap.get(sec);
			if(parasInSec.size()>1) {
				int keyIndex = rand.nextInt(parasInSec.size());
				String keyPara = trueMap.get(sec).get(keyIndex);
				int keyRelIndex = rand.nextInt(parasInSec.size());
				while(keyIndex==keyRelIndex)
					keyRelIndex = rand.nextInt(parasInSec.size());
				String keyRelPara = parasInSec.get(keyRelIndex);
				ArrayList<String> candParas = new ArrayList<String>();
				candParas.add(keyRelPara);
				int keyNonrelPageIndex = rand.nextInt(parasInPage.size());
				while(parasInPage.get(keyNonrelPageIndex).equals(keyPara)||parasInPage.get(keyNonrelPageIndex).equals(keyRelPara))
					keyNonrelPageIndex = rand.nextInt(parasInPage.size());
				//String nonrelPage = pages.get(keyNonrelPageIndex);
				String keyNonrelPagePara = parasInPage.get(keyNonrelPageIndex);
				candParas.add(keyNonrelPagePara);
				int nonPageIndex = rand.nextInt(pages.size());
				while(pages.get(nonPageIndex).equals(currPage))
					nonPageIndex = rand.nextInt(pages.size());
				String nonRelPage = pages.get(nonPageIndex);
				int keyNonrelNonpageIndex = rand.nextInt(truePageMap.get(nonRelPage).size());
				candParas.add(truePageMap.get(nonRelPage).get(keyNonrelNonpageIndex));
				sample.put(keyPara, candParas);
			}
		}
		return sample;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
