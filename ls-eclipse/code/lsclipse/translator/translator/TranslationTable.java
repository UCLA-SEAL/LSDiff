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

import java.util.ArrayList;
import java.util.Hashtable;

public class TranslationTable {
	/* Types/relationships: (+ before, after)
	   1. package (packageFullName)
	   2. type (typeFullName, typeShortName, packageFullName) - class
	   3. method (methodFullName, methodShortName, typeFullName)
	   4. field (fieldFullName, fieldShortName, typeFullName)
	   
	   5. return (methodFullName, returnTypeFullName)
	   6. fieldoftype (fieldFullName, declaredTypeFullName)
	   7. typeintype (innerTypeFullName, outerTypeFullName)
	   8. accesses (fieldFullName, accessorMethodFullName)
	   9. calls (callerMethodFullName, calleeMethodFullName)
	   10. subtype (superTypeFullName, subTypeFullName)
	   11. inheritedfield (fieldShortName, superTypeFullName, subTypeFullName)
	   12. inheritedmethod (methodShortName, superTypeFullName, subTypeFullName)
	*/
	
	private Hashtable<String, Translation> translations;
	private Hashtable<String, String> qualifiers;
	
	public TranslationTable() {
		translations = new Hashtable<String, Translation>();
		qualifiers = new Hashtable<String, String>();
		
		/* 
		 * active subject, passive subject, active verb, passive verb, is first argument active,
		 * does translation have object, result subject, string that specifies whether arguments are full
		 * identifiers, boolean that specifies whether type is "special": i.e. whether it needs to be
		 * translated fully if it is the result literal (so far this has only happened in 2 cases - 
		 * inheritedmethod and inheritedfield), boolean that specifies if type is structural (package,
		 * class, method, field)
		 */
		
		translations.put("package", new Translation("packages", "", "exist", "", true, false, "packages", "T", false, true));
		translations.put("type", new Translation("types", "", "exist", "", true, false, "types", "TFT", false, true));
		translations.put("method", new Translation("methods", "", "exist", "", true, false, "methods", "TFT", false, true));
		translations.put("field", new Translation("fields", "", "exist", "", true, false, "fields", "TFT", false, true));
		
		translations.put("return", new Translation("methods", "", "return", "", true, true, "returns", "TT", false, false));
		translations.put("fieldoftype", new Translation("fields", "types", "initialize", "are initialized by", true, true, "fields", "TT", false, false));
		translations.put("typeintype", new Translation("types", "types", "appear in", "enclose", true, true, "types", "TT", false, false));
		translations.put("accesses", new Translation("methods", "fields", "access", "get accessed by", false, true, "accesses", "TT", false, false));
		translations.put("calls", new Translation("methods", "methods", "call", "get called by", true, true, "calls", "TT", false, false));
		translations.put("subtype", new Translation("types", "types", "extend", "", true, true, "subtypes", "TT", false, false));
		translations.put("extends", new Translation("types", "types", "extend", "get extended by", false, true, "subtypes", "TT", false, false));
		translations.put("implements", new Translation("types", "types", "implement", "get implemented by", false, true, "implementations", "TT", false, false));
		
		// The following two facts are in the table, but do not work correctly:
		translations.put("inheritedfield", new Translation("types", "fields", "inherit", "get inherited by", false, true, "fields", "FTT", true, false));
		translations.put("inheritedmethod", new Translation("types", "methods", "inherit", "get inherited by", false, true, "methods", "FTT", true, false));
		
		qualifiers.put("before", "did");
		qualifiers.put("after", "now");
		qualifiers.put("deleted", "deleted");
		qualifiers.put("added", "added");
		qualifiers.put("modified", "modified");
		
	}
	
	public String getSubject(Literal inLit) {
		//This method adds the encompassing structure information the subject of the sentence
		String subject = simpleGetSubject(inLit);
		
		if (translations.get(inLit.getType()).isStructural()) {
			int structureArgIndex = inLit.getArgs().size() - 1;
			if (structureArgIndex > 0) {
				String arg = inLit.getArgs().get(structureArgIndex);
				if (!arg.contains("?x"))
					subject = subject + " in " + translateArg(inLit.getArgs().get(structureArgIndex), true);
			}
		}
		
		return subject;
	}
	
	private String simpleGetSubject(Literal inLit) {
		//This method simply finds the correct subject translation for the given literal
		String subject;
		
		if (isActive(inLit))
			subject = translations.get(inLit.getType()).getActiveSubject();
		else
			subject = translations.get(inLit.getType()).getPassiveSubject();
		
		return subject;
	}
	
	public String getVerb(Literal inLit, boolean withQualifier) {
		//The verb is returned fully-qualified (with a temporal or descriptive qualifier)
		String verb = "";
		
		if (withQualifier)
			verb = qualifiers.get(inLit.getQualifier()) + " ";
		
		if (isActive(inLit))
			verb = verb + translations.get(inLit.getType()).getActiveVerb();
		else
			verb = verb + translations.get(inLit.getType()).getPassiveVerb();
		
		return verb;
	}
	
	public String getObject(Rule inRule) {
		//This method finds the object of the rule, only drawing from those fact types that have objects
		String object = "";
		int litNum, argNum;
		
		for (litNum = 0; litNum < inRule.getLiterals().size() - 1; litNum++) {
			Literal currentLit = inRule.getLiterals().get(litNum);
			if (translations.get(currentLit.getType()).hasObject()) {
				for (argNum = 0; argNum < currentLit.getArgs().size(); argNum++) {
					String currentArg = currentLit.getArgs().get(argNum);
					if (!currentArg.contains("?x"))
						object = translateArg(currentArg, 
								isFullName(translations.get(currentLit.getType()).getFullNames().charAt(argNum)));
					else if (object.equals(""))
						object = "all " + getSubject(currentLit);
				}
			}
		}
		
		return object;
	}
	
	public String getObject(Literal inLit) {
		Rule tempRule = new Rule();
		tempRule.addLiteral(inLit);
		return getObject(tempRule);
	}
	
	public String getResult(Rule inRule) {
		//This method assembles the last literal into a translated result, depending on temporal
		//and characteristic qualifiers
		String result = "";
		ArrayList<Literal> literals = inRule.getLiterals();
		Literal resultLit = literals.get(literals.size() - 1);
		Literal subjectLit = inRule.findSubjectLiteral();
		
		if (doSubjectResultMatch(subjectLit, resultLit))
			result = result + "were ";
		
		result = result + qualifiers.get(resultLit.getQualifier()) + " ";
			
		if (!doSubjectResultMatch(subjectLit, resultLit)) {
			if (translations.get(resultLit.getType()).isSpecialResult()) {
				result = result + getSubject(resultLit) + " that " + getVerb(resultLit, false) + " " + getObject(resultLit);
			}
			else {
				result = result + "those " + translations.get(resultLit.getType()).getResult();
			}
		}
		
		return result;
	}
	
	public String getException(String inEx) {
		return "Except for " + translateArg(inEx, true);
	}
	
	private String translateArg(String inString, boolean isFullName) {
		//This method translated encompassing structure
		String tempString = inString, retString = "";
		int index;
		boolean breakCond = false;
		
		if (isFullName) {
			index = tempString.indexOf("%");
			if (index > 0) {
				retString = " in package " + tempString.substring(0, index);
				tempString = tempString.substring(index + 1);
			}
			else if (index == 0) {
				tempString = tempString.substring(1);
			}
			else {
				//retString = "package " + tempString + retString;
				retString = tempString + retString;
				breakCond = true;
			}

			index = tempString.indexOf("#");
			if ((index >= 0) && !breakCond) {
				retString = " in class " + tempString.substring(0, index) + retString;
				tempString = tempString.substring(index + 1);
			}
			else if (!breakCond){
				retString = "class " + tempString + retString;
				breakCond = true;
			}
			
			index = tempString.indexOf("/");
			if ((index >= 0) && !breakCond) {
				retString = " in method " + tempString.substring(0, index) + retString;
				tempString = tempString.substring(index + 1);
			}
			else if (!breakCond){
				retString = "method " + tempString + retString;
				breakCond = true;
			}
		
			if ((tempString.length() > 0) && !breakCond) {
				retString = "field " + tempString + retString;
			}
		}
		else
			retString = inString;
		
		return retString;
	}
	
	private boolean doSubjectResultMatch(Literal subject, Literal result) {
		if (simpleGetSubject(subject).equals(translations.get(result.getType()).getResult()))
			return true;
		else 
			return false;
	}
	
	private boolean isActive(Literal inLit) {
		boolean retBool;
		int argNum;
		
		if (!translations.get(inLit.getType()).isActiveFirst()) {
			for (argNum = 0; argNum < inLit.getArgs().size(); argNum++) {
				if (inLit.getArgs().get(argNum).contains("?x"))
					break;
			}
			if (argNum == 0)
				retBool = false;
			else
				retBool = true;
		}
		else {
			for (argNum = 0; argNum < inLit.getArgs().size(); argNum++) {
				if (inLit.getArgs().get(argNum).contains("?x"))
					break;
			}
			if (argNum == 0)
				retBool = true;
			else
				retBool = false;
		}
		
		return retBool;
	}
	
	private boolean isFullName(char inChar) {
		if (inChar == 'T')
			return true;
		else
			return false;
	}
	
}
