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
package tyRuBa.modes;

public class ErrorMode extends Mode {
	
	String msg;
		
	public ErrorMode(String msg) {
		super(Multiplicity.zero, Multiplicity.infinite);
		this.msg = msg;
	}
			
	public String toString() {
		return "ERROR: " + msg;
	}
	
	public boolean equals(Object other) {
		if (other instanceof ErrorMode) {
			return msg.equals(((ErrorMode)other).msg);
		} else {
			return false;
		}
	}
	
	public int hashCode() {
		return 122 + msg.hashCode();
	}

	public Mode add(Mode other) {
		return this;
	}

}