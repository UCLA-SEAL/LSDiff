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
import java.util.List;

import lsd.rule.LSDBinding;
import lsd.rule.LSDLiteral;
import lsd.rule.LSDRule;


public class Rule {
	ArrayList<Literal> myLiterals;
	ArrayList<String> myExceptions;
	TranslationTable table;
	int instances[];
	
	public Rule(LSDRule r, int supportingInst, int totalInst) {
		table = new TranslationTable();
		instances = new int[2];
		instances[0] = supportingInst;
		instances[1] = totalInst;
		//deal with literals
		ArrayList<LSDLiteral> literals = r.getLiterals();
		myLiterals = new ArrayList<Literal>();
		ArrayList<Literal> conclusions = new ArrayList<Literal>();
		for (int i=0; i<literals.size(); ++i) {
			LSDLiteral literal = literals.get(i);
			boolean isConclusion = literal.isConclusion();
			//grab bindings
			List<LSDBinding> bindings = literal.getBindings(); 
			ArrayList<String> args = new ArrayList<String>();
			for (int j = 0; j <bindings.size(); ++j) {
				LSDBinding b = bindings.get(j);
				args.add(stripQuotes(b.toString()));
			}
			//grab literal type
			String dispName = literal.getPredicate().getDisplayName();
			int index = dispName.indexOf('_');	//assume that this underscore always exists
			String qual = dispName.substring(0, index);
			String type = dispName.substring(index+1, dispName.length()); 
			//create literal
			Literal myLiteral = new Literal(qual, type, args.size(), args);
			//make sure conclusion only appears at the end
			if (isConclusion)
				conclusions.add(myLiteral);
			else
				myLiterals.add(myLiteral);
		}
		myLiterals.addAll(conclusions);
		//deal with exceptions
		myExceptions = new ArrayList<String>();
	}

	private static String stripQuotes(String str) {
		if (str.charAt(0)=='"' || str.charAt(0)=='\'') {
			str = str.substring(1);
		}
		if (str.charAt(str.length()-1)=='"' || str.charAt(str.length()-1)=='\'') {
			str = str.substring(0, str.length()-1);
		}
		return str;
	}
	public Rule (ArrayList<Literal> inLits, int supportingInst, int totalInst) {
		myLiterals = inLits;
		myExceptions = new ArrayList<String>();
		table = new TranslationTable();
		instances = new int[2];
		instances[0] = supportingInst;
		instances[1] = totalInst;
	}
	
	public Rule (int supportingInst, int totalInst) {
		this(new ArrayList<Literal>(), supportingInst, totalInst);
	}
	
	public Rule () {
		this(new ArrayList<Literal>(), 0, 0);
	}
	
	public ArrayList<Literal> getLiterals () {
		return myLiterals;
	}
	
	public void addLiteral(Literal inLit) {
		myLiterals.add(inLit);
	}

	public ArrayList<String> getExceptions() {
		return myExceptions;
	}
	
	public void addException(String inEx) {
		myExceptions.add(inEx);
	}
	
	public String toString() {
		String retString = "";
		
		for (int i = 0; i < myLiterals.size(); i++) {
			retString = retString + myLiterals.get(i).toString();
			retString = retString + " ";
		}
		
		return retString;
	}
	
	public String translateRule() {
		String translation = "";//"Rule is supported by " + instances[0] + " out of " + instances[1] + " instances:\n";
		
		Literal subject = findSubjectLiteral();
		translation = translation + "All " + findSubject(subject) + " that " + findVerb() + " " + findObject() 
			+ " " + findResult();
		
		for (int i = 0; i < myExceptions.size(); i++) {
			translation = translation + translateException(myExceptions.get(i));
		}
		
		return translation;
	}
	
	private String translateException(String inEx) {
		return table.getException(inEx);
	}
	
	public Literal findSubjectLiteral() {
		/*
		 * Find first instance of ?x# in all literals in rule
		 */
		int foundSubject = 0, i = 0, j = 0;
		
		for (i = 0; i < myLiterals.size(); i++) {
			ArrayList<String> args = myLiterals.get(i).getArgs();
			for (j = 0; j < args.size(); j++) {
				int charIndex = args.get(j).indexOf('?');
				if (charIndex >= 0) {
					foundSubject = 1;
					break;
				}
			}
			if (foundSubject == 1)
				break;
		}
		
		return myLiterals.get(i);
	}
	
	private String findSubject(Literal inLit) {
		String subject = table.getSubject(inLit);
		return subject;
	}
	
	private String findVerb() {
		/*
		 * Verb comes from last antecedent
		 */
		String verb = "";
		int antNum;
		
		for (antNum = 0; antNum < myLiterals.size() - 1; antNum++) {
			verb = table.getVerb(myLiterals.get(antNum), true);
		}
		
		return verb;
	}
	
	private String findObject() {
		return table.getObject(this);
	}
	
	public boolean doLiteralsMatch(Literal one, Literal two) {
		if (one.getArgs().equals(two.getArgs()) && one.getType().equals(two.getType()))
			return true;
		else
			return false;
	}
	
	private String findResult() {
		return table.getResult(this);
	}
	
}
