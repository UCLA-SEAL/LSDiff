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

import java.util.ArrayList;
import java.util.List;

public class Utils {

	//class to unify all naming issues between LSdiff, bytecode and source differences
	public static class BevName {
		private String primitiveName;
		private String pkgName;
		private String className;
		private String memberName;
		private List<BevName> argNames;
		public static enum Type {
			PRIMITIVE,
			PACKAGE,
			CLASS,
			METHOD,
			FIELD,
		}
		private Type type;
		private BevName() {}
		public static BevName makeFromLSdiff(String lsdiffname) {
			return makeFromLSdiff(lsdiffname, guessLSdiffType(lsdiffname));
		}
		public static BevName makeFromLSdiff(String lsdiffname, Type type) {
			try {
				BevName bvn = new BevName();
				if (type==Type.PRIMITIVE) {
					bvn.primitiveName = lsdiffname;
					bvn.type = type;
				} else if (type==Type.PACKAGE){
					bvn.pkgName = lsdiffname;
					bvn.type = Type.PACKAGE;
				} else if (type==Type.CLASS) {
					int ind1 = lsdiffname.indexOf('%');
					bvn.pkgName = lsdiffname.substring(0, ind1);
					bvn.className = lsdiffname.substring(ind1+2, lsdiffname.length());
					bvn.type = Type.CLASS;
				} else if (type==Type.FIELD) {
					int ind1 = lsdiffname.indexOf('%');
					int ind2 = lsdiffname.indexOf('#');
					bvn.pkgName = lsdiffname.substring(0, ind1);
					bvn.className = lsdiffname.substring(ind1+2, ind2);
					bvn.memberName = lsdiffname.substring(ind2+1, lsdiffname.length());
					bvn.type = Type.FIELD;
				} else if (type==Type.METHOD) {
					int ind1 = lsdiffname.indexOf('%');
					int ind2 = lsdiffname.indexOf('#');
					int ind3 = lsdiffname.indexOf('(');
					bvn.pkgName = lsdiffname.substring(0, ind1);
					bvn.className = lsdiffname.substring(ind1+2, ind2);
					bvn.memberName = lsdiffname.substring(ind2+1, ind3);
					bvn.type = Type.METHOD;
					//parse arguments
					bvn.argNames = new ArrayList<BevName>();
					int i = ind3+1;
					int j = lsdiffname.indexOf(',', i+1);
					while (true) {
						if (j<0) { //end of the line
							BevName argname = BevName.makeFromLSdiff(lsdiffname.substring(i, lsdiffname.length()-1), Type.CLASS);
							bvn.argNames.add(argname);
							break;
						} else {
							BevName argname = BevName.makeFromLSdiff(lsdiffname.substring(i, j), Type.CLASS);
							bvn.argNames.add(argname);
							i = j+1;
							j = lsdiffname.indexOf(',', i+1);
						}
					}
				}
				return bvn;
			} catch (NullPointerException e) {
				System.err.println("Name does not conform to type");
				e.printStackTrace();
				return null;
			}
		}
		//Guess type represented by this lsdiff name. Assumes no inner classes, no top level packages
		//does not work for package and class names
		public static Type guessLSdiffType(String lsdiffname) {
			if (lsdiffname.indexOf('.')<0) {
				return Type.PRIMITIVE;
			} else if (lsdiffname.indexOf('%')<0) {
				return Type.PACKAGE;
			} else if (lsdiffname.indexOf('#')<0) {
				return Type.CLASS;
			} else if (lsdiffname.indexOf('(')<0) {
				return Type.FIELD;
			} else {
				return Type.METHOD;
			}
		}
		public static BevName makeFromByteCode(String bytecodename) {
			BevName bvn = new BevName();
			
			//Pre-process for extra characters
			
			//Assume this is a method unless proven otherwise
			int ind2 = bytecodename.indexOf('.');
			if (ind2<0) {	//this is a class
				int ind1 = bytecodename.lastIndexOf('/');
				bvn.pkgName = bytecodename.substring(0, ind1).replace('/', '.');
				bvn.className = bytecodename.substring(ind1+1);
				bvn.type = Type.CLASS;
			} else {
				int ind1 = bytecodename.lastIndexOf('/', ind2);
				int ind3 = bytecodename.indexOf(':', ind2);
				if (bytecodename.indexOf('(')<0) {	//this ia a field
					bvn.pkgName = bytecodename.substring(0, ind1).replace('/', '.');
					bvn.className = bytecodename.substring(ind1+1, ind2);
					bvn.memberName = bytecodename.substring(ind2+1);
					bvn.type = Type.FIELD;
				} else {	//this is a method
					bvn.pkgName = bytecodename.substring(0, ind1).replace('/', '.');
					bvn.className = bytecodename.substring(ind1+1, ind2);
					bvn.memberName = bytecodename.substring(ind2+1, ind3);
					bvn.type = Type.METHOD;
					//parse arguments (TODO)
				}
			}
			return bvn;


//			return makeFromLSdiff(lsdiffname, guessLSdiffType(lsdiffname));
		}
	}
	
	public static String byteCodeClassNameToLsdiff(String bytecodename) {
		String res = bytecodename;
		int index = res.lastIndexOf('.');
		if (index>0) res = res.substring(0, index)+"#"+res.substring(index+1);
		res = res.replace("/", ".");
		index = res.lastIndexOf(".");
		if (index>0)
			res = res.substring(0,index)+"%"+res.substring(index);
		return res;
	}
	public static String byteCodeMethodNameToLsdiff(String bytecodename) {
		return "";
	}
	public static String lsdiffNameToByteCode(String lsdiffname) {
		String res = lsdiffname;
		res = res.replaceAll("%", "");
		res = res.replace(".", "/");
		res = res.replace("#", ".");
		return res;
	}
	public static String srcNameToByteCode(String srcname) {
		String res = srcname;
		res = res.replace('.', '/');
		int index = res.lastIndexOf('/');
		if (index>0) res = res.substring(0, index)+"."+res.substring(index+1);
		return res;
	}
	public static String dropParams(String name) {
		if (name.indexOf('(')<0)
			return name;
		else
			return name.substring(0, name.indexOf('('));
	}
}
