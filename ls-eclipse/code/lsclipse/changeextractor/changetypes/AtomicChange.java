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
package changetypes;

public class AtomicChange {
	
	public enum Modifier {
		ADDED,
		DELETED,
		MODIFIED,
		BEFORE,
		AFTER,
	}

	public Modifier mod;
	public Fact fact;

	private AtomicChange(Modifier mod, Fact fact) {
		this.mod = mod;
		this.fact = fact;
	}

	public AtomicChange(AtomicChange f) {
		this.mod = f.mod;
		this.fact = f.fact;
	}

	public int hashCode() {
		return fact.hashCode();
	}
	public boolean equals(Object o) {
		if (o.getClass()!=this.getClass()) return false;
		AtomicChange f = (AtomicChange)o;
		if (!mod.equals(f.mod)) return false;
		if (!fact.equals(f.fact)) return false;
		return true;
	}

	public String toString() {
		return mod.toString().toLowerCase()+"_"+fact.toString();
	}
	
	public static AtomicChange makeChange(Modifier mod, Fact fact) {
		return new AtomicChange(mod, fact);
	}
}

