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


public class Literal {
	private String qualifier, type; 
	private ArrayList<String> args;
	
	public Literal (String inQual, String inType, int inNumArgs, ArrayList<String> inArgs) {		
		qualifier = inQual;
		type = inType;
		args = inArgs;
	}
	
	public String toString() {
		String retString = qualifier + " " + type + " (" + args.toString() + ")";
		return retString;
	}
	
	public ArrayList<String> getArgs() {
		return args;
	}
	
	public String getQualifier() {
		return qualifier;
	}
	
	public String getType() {
		return type;
	}
	
	public boolean equals(Literal otherLit) {
		if (this.type.equals(otherLit.type) && this.args.equals(otherLit.args))
			return true;
		else
			return false;
	}
	
}
