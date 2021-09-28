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

import java.io.File;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import changetypes.AtomicChange.Modifier;

import metapackage.MetaInfo;
import tyRuBa.engine.Frame;
import tyRuBa.engine.FrontEnd;
import tyRuBa.engine.RBExpression;
import tyRuBa.engine.RBVariable;
import tyRuBa.util.ElementSource;

public class ChangeSet extends HashSet<AtomicChange> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public void print(PrintStream out) {
		if (this.size()>0) {
			out.println("~~~Changes~~~");
			for (AtomicChange ac : this) {
				out.println(ac.toString());
			}
		} else {
			out.println("No changes");
		}
	}

	public void deriveInheritedMembers(FactBase oldFB, FactBase newFB) {
		//Init tyruba frontend
		FrontEnd fe = new FrontEnd(true, new File(MetaInfo.fdbDir), true,
						null, true, false);
		fe.setCacheSize(FrontEnd.defaultPagerCacheSize);

		//Read facts into fe
		//load rule files
		try {
			fe.load(MetaInfo.includedDelta.getAbsolutePath());
			fe.load(MetaInfo.deltaDerivationRules.getAbsolutePath());
		} catch (Exception e) {
			System.err.println(e);
			return;
		}
		//load old FB
		for (Fact f : oldFB) {
			boolean add = false;

			//add subclassing
			if (f.type==Fact.Types.EXTENDS) {
				add = true;
			}
			
			//add inherited fields and methods
			if (f.type==Fact.Types.INHERITEDFIELD || f.type==Fact.Types.INHERITEDMETHOD) {
				add = true;
			}

			//add public fields and methods
			if ((f.type==Fact.Types.FIELD || f.type==Fact.Types.METHOD) && 
					(f.getVisibility()==Fact.Visibility.PUBLIC || f.getVisibility()==Fact.Visibility.PROTECTED) && 
					!f.params.get(1).startsWith("<init>(")) {
				add = true;
			}

			if (add) {
				try {
					fe.parse("before_"+f+".");
				} catch (Exception e) {
					System.err.println(e);
				}
			}
		}

		//load deltas
		for (AtomicChange ac : this) {
			boolean add = false;
			Fact f = ac.fact;

			//add subclassing
			if (f.type==Fact.Types.EXTENDS) {
				add = true;
			}

			//add public fields and methods
			if ((f.type==Fact.Types.FIELD || f.type==Fact.Types.METHOD) && 
					(f.getVisibility()==Fact.Visibility.PUBLIC || f.getVisibility()==Fact.Visibility.PROTECTED) && 
					!f.params.get(1).startsWith("<init>(")) {
				add = true;
			}

			if (add) {
				try {
					fe.parse(ac+".");
				} catch (Exception e) {
					System.err.println(e);
				}
			}
		}

		//Try to query the fe
		String query;
		RBExpression exp;
		ElementSource es;
		RBVariable ms = RBVariable.make("?ms");
		RBVariable t1 = RBVariable.make("?t1");
		RBVariable t2 = RBVariable.make("?t2");

		//Add inherited methods
		query = "added_inheritedmethod(?ms, ?t1, ?t2)";
		try {
			exp = fe.makeExpression(query);
			es = fe.frameQuery(exp);
			while (es.status() == ElementSource.ELEMENT_READY) {
				Frame frame = (Frame) es.nextElement();
				AtomicChange ac = AtomicChange.makeChange(Modifier.ADDED, 
						Fact.makeInheritedMethodFact(frame.get(ms).toString(), 
								frame.get(t1).toString(), frame.get(t2).toString(), "", 0, 0));
				System.out.println(ac);
				add(ac);
			}
		} catch (Exception e) {
		}
		//Add inherited fields
		query = "added_inheritedfield(?ms, ?t1, ?t2)";
		try {
			exp = fe.makeExpression(query);
			es = fe.frameQuery(exp);
			while (es.status() == ElementSource.ELEMENT_READY) {
				Frame frame = (Frame) es.nextElement();
				AtomicChange ac = AtomicChange.makeChange(Modifier.ADDED, 
						Fact.makeInheritedFieldFact(frame.get(ms).toString(), 
								frame.get(t1).toString(), frame.get(t2).toString(), "", 0, 0));
				System.out.println(ac);
				add(ac);
			}
		} catch (Exception e) {
		}
		//Delete inherited methods
		query = "deleted_inheritedmethod(?ms, ?t1, ?t2)";
		try {
			exp = fe.makeExpression(query);
			es = fe.frameQuery(exp);
			while (es.status() == ElementSource.ELEMENT_READY) {
				Frame frame = (Frame) es.nextElement();
				AtomicChange addac = AtomicChange.makeChange(Modifier.ADDED, 
						Fact.makeInheritedMethodFact(frame.get(ms).toString(), 
								frame.get(t1).toString(), frame.get(t2).toString(), "", 0, 0));
				if (this.contains(addac)) {
					this.remove(addac);
				} else {
					AtomicChange ac = AtomicChange.makeChange(Modifier.DELETED, 
							Fact.makeInheritedMethodFact(frame.get(ms).toString(), 
									frame.get(t1).toString(), frame.get(t2).toString(), "", 0, 0));
					System.out.println(ac);
					add(ac);
				}
			}
		} catch (Exception e) {
		}
		//Delete inherited fields
		query = "deleted_inheritedfield(?ms, ?t1, ?t2)";
		try {
			exp = fe.makeExpression(query);
			es = fe.frameQuery(exp);
			while (es.status() == ElementSource.ELEMENT_READY) {
				Frame frame = (Frame) es.nextElement();
				AtomicChange addac = AtomicChange.makeChange(Modifier.ADDED, 
						Fact.makeInheritedFieldFact(frame.get(ms).toString(), 
								frame.get(t1).toString(), frame.get(t2).toString(), "", 0, 0));
				if (this.contains(addac)) {
					this.remove(addac);
				} else {
					AtomicChange ac = AtomicChange.makeChange(Modifier.DELETED, 
							Fact.makeInheritedFieldFact(frame.get(ms).toString(), 
									frame.get(t1).toString(), frame.get(t2).toString(), "", 0, 0));
					System.out.println(ac);
					add(ac);
				}
			}
		} catch (Exception e) {
		}
	}

}
