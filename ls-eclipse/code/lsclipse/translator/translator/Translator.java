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
package translator;

import java.util.*;
import java.io.*;

public class Translator {
	File ruleFile;
	FileWriter writeFile;
	boolean willWriteToFile;

	public static void main(String[] args) {
		Translator t = new Translator("C:\\sample.rules");
		t.translate();
	}

	public Translator(String inReadFile, String inWriteFile) {
		this(inReadFile, inWriteFile, true);
	}

	public Translator(String inReadFile) {
		this(inReadFile, "", false);
	}

	private Translator(String inReadFile, String inWriteFile, boolean inWillWriteToFile) {
		ruleFile = new File(inReadFile);
		try {
			if (!inWriteFile.equals(""))
				writeFile = new FileWriter(inWriteFile);
		}
		catch (Exception e) {
		}
		willWriteToFile = inWillWriteToFile;
	}
	
	public void translate() {
		ArrayList<Rule> rules;
		Parser fileParser = new Parser(ruleFile);
		BufferedWriter fileWriter = null;
		
		if (willWriteToFile) {
			fileWriter = new BufferedWriter(writeFile);
		}
		
		try {
			rules = fileParser.parseRuleFile();
			
			for (int i = 0; i < rules.size(); i++) {
				String rule = printRule(rules.get(i), i + 1);
				System.out.println(rule);
				if (willWriteToFile) {
					fileWriter.write(rule + "\n");
				}
			}
			if (willWriteToFile) {
				fileWriter.close();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	private String printRule(Rule inRule, int ruleNum) {
		return "#" + ruleNum + ": " + inRule.translateRule() + "\n";
	}
}



