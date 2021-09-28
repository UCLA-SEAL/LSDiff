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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lsd.rule.LSDFact;
import lsd.rule.LSDInvalidTypeException;
import lsd.rule.LSDLiteral;
import lsd.rule.LSDRule;
import lsd.rule.LSDVariable;
import lsd.rule.LSDBinding;

public class LSDResult {
	
	public static class Bookmark {
		public String filename;
		public int startpos;
		public int length;
		public boolean oldFB;
		public Bookmark(String filename, int startpos, int length, boolean oldFB) {
			this.filename = filename;
			this.startpos = startpos;
			this.length = length;
			this.oldFB = oldFB;
		}
	}

	public int num_matches;
	public int num_counter;
	public LSDRule rule;
	public String desc;
	public List<LSDFact> examples;
	public List<Map<LSDVariable, String>> exceptions;
	public List<Bookmark> examplesBookmarks;
	public List<Bookmark> exceptionsBookmarks;
	private List<String> examplesString = null;
	private List<String> exceptionsString = null;
	public List<String> getExampleStr() {
		if (examplesString==null) {	//contruct strings from examples
			examplesString = new ArrayList<String>();
			for (LSDFact fact : examples) {
				examplesString.add(fact.toString());
			}
		}
		return examplesString;
	}
	public List<String> getExceptionsString() {
		if (exceptionsString==null) {	//contruct strings from examples
			exceptionsString = new ArrayList<String>();
			for (Map<LSDVariable, String> exception : exceptions) {
				String s = "";
				for (LSDLiteral l : rule.getAntecedents().getLiterals()) {
					if (!s.isEmpty()) s+=", ";
					LSDLiteral mylit = l;
					for (Entry<LSDVariable, String> entry : exception.entrySet()) {
						try {
							mylit = mylit.substitute(entry.getKey(), new LSDBinding(entry.getValue()));
						} catch (LSDInvalidTypeException e) {
							e.printStackTrace();
						}
					}
					s += mylit.nonNegatedCopy().toString();
				}
				s = "{"+s+"}";
				exceptionsString.add(s);
			}
		}
		return exceptionsString;
	}
}
