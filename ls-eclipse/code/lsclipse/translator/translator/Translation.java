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


public class Translation {
	
	private String activeS, passiveS, activeV, passiveV, result, fullNames;
	private boolean isActiveFirst, hasObject, isSpecialResult, isStructural;
	
	/* 
	 * active subject, passive subject, active verb, passive verb, is first argument active,
	 * does translation have object, result subject, string that specifies whether arguments are full
	 * identifiers, boolean that specifies whether type is "special": i.e. whether it needs to be
	 * translated fully if it is the result literal (so far this has only happened in 2 cases - 
	 * inheritedmethod and inheritedfield), boolean that specifies if type is structural (package,
	 * class, method, field)
	 */

	public Translation(String inActiveS, String inPassiveS, String inActiveV, String inPassiveV,
			boolean inIsActiveFirst, boolean inHasObject, String inResult, String inFullNames,
			boolean inIsSpecialResult, boolean inIsStructural) {
		activeS = inActiveS;
		passiveS = inPassiveS;
		activeV = inActiveV;
		passiveV = inPassiveV;
		isActiveFirst = inIsActiveFirst;
		hasObject = inHasObject;
		result = inResult;
		fullNames = inFullNames;
		isSpecialResult = inIsSpecialResult;
		isStructural = inIsStructural;
	}
	
	public String getActiveSubject() {
		return activeS;
	}
	
	public String getPassiveSubject() {
		return passiveS;
	}
	
	public String getActiveVerb() {
		return activeV;
	}
	
	public String getPassiveVerb() {
		return passiveV;
	}
	
	public boolean isActiveFirst() {
		return isActiveFirst;
	}
	
	public boolean hasObject() {
		return hasObject;
	}
	
	public String getResult() {
		return result;
	}
	
	public String getFullNames() {
		return fullNames;
	}
	
	public boolean isSpecialResult() {
		return isSpecialResult;
	}
	
	public boolean isStructural() {
		return isStructural;
	}
}
