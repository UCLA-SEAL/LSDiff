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

import java.util.Vector;

public class Fact {

	public enum Types {
		//LSD facts
		PACKAGE,
		TYPE,
		METHOD,
		FIELD,
		RETURN,
		FIELDOFTYPE,
		ACCESSES,
		CALLS,
		EXTENDS,
		IMPLEMENTS,
		INHERITEDFIELD,
		INHERITEDMETHOD,
		TYPEINTYPE,
		//Non-LSD facts
		METHODBODY,		//(methodFullName, methodBody)
		METHODSIGNATURE,	//(methodFullName, methodSignature) Method args is encoded as "type1:arg1,type2:arg2,...->return"
	}
	public enum Visibility {
		PRIVATE, 
		PROTECTED, 
		PUBLIC,
		PACKAGE,
	}
	public enum Kind {
		INTERFACE,
		CLASS,
	}

	public Types type;
	public Vector<String> params;

	//meta data
	public Visibility visibility;
	public Kind kind;

	//physical location of node in source file
	public String filename = "";
	public int startposition;
	public int length;

	public Fact(Types mytype, Vector<String> myparams, String filename, int startposition, int length) {
		type = mytype;
		params = new Vector<String>(myparams);
		this.filename = filename;
		this.startposition = startposition;
		this.length = length;
	}

	public Fact(Fact f) {
		type = f.type;
		params = f.params;
	}

	public int hashCode() {
		return params.hashCode()+type.ordinal()*1024;
//		return type.ordinal();
	}

	public Visibility getVisibility() {
		return visibility;
	}
	public Kind getKind() {
		return kind;
	}

	public boolean equals(Object o) {
		if (o.getClass()!=this.getClass()) return false;
		Fact f = (Fact)o;
		if (!type.equals(f.type)) return false;
		for (int i=0; i<params.size(); ++i) {
			if (!params.get(i).equals(f.params.get(i))) 
//					&& !params.get(i).equals("*") && !f.params.get(i).equals("*"))
				return false;
		}
		return true;
	}

	public String toString() {
		String res = "";
		for (int i=0; i<params.size(); ++i) {
			String arg = params.get(i);
			if (!res.contentEquals("")) res+=", ";
			res += "\""+arg+"\"";
		}
		res = type.toString().toLowerCase()+"("+res+")";
		return res;
	}

	public boolean matchParams(Vector<String> otherparams) {
		for (int i=0; i<otherparams.size(); ++i) {
			if (!params.get(i).equals(otherparams.get(i))
					&& !params.get(i).equals("*") && !otherparams.get(i).equals("*"))
				return false;
		}
		return true;
	}
	
	public String getDeclaringTypeName() {	//basically return the type full name
		switch (type) {
		//N.A. types, ie packages
		case PACKAGE:
			return "";
		case TYPE:
		case TYPEINTYPE:
			return params.get(0);
		case METHOD:
		case FIELD:
		case RETURN:
		case FIELDOFTYPE:
		case CALLS:
			return getTypeFromFullName(params.get(0));
		case ACCESSES:
			return getTypeFromFullName(params.get(1));
		case EXTENDS:
		case IMPLEMENTS:
			return params.get(1);
		case INHERITEDFIELD:
		case INHERITEDMETHOD:
			return params.get(2);
		}
		return "";
	}
	//Get type name from method or field full name
	private static String getTypeFromFullName(String fullname) {
		int ind = fullname.lastIndexOf('#');
		if (ind<0) return "";
		return fullname.substring(0, ind);
	}

	public static Fact makeFact(Types type, Vector<String> params,
			String filename, int startposition, int length) {
		return new Fact(type, params, filename, startposition, length);
	}

	public static Fact makePackageFact(String packageFullName,
			String filename, int startposition, int length) {
		Vector<String> params = new Vector<String>();
		params.add(packageFullName);
		return new Fact(Types.PACKAGE, params, filename, startposition, length);
	}

	public static Fact makeTypeFact(String typeFullName, String typeShortName, String packageFullName, 
			Kind typeKind, String filename, int startposition, int length) {
		Vector<String> params = new Vector<String>();
		params.add(typeFullName);
		params.add(typeShortName);
		params.add(packageFullName);
		Fact f = new Fact(Types.TYPE, params, filename, startposition, length);
		f.kind = typeKind;
		return f;
	}

	public static Fact makeFieldFact(String fieldFullName, String fieldShortName, String typeFullName, 
			Visibility visibility, String filename, int startposition, int length) {
		Vector<String> params = new Vector<String>();
		params.add(fieldFullName);
		params.add(fieldShortName);
		params.add(typeFullName);
		Fact f = new Fact(Types.FIELD, params, filename, startposition, length);
		f.visibility = visibility;
		return f;
	}

	public static Fact makeMethodFact(String methodFullName, String methodShortName, String typeFullName, 
			Visibility visibility, String filename, int startposition, int length) {
		Vector<String> params = new Vector<String>();
		params.add(methodFullName);
		params.add(methodShortName);
		params.add(typeFullName);
		Fact f = new Fact(Types.METHOD, params, filename, startposition, length);
		f.visibility = visibility;
		return f;
	}

	public static Fact makeFieldTypeFact(String fieldFullName, String declaredTypeFullName,
			String filename, int startposition, int length) {
		Vector<String> params = new Vector<String>();
		params.add(fieldFullName);
		params.add(declaredTypeFullName);
		return new Fact(Types.FIELDOFTYPE, params, filename, startposition, length);
	}

	public static Fact makeTypeInTypeFact(String innerTypeFullName, String outerTypeFullName,
			String filename, int startposition, int length) {
		Vector<String> params = new Vector<String>();
		params.add(innerTypeFullName);
		params.add(outerTypeFullName);
		return new Fact(Types.TYPEINTYPE, params, filename, startposition, length);
	}

	public static Fact makeReturnsFact(String methodFullName, String returnTypeFullName,
			String filename, int startposition, int length) {
		Vector<String> params = new Vector<String>();
		params.add(methodFullName);
		params.add(returnTypeFullName);
		return new Fact(Types.RETURN, params, filename, startposition, length);
	}

	public static Fact makeImplementsFact(String superTypeFullName, String subTypeFullName,
			String filename, int startposition, int length) {
		Vector<String> params = new Vector<String>();
		params.add(superTypeFullName);
		params.add(subTypeFullName);
		return new Fact(Types.IMPLEMENTS, params, filename, startposition, length);
	}

	public static Fact makeExtendsFact(String superTypeFullName, String subTypeFullName,
			String filename, int startposition, int length) {
		Vector<String> params = new Vector<String>();
		params.add(superTypeFullName);
		params.add(subTypeFullName);
		return new Fact(Types.EXTENDS, params, filename, startposition, length);
	}

	public static Fact makeInheritedFieldFact(String fieldShortName, String superTypeFullName, 
			String subTypeFullName, String filename, int startposition, int length) {
		Vector<String> params = new Vector<String>();
		params.add(fieldShortName);
		params.add(superTypeFullName);
		params.add(subTypeFullName);
		return new Fact(Types.INHERITEDFIELD, params, filename, startposition, length);
	}

	public static Fact makeInheritedMethodFact(String methodShortName, String superTypeFullName, 
			String subTypeFullName, String filename, int startposition, int length) {
		Vector<String> params = new Vector<String>();
		params.add(methodShortName);
		params.add(superTypeFullName);
		params.add(subTypeFullName);
		return new Fact(Types.INHERITEDMETHOD, params, filename, startposition, length);
	}

	public static Fact makeMethodBodyFact(String methodFullName, String methodBody,
			String filename, int startposition, int length) {
		Vector<String> params = new Vector<String>();
		params.add(methodFullName);
		params.add(methodBody);
		return new Fact(Types.METHODBODY, params, filename, startposition, length);
	}

	public static Fact makeMethodArgsFact(String methodFullName, String methodSignature,
			String filename, int startposition, int length) {
		Vector<String> params = new Vector<String>();
		params.add(methodFullName);
		params.add(methodSignature);
		return new Fact(Types.METHODSIGNATURE, params, filename, startposition, length);
	}

	public static Fact makeCallsFact(String callerMethodFullName, String calleeMethodFullName,
			String filename, int startposition, int length) {
		Vector<String> params = new Vector<String>();
		params.add(callerMethodFullName);
		params.add(calleeMethodFullName);
		return new Fact(Types.CALLS, params, filename, startposition, length);
	}

	public static Fact makeAccessesFact(String fieldFullName, String accessorMethodFullName,
			String filename, int startposition, int length) {
		Vector<String> params = new Vector<String>();
		params.add(fieldFullName);
		params.add(accessorMethodFullName);
		return new Fact(Types.ACCESSES, params, filename, startposition, length);
	}
}

