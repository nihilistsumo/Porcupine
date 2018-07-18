package com.trema.pcpn.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.lucene.queryparser.classic.ParseException;

import com.trema.pcpn.aspect.AspectSimilarity;
import com.trema.pcpn.aspect.ParasimAspectRlib;
import com.trema.pcpn.aspect.ParasimAspectSimJob;
import com.trema.pcpn.parasimutil.CombineRunFilesToRLibFetFile;
import com.trema.pcpn.parasimutil.CombineRunFilesUsingRlibModel;
import com.trema.pcpn.parasimutil.ParasimFetFileWriter;
import com.trema.pcpn.parasimutil.ParasimFetFileWriterShort;
import com.trema.prcpn.similarity.ParaSimRanker;
import com.trema.prcpn.similarity.ParaSimRankerQueryParatext;
import com.trema.prcpn.similarity.ParaSimRankerRand;
import com.trema.prcpn.similarity.ParaSimSanityCheck;


public class PorcupineRunner {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			Properties prop = new Properties();
			prop.load(new FileInputStream(new File("project.properties")));
			PorcupineHelper ph = new PorcupineHelper();
			String cmd = prop.getProperty("mode");
			boolean truePage = false;
			/*
			if(cmd.charAt(0)=='t')
				truePage = true;
			if(cmd.endsWith("wvhac")){
				ph.runHACClustering(prop, truePage, "w2v");
			}
			else if(cmd.endsWith("tmhac")){
				ph.runHACClustering(prop, truePage, "tm");
			}
			else if(cmd.endsWith("tfhac")){
				ph.runHACClustering(prop, truePage, "tfidf");
			}
			else if(cmd.endsWith("wvkm")){
				ph.runKMeansClustering(prop, truePage, "w2v");
			}
			else if(cmd.endsWith("tmkm")){
				ph.runKMeansClustering(prop, truePage, "tm");
			}
			else if(cmd.endsWith("tfkm")){
				ph.runKMeansClustering(prop, truePage, "tfidf");
			}
			else if(cmd.endsWith("rand")){
				ph.runRandomClustering(prop, truePage);
			}
			else if(cmd.equalsIgnoreCase("cltxt")){
				ph.convertClusterDataToText(prop);
			}
			else if(cmd.equalsIgnoreCase("mc")){
				ph.runClusteringMeasure(prop);
			}
			else if(cmd.equalsIgnoreCase("sim")){
				ph.runSimilarityRanker(prop, truePage, "wnwu");
			}
			*/
			if(args[0].equalsIgnoreCase("fet")) {
				ParasimFetFileWriterShort pfw = new ParasimFetFileWriterShort();
				String fetFileOut = args[1];
				String candRunFilePath = args[2];
				String parasimQrelsPath = args[3];
				boolean withTruePage;
				if(args[4].equalsIgnoreCase("true"))
					withTruePage = true;
				else
					withTruePage = false;
				pfw.writeFetFile(prop, fetFileOut, candRunFilePath, parasimQrelsPath, withTruePage);
			}
			//Index paragraphs
			// i index-directory-path paragraph-cbor-path with-entity?(entity/Entity/...)
			else if(args[0].equalsIgnoreCase("i")) {
				LuceneIndexer li = new LuceneIndexer();
				String indexOutPath = args[1];
				String paraCborPath = args[2];
				String withEnt = args[3];
				String removeSt = args[4];
				boolean withEntity = false;
				boolean removeStops = false;
				if(withEnt.startsWith("ent")||withEnt.startsWith("Ent")||withEnt.startsWith("ENT"))
					withEntity = true;
				if(removeSt.startsWith("stop")||removeSt.startsWith("STOP"))
					removeStops = true;
				li.indexParas(indexOutPath, paraCborPath, withEntity, removeStops);
			}
			else if(args[0].equalsIgnoreCase("r")) {
				ParaSimRanker psr = new ParaSimRanker();
				String method = args[1];
				String runFileOut = args[2];
				String candRunFilePath = args[3];
				boolean withTruePage = false;
				if(args[4].equalsIgnoreCase("true"))
					withTruePage = true;
				psr.rank(prop, method, runFileOut, candRunFilePath, withTruePage);
			}
			else if(args[0].equalsIgnoreCase("rpara")) {
				ParaSimRankerQueryParatext psrqp = new ParaSimRankerQueryParatext();
				String indexDir = args[1];
				String indexDirNoStops = args[2];
				String candRun = args[3];
				String articleQrelsPath = args[4];
				String outRun = args[5];
				String method = args[6];
				String withTruePagePara = args[7];
				psrqp.rank(indexDir, indexDirNoStops, candRun, articleQrelsPath, outRun, method, withTruePagePara);
			}
			else if(args[0].equalsIgnoreCase("rrand")) {
				ParaSimRankerRand psrr = new ParaSimRankerRand();
				String candRunFilePath = args[1];
				String articleQrelsPath = args[2];
				String runFileOut = args[3];
				String withTruePagePara = args[4];
				psrr.rank(candRunFilePath, articleQrelsPath, runFileOut, withTruePagePara);
			}
			else if(args[0].equalsIgnoreCase("san")) {
				ParaSimSanityCheck san = new ParaSimSanityCheck();
				String methods = args[1];
				String indexDirAsp = args[2];
				String indexDir = args[3];
				String indexDirNoStops = args[4];
				String topQrelsPath = args[5];
				String artQrelsPath = args[6];
				int keyNo = Integer.parseInt(args[7]);
				int retAspNo = Integer.parseInt(args[8]);
				String print = args[9];
				san.check(prop, methods, indexDirAsp, indexDir, indexDirNoStops, topQrelsPath, artQrelsPath, keyNo, retAspNo, print);
			}
			else if(args[0].equalsIgnoreCase("pw2v")) {
				ParaW2VConverter pw2v = new ParaW2VConverter();
				String paraFilePath = args[1];
				String ip = args[2];
				String db = args[3];
				String table = args[4];
				String user = args[5];
				String pwd = args[6];
				pw2v.convert(prop, paraFilePath, ip, db, table, user, pwd);
			}
			else if(args[0].equalsIgnoreCase("asp")) {
				AspectSimilarity asp = new AspectSimilarity();
				String option = args[1];
				String ip = args[2];
				String db = args[3];
				String table = args[4];
				String user = args[5];
				String pwd = args[6];
				String paraEntFile = args[7];
				if(option.equalsIgnoreCase("d")) {
					asp.insertEntitiesInDB(paraEntFile, DataUtilities.getDBConnection(ip, db, table, user, pwd));
				}
			}
			else if(args[0].equalsIgnoreCase("aspent")) {
				AspectEntityRetriever aspEntRet = new AspectEntityRetriever();
				String aspIndex = args[1];
				String index = args[2];
				aspEntRet.retrieveEntitiesFromAspect(prop, aspIndex, index);
			}
			else if(args[0].equalsIgnoreCase("cmb")) {
				CombineRunFilesToRLibFetFile cmb = new CombineRunFilesToRLibFetFile();
				String runfilesDir = args[1];
				String outputFetFilePath = args[2];
				String qrelsPath = args[3];
				cmb.writeFetFile(prop, runfilesDir, outputFetFilePath, qrelsPath);
			}
			else if(args[0].equalsIgnoreCase("split")) {
				TwoFoldRunfileSplit split = new TwoFoldRunfileSplit();
				String runfilesDir = args[1];
				String titlesPath = args[2];
				String outputDir1 = args[3];
				String outputDir2 = args[4];
				split.startProcess(runfilesDir, titlesPath, outputDir1, outputDir2);
			}
			else if(args[0].equalsIgnoreCase("cmbrun")){ 
				String runfilesDir = args[1];
				String rlibModelPath = args[2];
				String outputRunfilePath = args[3];
				int retParaInComb = Integer.parseInt(args[4]);
				CombineRunFilesUsingRlibModel cmbrun = new CombineRunFilesUsingRlibModel();
				cmbrun.writeRunFile(prop, runfilesDir, rlibModelPath, outputRunfilePath, retParaInComb);
			}
			else if(args[0].equalsIgnoreCase("asp2cv")) {
				String titlesPath = args[1];
				String candSetRunFilePath = args[2];
				String artQrelsPath = args[3];
				String paraSimQrelsPath = args[4];
				String fetFileOutputDir = args[5];
				String aspIsPath = args[6];
				String isPath = args[7];
				String isNoStopPath = args[8];
				String rlibPath = args[9];
				int retAspNo = Integer.parseInt(args[10]);
				String withTruePagePara = args[11];
				ParasimAspectRlib parasimAsp = new ParasimAspectRlib();
				parasimAsp.run2foldCV(prop, titlesPath, candSetRunFilePath, artQrelsPath, paraSimQrelsPath, fetFileOutputDir, aspIsPath, isPath, isNoStopPath, rlibPath, retAspNo, withTruePagePara);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
