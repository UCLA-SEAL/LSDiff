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

import java.io.*;
import java.util.*;
import java.util.regex.*;


public class Parser {
	
	File ruleFile;
	
	public Parser(File inRuleFile) {
		ruleFile = inRuleFile;
	}

	public ArrayList<Rule> parseRuleFile() throws FileNotFoundException {

		ArrayList<Rule> rules = new ArrayList<Rule>();
		
		Pattern ruleMarker = Pattern.compile("#(\\d+)\\D+(\\d+)/(\\d+)");
		Matcher m;
		Boolean hasEx;

		Scanner reader = new Scanner(ruleFile);

		String line;
		line = reader.nextLine();

		while (reader.hasNextLine()) {
			m = ruleMarker.matcher(line);
			if (m.find()) {
				hasEx = false;
				
				int firstNum = Integer.parseInt(m.group(2)); //supporting instances
				int secondNum = Integer.parseInt(m.group(3)); //total instances
				
				if (firstNum < secondNum) {
					hasEx = true;
				}

				line = reader.nextLine();
				Rule newRule = parseRule(line, firstNum, secondNum);

				if (hasEx) {
					Pattern findEx = Pattern.compile("#N");
					Matcher newM = findEx.matcher(line);
					while (!newM.find()) {
						if (reader.hasNextLine()) {
							line = reader.nextLine();
							newM = findEx.matcher(line);

							m = ruleMarker.matcher(line);
							if (m.find()) {
								break;
							}
						}
					}
					while (newM.reset().find()) {
						newRule.addException(parseException(line));
						line = reader.nextLine();
						newM = findEx.matcher(line);
					}
				}

				rules.add(newRule);
			}
			else {
				line = reader.nextLine();
			}
		}
		return rules;
	}

	private Rule parseRule(String line, int supportingInst, int totalInst) {
		Rule retRule = new Rule(supportingInst, totalInst);
		String pieces[] = line.split(" ");

		for (int i = 0; i < pieces.length; i++) {
			if ((!pieces[i].equals("^")) && (!pieces[i].equals("=>"))) {
				String splitPiece[], qual, type;
				ArrayList<String> args = new ArrayList<String>();

				pieces[i] = pieces[i].substring(0, pieces[i].length() - 1);

				splitPiece = pieces[i].split("_", 2);
				qual = splitPiece[0];

				int k = splitPiece[1].indexOf('(');
				type = splitPiece[1].substring(0, k);
				splitPiece[1] = splitPiece[1].substring(k + 1);

				String argString = splitPiece[1];
				boolean isDone = false;
				while (!isDone) {
					int beginIndex = 0, endIndex = 0;

					if (argString.charAt(0) == '"') {
						argString = argString.substring(1);
						endIndex = argString.indexOf('"');
						args.add(argString.substring(0, endIndex));
						if (argString.substring(endIndex + 1, argString.length()).contains(","))
							beginIndex = endIndex + 2;
						else
							beginIndex = argString.length();
					}
					else if (argString.contains(",")){
						endIndex = argString.indexOf(',');
						args.add(argString.substring(0, endIndex));
						beginIndex = endIndex + 1;
					}
					else {
						args.add(argString);
						beginIndex = argString.length();
					}

					argString = argString.substring(beginIndex, argString.length());
					if (argString.length() == 0)
						isDone = true;
				}

				retRule.addLiteral(new Literal(qual, type, args.size(), args));
			}
		}
		return retRule;
	}

	private String parseException(String inEx) {
		String retString;
		String exPieces[] = inEx.split("=\"");

		retString = exPieces[exPieces.length - 1];
		retString = retString.substring(0, retString.length() - 2);

		return retString;
	}

}
