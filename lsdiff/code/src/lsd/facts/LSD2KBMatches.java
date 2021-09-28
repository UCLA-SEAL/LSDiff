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
package lsd.facts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import lsd.io.*;
import lsd.rule.*;

import tyRuBa.engine.RuleBase;


public class LSD2KBMatches {
	PrintStream p;
	String outputDir;
	
	static {
		RuleBase.silent = true;
	}
	
	public LSD2KBMatches(PrintStream p, String outputDir) {
		this.p = p;
		this.outputDir = outputDir;
	}
	
	
	public static String matchesInRuleFile(File ruleFile, File twoKBFile, File deltaKBFile) {
		LSDTyrubaRuleChecker ruleChecker = null;
		try {
			ruleChecker = new LSDTyrubaRuleChecker();
			// Include all of the facts from the 2KB.
			ruleChecker.loadAdditionalDB(twoKBFile);
			ruleChecker.loadAdditionalDB(deltaKBFile);
		}
		catch (Throwable e) {
			e.printStackTrace();
			System.exit(-1);
		}
		Set<LSDFact> factsDeltaKB = new HashSet<LSDFact>();
		ArrayList<LSDFact> factList = new LSDTyrubaFactReader(
				deltaKBFile).getFacts();
		for (LSDFact f : factList) {
			factsDeltaKB.add((LSDFact) f);
		}
		Set<LSDFact> facts = new HashSet<LSDFact>();
		int numberOfRules = 0;
		int outsideReferences = 0;
		int outsideReferencingRules = 0;
		try {
			if (ruleFile.exists()) {
				BufferedReader in = new BufferedReader(
						new FileReader(ruleFile));
				String line = null;
				while ((line=in.readLine())!= null){ 

					if (line.trim().equals(""))
						continue;
					else if (line.trim().charAt(0) == '#') {
						continue;
					} else if (line.contains("=>")) {
//						Parse rule
						LSDRule rule = LSDAlchemyRuleReader.parseAlchemyRule(line);
						numberOfRules += 1;
						int factsThisRule = 0;
						for (LSDFact fact : ruleChecker.get2KBMatches(rule)) {
							if (factsDeltaKB.contains(fact.addedCopy()) ||
												factsDeltaKB.contains(fact.deletedCopy())) {
								continue;
							}
							factsThisRule += 1;
							facts.add(fact);
						}
						if (factsThisRule > 0) {
							outsideReferences += factsThisRule;
							outsideReferencingRules += 1;
						}
					}
				}
				in.close();
			}
			ruleChecker.shutdown();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return numberOfRules + "\t" + outsideReferencingRules + "\t" + outsideReferences + "\t" + facts.size();
	}
}
