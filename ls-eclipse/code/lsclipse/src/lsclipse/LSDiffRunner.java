/* 
*    Logical Structural Diff (LSDiff)  
*    Copyright (C) <2015>  <Dr. Miryung Kim miryung@cs.ucla.edu>
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package lsclipse;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import lsclipse.LSDResult.Bookmark;
import lsclipse.dialogs.ProgressBarDialog;
import lsd.facts.LSDRuleEnumerator;
import lsd.facts.LSdiffFilter;
import lsd.facts.LSdiffHierarchialDeltaKB;
import lsd.rule.LSDBinding;
import lsd.rule.LSDFact;
import lsd.rule.LSDRule;
import metapackage.MetaInfo;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;

import utils.IO;
import utils.WS;
import changetypes.ASTVisitorAtomicChange;
import changetypes.AtomicChange;
import changetypes.ChangeSet;
import changetypes.Fact;
import changetypes.FactBase;
import changetypes.AtomicChange.Modifier;
//import edu.washington.cs.induction.FileNameService;
//import edu.washington.cs.induction.OnePipeLineScript;

public class LSDiffRunner {

	private static ProgressBarDialog progbar;

	private static String resultStringIncFE;
	private static String resultStringGranularity;
	private static String resultStringNHops;

	//Define some projects
	/*
	private static String[] allprojs = { "carol", "dnsjava", "jfreechart" };
	private static String[][] allversions = 
			{ { "1.1.0", "1.2.0", "1.3.0", "1.4.0", "1.5.1", "2.0.0" },					//carol
			{ "2.0.2", "2.0.3", "2.0.4", "2.0.5", "2.0.6", "2.0.7", "2.0.8" },			//dnsjava
			{ "1.0.7", "1.0.8", "1.0.9", "1.0.10", "1.0.11", "1.0.12", "1.0.13" } };	//jfreechart 
	*/
/*
	private static String[] allprojs = { "dnsjava"};
	private static String[][] allversions = 
	{ { "2.0.2", "2.0.3", "2.0.4", "2.0.5" } };
	private static final int max_step = 1;
	private static final int default_granularity = LSdiffHierarchialDeltaKB.METHOD_LEVEL;
	private static final int default_nHop = 1;

	public static String batchLSDiffIncFE(ProgressBarDialog progbar) {	//Spam all projects! for changing granularity

		boolean incremental = true;

		IO.writeToFile("Incremental FE\n", "C:\\incfe.txt");
		String results = "";
		for (int step=1; step<=max_step; ++step) {
			IO.appendToFile("Step "+step+": \n", "C:\\incfe.txt");
			results+="Step "+step+": \n";
			for (int i=0; i<allprojs.length; ++i) {
				String proj = allprojs[i];
				IO.appendToFile(proj+"\n", "C:\\incfe.txt");
				results+=proj+"\n";
				String[] versions = allversions[i];
				for (int j=0; j<versions.length-step; j++) {
					String proj1 = proj+"-"+versions[j];
					String proj2 = proj+"-"+versions[j+step];
					Set<String> changedFiles = new HashSet<String>();
					try {
						changedFiles = WS.findChangedFilesByDiff(proj1, proj2);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					IO.appendToFile(","+versions[j]+","+versions[j+step]+",", "C:\\incfe.txt");
					results+=","+versions[j]+","+versions[j+step]+",";

					doLSDiff(proj1, proj2, changedFiles, progbar, false, default_granularity, default_nHop, true);
					System.out.println(resultStringIncFE);
					IO.appendToFile(resultStringIncFE+",", "C:\\incfe.txt");
					results+=resultStringIncFE+",";

					doLSDiff(proj1, proj2, changedFiles, progbar, true, default_granularity, default_nHop, true);
					System.out.println(resultStringIncFE);
					IO.appendToFile(resultStringIncFE+"\n", "C:\\incfe.txt");
					results+=resultStringIncFE+"\n";
				}
			}
		}
		System.out.println("\n"+results);
		return results;
	}
	public static String batchLSDiffGranularity(ProgressBarDialog progbar) {	//Spam all projects! for changing granularity

		boolean incremental = true;

		IO.writeToFile("Granularity\n", "C:\\granu.txt");
		String results = "";
		for (int step=1; step<=max_step; ++step) {
			IO.appendToFile("Step "+step+": \n", "C:\\granu.txt");
			results+="Step "+step+": \n";
			for (int i=0; i<allprojs.length; ++i) {
				String proj = allprojs[i];
				IO.appendToFile(proj+"\n", "C:\\granu.txt");
				results+=proj+"\n";
				String[] versions = allversions[i];
				for (int j=0; j<versions.length-step; j++) {
					String proj1 = proj+"-"+versions[j];
					String proj2 = proj+"-"+versions[j+step];
					Set<String> changedFiles = new HashSet<String>();
					try {
						changedFiles = WS.findChangedFilesByDiff(proj1, proj2);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					doLSDiff(proj1, proj2, changedFiles, progbar, incremental, LSdiffHierarchialDeltaKB.BODY_LEVEL, default_nHop, true);
					System.out.println(resultStringGranularity);
					IO.appendToFile(","+versions[j]+","+versions[j+step]+",", "C:\\granu.txt");
					results+=","+versions[j]+","+versions[j+step]+",";
					IO.appendToFile(resultStringGranularity+"\n", "C:\\granu.txt");
					results+=resultStringGranularity;
					results+="\n";
				}
			}
		}
		System.out.println("\n"+results);
		return results;
	}
	public static String batchLSDiffNHops(ProgressBarDialog progbar) {	//Spam all projects! for changing granularity
		
		boolean incremental = true;

		IO.writeToFile("nHops\n", "C:\\nhop.txt");
		String results = "";
		for (int step=1; step<=max_step; ++step) {
			IO.appendToFile("Step "+step+": \n", "C:\\nhop.txt");
			results+="Step "+step+": \n";
			for (int i=0; i<allprojs.length; ++i) {
				String proj = allprojs[i];
				IO.appendToFile(proj+"\n", "C:\\nhop.txt");
				results+=proj+"\n";
				String[] versions = allversions[i];
				for (int j=0; j<versions.length-step; j++) {
					String proj1 = proj+"-"+versions[j];
					String proj2 = proj+"-"+versions[j+step];
					Set<String> changedFiles = new HashSet<String>();
					try {
						changedFiles = WS.findChangedFilesByDiff(proj1, proj2);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					IO.appendToFile(","+versions[j]+","+versions[j+step]+",", "C:\\nhop.txt");
					results+=","+versions[j]+","+versions[j+step]+",";
					for (int nhop=0; nhop<=3; ++nhop) {
						doLSDiff(proj1, proj2, changedFiles, progbar, incremental, default_granularity, nhop, true);
						System.out.println(resultStringNHops);
						IO.appendToFile(resultStringNHops, "C:\\nhop.txt");
						results+=resultStringNHops;
					}
					IO.appendToFile("\n", "C:\\nhop.txt");
					results+="\n";
				}
			}
		}
		System.out.println("\n"+results);
		return results;
	}
*/
	public static List<LSDResult> doLSDiff(String proj1, String proj2, Set<String> changedFiles, double accuracy, int minConcFact, int k, int beamSize, ProgressBarDialog progbar) {
		return doLSDiff(proj1, proj2, changedFiles, progbar, accuracy, minConcFact, k, beamSize, true, LSdiffHierarchialDeltaKB.METHOD_LEVEL, 1, false);
	}

	private static List<LSDResult> doLSDiff(String proj1, String proj2,
			Set<String> changedFiles, ProgressBarDialog progbar,
			double accuracy, int minConcFact, int k, int beamSize, 
			boolean incremental,
			int level,
			int nHop,
			boolean batchmode) {

		LSDiffRunner.progbar = progbar;

		long starttime = System.currentTimeMillis();

		progbar.setStep(0);
		progbar.setMessage("Computing common files... \n");
		//Identify common files
		Set<String> allProj1Files = WS.getFiles(proj1);
		Set<String> allProj2Files = WS.getFiles(proj2);
		Set<String> commonFiles = new HashSet<String>();
		commonFiles.addAll(allProj1Files);
		commonFiles.removeAll(changedFiles);
		Set<String> changedProj1Files = new HashSet<String>(allProj1Files);
		changedProj1Files.removeAll(commonFiles);
		Set<String> changedProj2Files = new HashSet<String>(allProj2Files);
		changedProj2Files.removeAll(commonFiles);

		long initendtime = System.currentTimeMillis();

		//Extraction for FB1
		progbar.setStep(1);
		progbar.setMessage("Retrieving facts for FB1... \n");
		FactBase commonfb = makeFactBase(proj1, commonFiles);
		FactBase fb1 = new FactBase();
		fb1.addAll(commonfb);
		FactBase changedProj1FB = makeFactBase(proj1, changedProj1Files);
		changedProj1FB.deriveDefaultConstructors();
		fb1.addAll(changedProj1FB);
		int numFacts1 = fb1.size();
		progbar.appendLog("Extraction OK! Extracted "+numFacts1+" facts for FB1\n");
		//Derived facts
		progbar.setMessage("Adding derived facts for FB1... \n");
		long fb1derivestarttime = System.currentTimeMillis();
		fb1.deriveDefaultConstructors();
		fb1.deriveInheritedMembers();
		long fb1deriveendtime = System.currentTimeMillis();
		progbar.appendLog("Derivation OK! Added "+(fb1.size()-numFacts1)+" facts to FB1\n");
		progbar.appendLog("All done! FB1 contains "+fb1.size()+" facts\n");

		long fb1endtime = System.currentTimeMillis();
		long fb2derivestarttime = fb1endtime;
		long fb2deriveendtime = fb1endtime;
		long fb2endtime = fb1endtime;
		int numFacts2;

		ChangeSet cs = null;
		FactBase fb2 = null;

		//Extraction for FB2 and changeset
		if (!incremental) {
			progbar.setStep(2);
			progbar.setMessage("Retrieving facts for FB2... ");
			fb2 = makeFactBase(proj2, allProj2Files);
			numFacts2 = fb2.size();
			progbar.appendLog("Extraction OK! Extracted "+numFacts2+" facts for FB2\n");
			fb2endtime = System.currentTimeMillis();

			//Derived facts
			progbar.setMessage("Adding derived facts for FB2... \n");
			fb2derivestarttime = System.currentTimeMillis();
			fb2.deriveDefaultConstructors();
			fb2.deriveInheritedMembers();
			fb2deriveendtime = System.currentTimeMillis();
			progbar.appendLog("Derivation OK! Added "+(fb2.size()-numFacts2)+" facts to FB2\n");
			progbar.appendLog("All done! FB2 contains "+fb2.size()+" facts\n");

			//compute diff
			progbar.setStep(3);
			progbar.setMessage("Computing factbase differences... ");
			cs = fb2.diff(fb1);
			progbar.appendLog("All done! "+cs.size()+" changes found\n");
		} else {
			progbar.setStep(2);
			progbar.setMessage("Retrieving facts for FB2... ");
			FactBase changedProj2FB = makeFactBase(proj2, changedProj2Files);
			fb2 = changedProj2FB;
			numFacts2 = changedProj2FB.size();
			fb2.deriveDefaultConstructors();
			progbar.appendLog("Extraction OK! Extracted "+numFacts2+" facts for FB2\n");
			fb2endtime = System.currentTimeMillis();

			//add added files become ADDED_*, deleted files become delete_* and modified files must do set diff
			progbar.setStep(3);
			progbar.setMessage("Computing factbase differences... ");
			cs = changedProj2FB.diff(changedProj1FB);
			progbar.appendLog("All done! "+cs.size()+" changes found\n");

			//now handle inheritance facts using Tyruba
			progbar.setMessage("Adding derived facts for FB2... \n");
			fb2derivestarttime = System.currentTimeMillis();
			cs.deriveInheritedMembers(fb1, fb2);
			fb2deriveendtime = System.currentTimeMillis();
			progbar.appendLog("Derivation OK! Added "+(fb2.size()-numFacts2)+" facts to FB2\n");
			progbar.appendLog("All done! FB2 contains "+fb2.size()+" facts\n");
		}

		long diffendtime = System.currentTimeMillis();
		
		progbar.setStep(4);
		//Convert into LSDFact/Rule format
		progbar.setMessage("Preparing to run LSDiff...\n");
		progbar.appendLog("Converting atomic change to LSDiff changes... ");
		ArrayList<LSDFact> input2kbFacts = new ArrayList<LSDFact>();
		ArrayList<LSDFact> inputDeltaFacts = new ArrayList<LSDFact>();
		//ArrayList<LSDRule> inputWinnowingRules = new ArrayList<LSDRule>();
		for (Fact f : fb1) {
			input2kbFacts.add(LSDWrapper.fact2LSDFact(f, Modifier.BEFORE));
		}
		for (Fact f : fb2) {
			input2kbFacts.add(LSDWrapper.fact2LSDFact(f, Modifier.AFTER));
		}
		for (AtomicChange ac : cs) {
			inputDeltaFacts.add(LSDWrapper.atomicChange2LSDFact(ac));
		}

		progbar.appendLog("OK\n");
//*
		//write to LSDiff input file
		progbar.appendLog("Writing to LSDiff input files... \n");
		BufferedWriter lsd2kbfile = null;
		try {
			//First check if path exists -- if not, create it
			File f2KBfile = new File(MetaInfo.lsclipse2KB);
			File dir = f2KBfile.getParentFile();
			if (!dir.exists()) {
				dir.mkdirs();
			}
			//write away!
			progbar.appendLog("  Writing 2KB to "+MetaInfo.lsclipse2KB+"\n");
			lsd2kbfile = new BufferedWriter(new FileWriter(MetaInfo.lsclipse2KB));
			lsd2kbfile.write("#include \"2KB_lsdPred.rub\"\n");
			for (LSDFact f : input2kbFacts) {
				lsd2kbfile.append(f.toString()+".\n");
			}
			lsd2kbfile.close();
		} catch (IOException e) {
			progbar.appendError("Unable to create 2KB input file! Exiting...");
			progbar.dispose();
			return null;
		}
		BufferedWriter lsddeltafile = null;
		try {
			progbar.appendLog("  Writing deltas to "+MetaInfo.lsclipseDelta+"\n");
			lsddeltafile = new BufferedWriter(new FileWriter(MetaInfo.lsclipseDelta));
			lsddeltafile.write("#include \"deltaKB_lsdPred.rub\"\n");
			lsddeltafile.close();
			for (LSDFact f : inputDeltaFacts) {
				lsddeltafile = new BufferedWriter(new FileWriter(MetaInfo.lsclipseDelta, true));
				lsddeltafile.append(f.toString()+".\n");
				lsddeltafile.close();
			}
		} catch (IOException e) {
			progbar.appendError("Unable to create delta KB input file! Exiting...");
			progbar.dispose();
			return null;
		}
		progbar.appendLog("OK\n");

		long lsdiffprependtime = System.currentTimeMillis();

//*/
		//somehow invoke LSDdiff
		progbar.setMessage("Invoking LSDiff (this may take a while)... ");
		BufferedWriter output = null;
		LSDRuleEnumerator enumerator = null;
		List<LSDRule> rules = null;
		int[][] perfResults = new int[5][6];
		try {
			installLSDiff();	//install files if necessary

			File winnowingRulesFile = new File(MetaInfo.winnowings);
			File typeLevelWinnowingRulesFile = new File(MetaInfo.modifiedWinnowings);
			File resultsFile = new File(MetaInfo.resultsFile);
			File twoKBFile = new File(MetaInfo.lsclipse2KB);
			File deltaKBFile = new File(MetaInfo.lsclipseDelta);
			enumerator = new LSDRuleEnumerator(twoKBFile, deltaKBFile, winnowingRulesFile, resultsFile,
					minConcFact, accuracy, k, beamSize, MetaInfo.maxException,
					typeLevelWinnowingRulesFile, output);
			//MK 1. this is the call that Alex used 
		//	rules = enumerator.levelIncrementLearning(System.out, level, nHop, perfResults);
			//MK 2. this is the original call in the paper 

			rules = enumerator.bruteForceLearning(nHop, new LSdiffFilter(true, true, true, true, true));
		} catch (Exception e) {
			progbar.appendError("Unable to do LSDiff analysis");
			progbar.dispose();
			return null;
		}
		if (rules==null) {
			progbar.appendError("Unable to derive any rules!");
			progbar.dispose();
			return null;
		}
		progbar.appendLog("OK\n");
		progbar.appendLog("Found "+rules.size()+" rules\n");
		
		long lsdiffendtime = System.currentTimeMillis();

		//return results
		List<LSDResult> res = new ArrayList<LSDResult>();
		for (LSDRule r : rules) {
			LSDResult result = new LSDResult();
			result.num_matches = enumerator.countMatches(r);
			result.num_counter = enumerator.countExceptions(r);
			result.rule = r;
			// MK 020611: This is a place to update 
//			result.desc = r.toString();
			result.desc = enumerator.getConcreteRule(r); 
			result.examples = enumerator.getRelevantFacts(r);
			result.exceptions = enumerator.getExceptions(r);
			
			//compute line number info for each example and exception
			result.examplesBookmarks = new ArrayList<LSDResult.Bookmark>();
			for (LSDFact f : result.examples) {
				FactBase facts = getReferencedFacts(f, fb1, fb2);
				if (facts==null || facts.size()==0) {
					result.examplesBookmarks.add(null);
				} else {
					Fact fact = facts.iterator().next();
					Modifier mod = LSDWrapper.LSDPred2Modifier(f.getPredicate());
					//select FB based on modifier
					boolean oldFB;
					if (mod.equals(Modifier.ADDED)) {
						oldFB = false;
					} else if (mod.equals(Modifier.DELETED) || mod.equals(Modifier.MODIFIED)) {
						oldFB = true;
					} else {
						oldFB = true;
					}
					result.examplesBookmarks.add(
							new Bookmark(fact.filename, fact.startposition, fact.length, oldFB));
				}
			}

			res.add(result);
		}

		progbar.setStep(5);
		progbar.setMessage("Cleaning up... ");
		progbar.appendLog("OK\n");

		long cleanupendtime = System.currentTimeMillis();

		//print out some timings
		long initduration = initendtime-starttime; 
		long changexduration = diffendtime-initendtime;
		long fb1duration = fb1endtime - initendtime;
		long fb1deriveduration = fb1deriveendtime - fb1derivestarttime;
		long fb2duration = fb2endtime - fb1endtime;
		long fb2deriveduration = fb2deriveendtime - fb2derivestarttime;
		long diffduration = diffendtime - fb2endtime;
		long lsdiffduration = lsdiffendtime - diffendtime;
		long lsprepduration = lsdiffprependtime - diffendtime;
		long lsdduration = lsdiffendtime - lsdiffprependtime;
		long cleanupduration = cleanupendtime - lsdiffendtime;
		long totalduration = cleanupendtime - starttime;
		progbar.appendLog("Time to initialize = "+initduration/1000+"s ("+initduration*100/totalduration+"%)\n");
		progbar.appendLog("Time to run change extractor = "+changexduration/1000+"s ("+changexduration*100/totalduration+"%)\n");
		progbar.appendLog("  FB1: "+fb1duration/1000+"s ("+fb1duration*100/changexduration+"%) (Extracted: "+(fb1duration-fb1deriveduration)/1000+" Derived: "+fb1deriveduration/1000+"s)\n");
		progbar.appendLog("       Found "+fb1.size()+" facts ("+(fb1.size()-numFacts1)+" derived) from "+allProj1Files.size()+" files\n");
		progbar.appendLog("  FB2: "+fb2duration/1000+"s ("+fb2duration*100/changexduration+"%) (Extracted: "+(incremental?fb2duration:fb2duration-fb2deriveduration)/1000+" Derived: "+fb2deriveduration/1000+"s)\n");
		progbar.appendLog("       Found "+fb2.size()+" facts ("+(fb2.size()-numFacts2)+" derived) from "+(incremental?changedProj2Files.size():allProj2Files.size())+" files\n");
		progbar.appendLog("  FB differencing: "+diffduration/1000+"s ("+diffduration*100/changexduration+"%)\n");
		progbar.appendLog("       Found "+cs.size()+" deltas\n");
		progbar.appendLog("Time to run LSDiff = "+lsdiffduration/1000+"s ("+lsdiffduration*100/totalduration+"%)\n");
		progbar.appendLog("  Prep time: "+lsprepduration/1000+"s ("+lsprepduration*100/lsdiffduration+"%)\n");
		progbar.appendLog("  Rule generation: "+lsdduration/1000+"s ("+lsdduration*100/lsdiffduration+"%)\n");
		progbar.appendLog("       Found "+rules.size()+" rules\n");
		progbar.appendLog("Time to clean up = "+cleanupduration/1000+"s ("+cleanupduration*100/totalduration+"%)\n");
		progbar.appendLog("Total time elapsed = "+totalduration/1000+"s\n");

		//set resultString (Inc FE)
//*
		resultStringIncFE = allProj1Files.size() + "," + 
						(fb1duration-fb1deriveduration)/1000.0 + "," +
						fb1deriveduration/1000.0 + "," +
						fb1duration/1000.0 + "," + 
						fb1.size() + "," +
						(fb1.size()-numFacts1) + "," + 
						(incremental?changedProj2Files.size():allProj2Files.size()) + "," + 
						(fb2duration-fb2deriveduration)/1000.0 + "," +
						fb2deriveduration/1000.0 + "," +
						fb2duration/1000.0 + "," + 
						(fb2.size()-numFacts2) + "," +
						cs.size() + "," +
						rules.size() + "," +
						totalduration/1000.0;
//*/
//*
		//granularity
		resultStringGranularity = 
		perfResults[0][LSdiffHierarchialDeltaKB.TYPE_LEVEL] + "," + 
		perfResults[1][LSdiffHierarchialDeltaKB.TYPE_LEVEL] + "," +
		perfResults[2][LSdiffHierarchialDeltaKB.TYPE_LEVEL] + "," +
		perfResults[3][LSdiffHierarchialDeltaKB.TYPE_LEVEL] + "," +
		perfResults[4][LSdiffHierarchialDeltaKB.TYPE_LEVEL] + "," +
		perfResults[0][LSdiffHierarchialDeltaKB.FIELD_LEVEL] + "," +
		perfResults[1][LSdiffHierarchialDeltaKB.FIELD_LEVEL] + "," +
		perfResults[2][LSdiffHierarchialDeltaKB.FIELD_LEVEL] + "," +
		perfResults[3][LSdiffHierarchialDeltaKB.FIELD_LEVEL] + "," +
		perfResults[4][LSdiffHierarchialDeltaKB.FIELD_LEVEL] + "," +
		perfResults[0][LSdiffHierarchialDeltaKB.BODY_LEVEL] + "," +
		perfResults[1][LSdiffHierarchialDeltaKB.BODY_LEVEL] + "," +
		perfResults[2][LSdiffHierarchialDeltaKB.BODY_LEVEL] + "," +
		perfResults[3][LSdiffHierarchialDeltaKB.BODY_LEVEL] + "," +
		perfResults[4][LSdiffHierarchialDeltaKB.BODY_LEVEL] + "," +
		fb1.size() + "," +
		fb2.size() + "," +
		cs.size() + "," +
		rules.size() + "," +
		(totalduration/1000.0) + "," +
		(lsdduration/1000.0);

//*/
//*
		//nHops
		resultStringNHops =  
		perfResults[0][LSdiffHierarchialDeltaKB.METHOD_LEVEL] + "," +
		perfResults[1][LSdiffHierarchialDeltaKB.METHOD_LEVEL] + "," +
		perfResults[2][LSdiffHierarchialDeltaKB.METHOD_LEVEL] + "," +
		perfResults[3][LSdiffHierarchialDeltaKB.METHOD_LEVEL] + "," +
		perfResults[4][LSdiffHierarchialDeltaKB.METHOD_LEVEL] + "," +
		fb1.size() + "," +
		fb2.size() + "," +
		cs.size() + "," +
		rules.size() + "," +
		(totalduration/1000.0) + "," +
		(lsdduration/1000.0);
//*/
		//dispose the progress bar
		if (!batchmode) {
			progbar.dispose();
		}

		return res;
	}

	private static String chooseTempProjName(String baseprojname) {
		return "aaa5";
	}

	private static void copyChangedFile(String path, IProject srcproj, IProject destProj) {
		IFile srcFile = srcproj.getFile(path);
		File src = new File(srcFile.getLocation().toOSString());
		IFile destFile = destProj.getFile(path);
		File dest = new File(destFile.getLocation().toOSString());
		try {
			//If file exists in new proj, copy it. If does not exist then overwrite
			if (src.exists()) {
				IO.copyFile(src, dest);
			} else {
				dest.delete();
			}
			destFile.refreshLocal(1, new NullProgressMonitor());
		} catch (IOException e) {
			progbar.appendError("Error copying file "+path+". Ignoring...\n");
		} catch (CoreException e) {
			progbar.appendError("Error refreshing after copy "+path+". Ignoring...\n");
		}
	}

	//Current not working. Fix me if need to be used 
	private static FactBase makeFBIncrementalFromWorkspace(String baseproject, Set<String> files, String newproject, FactBase commonfb, FactBase oldfb) {

		IProject baseproj = ResourcesPlugin.getWorkspace().getRoot().getProject(baseproject);
		IProject newproj = ResourcesPlugin.getWorkspace().getRoot().getProject(newproject);

		//make a hidden clone of the base project
		String workname = chooseTempProjName(baseproject);
		IPath projpath = new Path(workname);
		try {
			baseproj.copy(projpath, true, new NullProgressMonitor());
		} catch (CoreException e) {
			progbar.appendError("Unable to copy project "+baseproject+"!\n");
			return null;
		}
		IProject workproj = ResourcesPlugin.getWorkspace().getRoot().getProject(workname);

		//copy files from newproject
		for (String path : files) {
			copyChangedFile(path, newproj, workproj);
		}

		//extract facts from files
		FactBase changesfb = makeFactBase(workname, files);

		//cleanup
		try {
			workproj.close(new NullProgressMonitor());
			workproj.delete(true, true, new NullProgressMonitor());
		} catch (CoreException e) {
			progbar.appendError("Unable to remove temp project "+workproj+"!\n");
//			return null;
		}

		return null;
	}

	private static FactBase makeFactBase(String project, Set<String> files) {

		IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject(project);

		FactBase fb1 = new FactBase();
		FactBase fb;
		for (String filename : files) {
			progbar.appendLog("  Now working on "+filename+" ... ");
			//collect facts for old revision
			fb = getFactsFromFile(filename, proj);
			fb1.addAll(fb);
			progbar.appendLog("OK\n");
		}

		return fb1;
	}
	private static FactBase getReferencedFacts(LSDFact f, FactBase oldFB, FactBase newFB) {

		//get type and modifier from predicate
		AtomicChange ac = LSDWrapper.LSDFact2AtomicChange(f); 

		//select FB based on modifier
		FactBase fb;
		if (ac.mod==Modifier.ADDED) {
			fb = newFB;
		} else if (ac.mod==Modifier.DELETED || ac.mod==Modifier.MODIFIED) {
			fb = oldFB;
		} else {
			fb = oldFB;
		}

		//make params string
		Vector<String> params = new Vector<String>();
		List<LSDBinding> bindings = f.getBindings();
		for (LSDBinding binding : bindings) {
			String bindstr = binding.toString();
			//strip enclosing quotes
			if (bindstr.charAt(0)=='"') bindstr=bindstr.substring(1);
			if (bindstr.charAt(bindstr.length()-1)=='"') bindstr=bindstr.substring(0,bindstr.length()-1);
			params.add(bindstr);
		}

		//find relevant fact in FB
		FactBase res = fb.findFact(ac.fact.type, params);

		return res;
	}
	//if LSDiff is not installed, install the necessary files into metainfo directories 
	private static void installLSDiff() {	
		//first check if folders: input, output, and fdb are present
		File srcfile = new File(MetaInfo.srcDir);
		srcfile.mkdirs();
		File resfile = new File(MetaInfo.resDir);
		resfile.mkdirs();
		File fdbfile = new File(MetaInfo.fdbDir);
		fdbfile.mkdirs();
		//if 2KB_lsdPred not installed, install it now
		File included2KBFile = MetaInfo.included2kb;
		if (!included2KBFile.exists()) {
			InputStream is = lsclipse.LSclipse.getDefault().getClass().getResourceAsStream("/lib/"+included2KBFile.getName());
			IO.writeStreamToFile(is, included2KBFile);
		}
		//if deltaKB_lsdPred not installed, install it now
		File includedDeltaKBFile = MetaInfo.includedDelta;
		if (!includedDeltaKBFile.exists()) {
			InputStream is = lsclipse.LSclipse.getDefault().getClass().getResourceAsStream("/lib/"+includedDeltaKBFile.getName());
			IO.writeStreamToFile(is, includedDeltaKBFile);
		}
		//if winnowingrules not installed, install it now
		File winnowingRulesFile = new File(MetaInfo.winnowings);
		if (!winnowingRulesFile.exists()) {
			InputStream is = lsclipse.LSclipse.getDefault().getClass().getResourceAsStream("/lib/"+winnowingRulesFile.getName());
			IO.writeStreamToFile(is, winnowingRulesFile);
		}
		//if newwinnowingrules not installed, install it now
		File typeLevelWinnowingRulesFile = new File(MetaInfo.modifiedWinnowings);
		if (!typeLevelWinnowingRulesFile.exists()) {
			InputStream is = lsclipse.LSclipse.getDefault().getClass().getResourceAsStream("/lib/"+typeLevelWinnowingRulesFile.getName());
			IO.writeStreamToFile(is, typeLevelWinnowingRulesFile);
		}
		
		//if include1KBFile not installed, install it now
		File include1KBFile = MetaInfo.included1kb;
		if (!include1KBFile.exists()) {
			InputStream is = lsclipse.LSclipse.getDefault().getClass().getResourceAsStream("/lib/"+include1KBFile.getName());
			IO.writeStreamToFile(is, include1KBFile);
		}
		//if derivationRules not installed, install it now
		File derivationRules = MetaInfo.derivationRules;
		if (!derivationRules.exists()) {
			InputStream is = lsclipse.LSclipse.getDefault().getClass().getResourceAsStream("/lib/"+derivationRules.getName());
			IO.writeStreamToFile(is, include1KBFile);
		}
		//if deltaDerivationRules not installed, install it now
		File deltaDerivationRules = MetaInfo.deltaDerivationRules;
		if (!deltaDerivationRules.exists()) {
			InputStream is = lsclipse.LSclipse.getDefault().getClass().getResourceAsStream("/lib/"+deltaDerivationRules.getName());
			IO.writeStreamToFile(is, deltaDerivationRules);
		}
	}

	private static FactBase getFactsFromFile(String pathname, IProject proj) {
		try {
			//get project and source file from ws
			IJavaProject javaproj = JavaCore.create(proj);

			IFile file = proj.getFile(pathname);
			String source = IO.readStreamToString(file.getContents());

			//do some parsing
	        ASTParser parser = ASTParser.newParser(AST.JLS3);
	        parser.setProject(javaproj);
	        parser.setResolveBindings(true);
	        parser.setSource(source.toCharArray());
	        parser.setUnitName(pathname);

	        ASTNode ast = parser.createAST(new NullProgressMonitor());
	        ASTVisitorAtomicChange acvisitor = new ASTVisitorAtomicChange(pathname);
	        ast.accept(acvisitor);

	        return acvisitor.facts;
		} catch (Exception e){
			System.err.println("Error parsing "+pathname+" in project "+proj.getName());
			return new FactBase();
		}
	}
	/*
	public static void inferAPIMatchingRuleAsNeeded(IProject oldProj, IProject newProj, String versions[], boolean checkIn, boolean overwrite) { 
		int increment = 1; 
		if (checkIn) { 
			increment = 2; 
		}
		for (int i=0; i< versions.length-1; i= i+ increment){
			String oldVersion = versions[i];
			String newVersion = versions[i+1];
			
			File oldSourceDir = new File(oldProj.getFullPath().toOSString());
			File newSourceDir = new File(newProj.getFullPath().toOSString());

			if (!oldSourceDir.exists()) {
				System.out.println(oldSourceDir.getAbsolutePath()
						+ " Doesn't Exist");
			}
			if (!newSourceDir.exists()) {
				System.out.println(newSourceDir.getAbsolutePath()
						+ " Doesn't Exist");
			}
			File matchingFile = FileNameService.getMatchingXMLFile(newProj.getName(),
						oldVersion, newVersion, 0.7, 0.34);
			if (matchingFile.exists() || overwrite==false) continue;
			OnePipeLineScript.getMatchingForLSD(newProj.getName(), oldVersion, oldSourceDir, newVersion, newSourceDir);
		}
	}
	public static boolean checkMatchingRuleFiles(String projectName, String [] versions, boolean checkIn) {
		boolean allExist = true;
		int increment = 1; 
		if (checkIn) { 
			increment = 2; 
		}
		for (int i=0; i< versions.length-1; i= i+ increment){
			String oldVersion = versions[i];
			String newVersion = versions[i+1];

			File matchingFile = FileNameService.getMatchingXMLFile(projectName,
					oldVersion, newVersion, 0.7, 0.34);
			if (!matchingFile.exists()) { 
				allExist= false;

				System.out.println(matchingFile.getAbsolutePath()+" Doesn't Exist");
			}
		}
		return allExist;
	}
//*/
}
