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
package tyRuBa.engine;

import java.io.Serializable;


public class Validator implements Serializable {
    
    private boolean isOutdated = true;
	private boolean isValid = true;
	
    private long handle = -1;
    	
    public Validator() {}

    public long handle() {
		return handle;
	}
	
	public void setHandle(long handle) {
		this.handle = handle;
	}

	/**
	 * Method isValid.
	 * @return boolean
	 */
	public boolean isValid() {
		return isValid;
	}
    
    public void invalidate() {
        isValid = false;
    }

	public String toString() {
		return "Validator("+handle+","
		  + (isOutdated ? "OUTDATED" : "UPTODATE") +"," 
		  + (isValid ? "VALID" : "INALIDATED") + ")";
	}

	public boolean isOutdated() {
		return isOutdated;
	}

	public void setOutdated(boolean flag) {
		isOutdated = flag;
	}
	
	private boolean hasAssociatedFacts = false;
	
	public boolean hasAssociatedFacts() {
	    return hasAssociatedFacts;
	}
	
	public void setHasAssociatedFacts(boolean flag) {
	    hasAssociatedFacts = flag;
	}

}