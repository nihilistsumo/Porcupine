package com.trema.prcpn.similarity;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.impl.JiangConrath;
import edu.cmu.lti.ws4j.impl.Lin;
import edu.cmu.lti.ws4j.impl.Path;
import edu.cmu.lti.ws4j.impl.WuPalmer;

public class SimilarityCalculator {
	
	public double calculateWordnetSimilarity(ILexicalDatabase db, String paraText1, String paraText2, String method) {
		double simScore = 0.0;
		String[] tokens1 = paraText1.toLowerCase().split(" ");
		String[] tokens2 = paraText2.toLowerCase().split(" ");
		//ILexicalDatabase db = new NictWordNet();
		double[][] simMatrix;
		switch(method) {
		case "ji":
			JiangConrath ji = new JiangConrath(db);
			simMatrix = ji.getNormalizedSimilarityMatrix(tokens1, tokens2);
			simScore = this.avgMatrix(simMatrix);
			//System.out.println(simScore);
			break;
		case "pat":
			Path pat = new Path(db);
			simMatrix = pat.getNormalizedSimilarityMatrix(tokens1, tokens2);
			simScore = this.avgMatrix(simMatrix);
			//System.out.println(simScore);
			break;
		case "wu":
			WuPalmer wu = new WuPalmer(db);
			simMatrix = wu.getNormalizedSimilarityMatrix(tokens1, tokens2);
			simScore = this.avgMatrix(simMatrix);
			//System.out.println(simScore);
			break;
		case "lin":
			Lin lin = new Lin(db);
			simMatrix = lin.getNormalizedSimilarityMatrix(tokens1, tokens2);
			simScore = this.avgMatrix(simMatrix);
			//System.out.println(simScore);
			break;
		}
		return simScore;
	}
	
	public double calculateW2VCosineSimilarity(double[] vec1, double[] vec2) {
		double simScore = 0.0;
		simScore = this.getDotProduct(vec1, vec2)/(this.getSquaredSum(vec1)*this.getSquaredSum(vec2));
		return simScore;
	}
	
	public double calculateEntitySimilarity(String paraID1, String paraID2) {
		double simScore = 0.0;
		
		return simScore;
	}
	
	public double avgMatrix(double[][] mat) {
		double avg = 0.0;
		for(int i=0; i<mat.length; i++) {
			for(int j=0; j<mat[0].length; j++) {
				avg+=mat[i][j];
			}
		}
		avg = avg/(mat.length*mat[0].length);
		return avg;
	}
	
	public double getDotProduct(double[] a, double[] b){
		double val = 0;
		for(int i=0; i<a.length; i++)
			val+=a[i]*b[i];
		return val;
	}

	public double getSquaredSum(double[] a) {
		double val = 0;
		for(int i=0; i<a.length; i++)
			val+=a[i]*a[i];
		val = Math.sqrt(val);
		return val;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String s1 = "Biodiversity is not evenly distributed, rather it varies greatly across the globe as well as within regions. "
				+ "Among other factors, the diversity of all living things (biota) depends on temperature, precipitation, altitude, soils, geography and the presence of other species. "
				+ "The study of the spatial distribution of organisms, species and ecosystems, is the science of biogeography.";
		String s2 = "Terrestrial biodiversity is thought to be up to 25 times greater than ocean biodiversity. "
				+ "A recently discovered method put the total number of species on Earth at 8.7 million, of which 2.1 million were estimated to live in the ocean. "
				+ "However, this estimate seems to under-represent the diversity of microorganisms.";
		String s3 = "The penultimate process is called conching. A conche is a container filled with metal beads, which act as grinders. "
				+ "The refined and blended chocolate mass is kept in a liquid state by frictional heat. Chocolate prior to conching has an uneven and gritty texture. "
				+ "The conching process produces cocoa and sugar particles smaller than the tongue can detect, hence the smooth feel in the mouth. "
				+ "The length of the conching process determines the final smoothness and quality of the chocolate. High-quality chocolate is conched for about 72 hours, and lesser grades about four to six hours. "
				+ "After the process is complete, the chocolate mass is stored in tanks heated to about 45 to 50 °C (113 to 122 °F) until final processing";
		SimilarityCalculator sc = new SimilarityCalculator();
		System.out.println("Jiang");
		ILexicalDatabase db = new NictWordNet();
		sc.calculateWordnetSimilarity(db, s1, s2, "ji");
		sc.calculateWordnetSimilarity(db, s1, s3, "ji");
		sc.calculateWordnetSimilarity(db, s3, s2, "ji");
		
		System.out.println("Path");
		sc.calculateWordnetSimilarity(db, s1, s2, "pat");
		sc.calculateWordnetSimilarity(db, s1, s3, "pat");
		sc.calculateWordnetSimilarity(db, s3, s2, "pat");
		
		System.out.println("Wu");
		sc.calculateWordnetSimilarity(db, s1, s2, "wu");
		sc.calculateWordnetSimilarity(db, s1, s3, "wu");
		sc.calculateWordnetSimilarity(db, s3, s2, "wu");
		
		System.out.println("Lin");
		sc.calculateWordnetSimilarity(db, s1, s2, "lin");
		sc.calculateWordnetSimilarity(db, s1, s3, "lin");
		sc.calculateWordnetSimilarity(db, s3, s2, "lin");
	}

}
