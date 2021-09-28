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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

import lsd.rule.LSDBinding;
import lsd.rule.LSDFact;
import lsd.rule.LSDPredicate;
import changetypes.AtomicChange;
import changetypes.Fact;
import changetypes.Fact.Types;
import changetypes.AtomicChange.Modifier;

public class LSDWrapper {

	static final Map<Types, String> factTypeToPredicateMap = new HashMap<Types, String>();
	static final Map<Modifier, String> modifierToPredicateMap = new HashMap<Modifier, String>();

	public static LSDFact fact2LSDFact(Fact f, Modifier modifier) {
		LSDPredicate pred = makeLSDPredicate(f.type, modifier);
		String constants = "";
		int numparams = f.params.size();
		for (int i=0; i<numparams; ++i) {
			String s = f.params.get(i);
			if (constants.length()>0) constants+=",";
			constants += "\""+s+"\"";
		}
		return LSDFact.createLSDFact(pred, constants, true);
	}
	public static LSDFact atomicChange2LSDFact(AtomicChange ac) {
		return fact2LSDFact(ac.fact, ac.mod);
	}
	public static AtomicChange LSDFact2AtomicChange(LSDFact f) {
		LSDPredicate pred = f.getPredicate();
		Modifier mod = LSDPred2Modifier(pred);
		Types type = LSDPred2Type(pred);

		//make params string
		Vector<String> params = new Vector<String>();
		List<LSDBinding> bindings = f.getBindings();
		for (LSDBinding binding : bindings) {
			String bindstr = binding.toString();
			//strip enclosing quotes
			if (bindstr.charAt(0)=='"') bindstr=bindstr.substring(1);
			if (bindstr.charAt(bindstr.length()-1)=='"') bindstr=bindstr.substring(0,bindstr.length()-1);
			params.add(bindstr);
		}

		Fact fact = Fact.makeFact(type, params, "", 0, 0);
		return AtomicChange.makeChange(mod, fact);
	}
	public static Types LSDPred2Type(LSDPredicate pred) {
		String displayname = pred.getDisplayName();
		int ind =displayname.indexOf('_');
		if (ind<0)	//this display name does not have two parts! 
			return null;
		String type = displayname.substring(ind+1);
		return LSDTypeString2FactType(type);
	}
	public static Modifier LSDPred2Modifier(LSDPredicate pred) {
		String displayname = pred.getDisplayName();
		int ind =displayname.indexOf('_');
		if (ind<0)	//this display name does not have two parts! 
			return null;
		String modifier = displayname.substring(0, ind);
		return LSDModString2Modifier(modifier);
	}

	private static void initFactTypeToPredicateMap() {
		factTypeToPredicateMap.put(Types.PACKAGE, "package");
		factTypeToPredicateMap.put(Types.TYPE, "type");

		factTypeToPredicateMap.put(Types.EXTENDS, "extends");
		factTypeToPredicateMap.put(Types.IMPLEMENTS, "implements");

		factTypeToPredicateMap.put(Types.INHERITEDFIELD, "inheritedfield");
		factTypeToPredicateMap.put(Types.INHERITEDMETHOD, "inheritedmethod");

		factTypeToPredicateMap.put(Types.METHOD, "method");
		factTypeToPredicateMap.put(Types.RETURN, "return");
		factTypeToPredicateMap.put(Types.FIELD, "field");
		factTypeToPredicateMap.put(Types.FIELDOFTYPE, "fieldoftype");
		factTypeToPredicateMap.put(Types.TYPEINTYPE, "typeintype");

		factTypeToPredicateMap.put(Types.CALLS, "calls");
		factTypeToPredicateMap.put(Types.ACCESSES, "accesses");
	}
	private static void initModifierToPredicateMap() {
		modifierToPredicateMap.put(Modifier.ADDED, "added");
		modifierToPredicateMap.put(Modifier.DELETED, "deleted");
		modifierToPredicateMap.put(Modifier.MODIFIED, "modified");
		modifierToPredicateMap.put(Modifier.BEFORE, "before");
		modifierToPredicateMap.put(Modifier.AFTER, "after");
	}
	
	private static String modifier2LSDModString(Modifier mod) {
		return mod.toString().toLowerCase();
	}
	private static Modifier LSDModString2Modifier(String lsdtype) {
		return Modifier.valueOf(lsdtype.toUpperCase());
	}
	private static String factType2LSDTypeString(Types type) {
		return type.toString().toLowerCase();
	}
	private static Types LSDTypeString2FactType(String lsdtype) {
		return Types.valueOf(lsdtype.toUpperCase());
	}

	private static LSDPredicate makeLSDPredicate(Types type, Modifier modifier) {
		try {
			return LSDPredicate.getPredicate(modifier2LSDModString(modifier)+"_"+factType2LSDTypeString(type));
		} catch (NullPointerException e) {
			return null;
		}
	}

}
