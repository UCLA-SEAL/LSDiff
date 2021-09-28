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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;

import metapackage.MetaInfo;
import tyRuBa.engine.Frame;
import tyRuBa.engine.FrontEnd;
import tyRuBa.engine.RBExpression;
import tyRuBa.engine.RBVariable;
import tyRuBa.util.ElementSource;

import changetypes.AtomicChange.Modifier;
import changetypes.Fact.Types;

public class FactBase extends HashSet<Fact> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//Forest of rules, arranged by class hierarchy
	//root of each tree in the forest is a class whose parent is not captured in this FB 
	private static class FactTree {
		//A node in the class tree, consists of all the facts derived from that class
		public static class FactTreeNode {
			public String packagename;	//TODO: currently not available
			public String typename;	//full name of type
			public String supername;	//full name of supertype
			public Fact typefact;
			public Set<Fact> facts;
			public FactTreeNode supernode=null;
			public Set<FactTreeNode> subnodes;
		}
		//Master list of all nodes
		private Map<String, FactTreeNode> nodes = new HashMap<String, FactTreeNode>();
		//Nodes which are currently roots of the class tree---no subtype fact found or super not in FB
		public Set<FactTreeNode> roots = new HashSet<FactTreeNode>();
		//Packages
		private Set<Fact> packages = new HashSet<Fact>();	//TODO: packages should be another type of root
		//Facts not part of any node (should be empty)
		private Set<Fact> unclaimedfacts = new HashSet<Fact>();

		private boolean addPackageFact(Fact f) {
			packages.add(f);
			return true;
		}
		private boolean addTypeFact(Fact f) {
			FactTreeNode node = new FactTreeNode();
			node.packagename = getParentFromFullName(f.params.get(0));
			node.typename = f.params.get(0);
			node.supername = "";
			node.typefact = f;
			node.subnodes = new HashSet<FactTreeNode>();
			node.facts = new HashSet<Fact>();
			node.facts.add(f);
			//claim facts from unclaimed pile, including subtype facts
			Set<Fact> claimedfacts = new HashSet<Fact>();
			for (Fact unclaimedfact : unclaimedfacts) {
				if (!unclaimedfact.getDeclaringTypeName().equals(node.typename))
					continue;	//this is not my node
				if (unclaimedfact.type == Fact.Types.EXTENDS) {	//if subtype fact then try to connect
					connectNodes(unclaimedfact.params.get(0), unclaimedfact.params.get(1));
				}
				node.facts.add(unclaimedfact);
				claimedfacts.add(unclaimedfact);
			}
			unclaimedfacts.removeAll(claimedfacts);
			//claim child nodes from roots
			Set<FactTreeNode> claimedroots = new HashSet<FactTreeNode>();
			for (FactTreeNode root : roots) {
				if (root.supername.equals(node.typename)) {
					connectNodes(node.typename, root.typename);
					claimedroots.add(root);
				}
			}
			roots.removeAll(claimedroots);
			//if no parentclass defined then it is a root for now, even if it has a superclass
			if (node.supernode == null)
				roots.add(node);
			//add to master list
			nodes.put(node.typename, node);
			return true;
		}
		private boolean connectNodes(String superclass, String subclass) {
			FactTreeNode supernode = nodes.get(superclass);
			FactTreeNode subnode = nodes.get(subclass);
			if (subnode!=null) {
				subnode.supername = superclass;
			}
			if (supernode!=null && subnode!=null) {
				subnode.supernode = supernode;
				supernode.subnodes.add(subnode);
				roots.remove(subnode);
				return true;
			}
			return false;
		}
		private boolean addFactToType(Fact f, String typename) {
			FactTreeNode node = nodes.get(typename);
			if (node==null) {
				unclaimedfacts.add(f);
			} else {
				node.facts.add(f);
			}
			return true;
		}
		public boolean addFact(Fact f) {
			//Decide what to do based on fact type
			switch (f.type) {
			case PACKAGE:
				return addPackageFact(f);
			case TYPE:
				return addTypeFact(f);
			case EXTENDS:
				connectNodes(f.params.get(0), f.params.get(1));
				return addFactToType(f, f.getDeclaringTypeName());
			default:
				return addFactToType(f, f.getDeclaringTypeName());
			}
		}
		public void printTree() {
			//pretty printer to show hierarchy of tree
			int factcount = 0;
			for (FactTreeNode root : roots) {
				System.out.println();
				factcount += printNode(root, "");
			}
			System.out.println("Total of "+nodes.size()+" classes and "+factcount+" facts in tree");
			System.out.println(roots.size()+" roots, "+packages.size()+" packages, and "+unclaimedfacts.size()+" unclaimed facts: ");
			for (Fact f: unclaimedfacts) {
				System.out.println("  - "+f);
			}
		}
		private int printNode(FactTreeNode node, String prefix) {
			int factcount = node.facts.size();
			System.out.println(prefix+node.typename+"  ("+node.packagename+")  "+factcount+" facts");
			for (Fact f: node.facts) {
//				System.out.println(prefix+"- "+f);
			}
			for (FactTreeNode subnode : node.subnodes) {
				factcount += printNode(subnode, prefix+"    ");
			}
			return factcount;
		}
		public FactTree(FactBase fb) {
			for (Fact f : fb) {
				addFact(f);
			}
		}
	}

	public FactBase() {
		super();
	}

	public FactBase(FactBase f) {
		super(f);
	}

	public void print(PrintStream out) {
		if (this.size()>0) {
			out.println("~~~Facts~~~");
			for (Fact f : this) {
				out.println(f.toString());
			}
		} else {
			out.println("No facts");
		}
	}

	private static String getParentFromFullName(String name) {
		//to get package from type, remove last dot
		int lastdot = name.lastIndexOf('.');
		if (lastdot == -1) return "";	//this is a short name
		return name.substring(0, lastdot);
	}

	private static String getChildFromFullName(String name) {
		int lastdot = name.lastIndexOf('.');
		return name.substring(lastdot+1);
	}

	public FactBase findFact(Types type, Vector<String> params) {
		FactBase res = new FactBase();
		for (Fact f : this) {
			if (f.type==type) {
				if (f.matchParams(params)) {
					res.add(f);
				}
			}
		}
		return res;
	}
	public boolean containsFact(Types type, Vector<String> params) {
		for (Fact f : this) {
			if (f.type==type) {
				if (f.matchParams(params)) {
					return true;
				}
			}
		}
		return false;
	}
/*
	private void makeChangeFromFact(ChangeSet res, Fact f, char typ) {
		switch (f.type) {
		case PACKAGE:
			res.add(AtomicChange.makePackageChange(typ, f.params.get(0)));
			res.changecount[AtomicChange.ChangeTypes.ADDED_PACKAGE.ordinal()]++;
			break;
		case TYPE:
			res.add(AtomicChange.makeTypeChange(typ, f.params.get(0), f.params.get(1), f.params.get(2)));
			res.changecount[AtomicChange.ChangeTypes.ADDED_TYPE.ordinal()]++;
			//package is also modified
			res.add(AtomicChange.makePackageChange('M', f.params.get(2)));
			res.changecount[AtomicChange.ChangeTypes.MODIFIED_PACKAGE.ordinal()]++;
			break;
		case METHOD:
			res.add(AtomicChange.makeMethodChange(typ, f.params.get(0), f.params.get(1), f.params.get(2)));
			res.changecount[AtomicChange.ChangeTypes.ADDED_METHOD.ordinal()]++;
			//type is also modified
			res.add(AtomicChange.makeTypeChange('M', f.params.get(2), getChildFromFullName(f.params.get(2)),
					getParentFromFullName(f.params.get(2))));
			res.changecount[AtomicChange.ChangeTypes.ADDED_TYPE.ordinal()]++;
			break;
		case FIELD:
			res.add(AtomicChange.makeFieldChange(typ, f.params.get(0), f.params.get(1), f.params.get(2)));
			res.changecount[AtomicChange.ChangeTypes.ADDED_FIELD.ordinal()]++;
			//type is also modified
			res.add(AtomicChange.makeTypeChange('M', f.params.get(2), getChildFromFullName(f.params.get(2)),
					getParentFromFullName(f.params.get(2))));
			res.changecount[AtomicChange.ChangeTypes.ADDED_TYPE.ordinal()]++;
			break;
		case RETURN:
			res.add(AtomicChange.makeReturnsChange(typ, f.params.get(0), f.params.get(1)));
			res.changecount[AtomicChange.ChangeTypes.ADDED_RETURN.ordinal()]++;
			//method is also modified
			res.add(AtomicChange.makeMethodChange('M', f.params.get(0), getChildFromFullName(f.params.get(0)),
					getParentFromFullName(f.params.get(0))));
			res.changecount[AtomicChange.ChangeTypes.ADDED_METHOD.ordinal()]++;
			//type is also modified
			res.add(AtomicChange.makeTypeChange('M', getParentFromFullName(f.params.get(0)), 
					getChildFromFullName(getParentFromFullName(f.params.get(0))),
					getParentFromFullName(getParentFromFullName(f.params.get(0)))));
			res.changecount[AtomicChange.ChangeTypes.ADDED_TYPE.ordinal()]++;
			break;
		case FIELDOFTYPE:
			res.add(AtomicChange.makeFieldTypeChange(typ, f.params.get(0), f.params.get(1)));
			res.changecount[AtomicChange.ChangeTypes.ADDED_FIELDOFTYPE.ordinal()]++;
			//field is also modified
			res.add(AtomicChange.makeFieldChange('M', f.params.get(0), getChildFromFullName(f.params.get(0)),
					getParentFromFullName(f.params.get(0))));
			res.changecount[AtomicChange.ChangeTypes.ADDED_FIELD.ordinal()]++;
			//type is also modified
			res.add(AtomicChange.makeTypeChange('M', getParentFromFullName(f.params.get(0)), 
					getChildFromFullName(getParentFromFullName(f.params.get(0))),
					getParentFromFullName(getParentFromFullName(f.params.get(0)))));
			res.changecount[AtomicChange.ChangeTypes.ADDED_TYPE.ordinal()]++;
			break;
		case TYPEINTYPE:
			res.add(AtomicChange.makeTypeInTypeChange(typ, f.params.get(0), f.params.get(1)));
			res.changecount[AtomicChange.ChangeTypes.ADDED_TYPEINTYPE.ordinal()]++;
			//child types is also modified
			res.add(AtomicChange.makeTypeChange('M', f.params.get(0), 
					getChildFromFullName(f.params.get(0)),
					getParentFromFullName(f.params.get(0))));
			res.changecount[AtomicChange.ChangeTypes.ADDED_TYPE.ordinal()]++;
			//parent types is also modified
			res.add(AtomicChange.makeTypeChange('M', f.params.get(1), 
					getChildFromFullName(f.params.get(1)),
					getParentFromFullName(f.params.get(1))));
			res.changecount[AtomicChange.ChangeTypes.ADDED_TYPE.ordinal()]++;
			break;
		case SUBTYPE:
			res.add(AtomicChange.makeSubtypeChange(typ, f.params.get(0), f.params.get(1)));
			res.changecount[AtomicChange.ChangeTypes.ADDED_SUBTYPE.ordinal()]++;
			//parent types is also modified
			res.add(AtomicChange.makeTypeChange('M', f.params.get(0), 
					getChildFromFullName(f.params.get(0)),
					getParentFromFullName(f.params.get(0))));
			res.changecount[AtomicChange.ChangeTypes.ADDED_TYPE.ordinal()]++;
			//child types is also modified
			res.add(AtomicChange.makeTypeChange('M', f.params.get(1), 
					getChildFromFullName(f.params.get(1)),
					getParentFromFullName(f.params.get(1))));
			res.changecount[AtomicChange.ChangeTypes.ADDED_TYPE.ordinal()]++;
			break;
		case IMPLEMENTS:
			res.add(AtomicChange.makeImplementsChange(typ, f.params.get(0), f.params.get(1)));
			res.changecount[AtomicChange.ChangeTypes.ADDED_IMPLEMENTS.ordinal()]++;
			//parent types is also modified
			res.add(AtomicChange.makeTypeChange('M', f.params.get(0), 
					getChildFromFullName(f.params.get(0)),
					getParentFromFullName(f.params.get(0))));
			res.changecount[AtomicChange.ChangeTypes.ADDED_TYPE.ordinal()]++;
			//child types is also modified
			res.add(AtomicChange.makeTypeChange('M', f.params.get(1), 
					getChildFromFullName(f.params.get(1)),
					getParentFromFullName(f.params.get(1))));
			res.changecount[AtomicChange.ChangeTypes.ADDED_TYPE.ordinal()]++;
			break;
		case EXTENDS:
			res.add(AtomicChange.makeExtendsChange(typ, f.params.get(0), f.params.get(1)));
			res.changecount[AtomicChange.ChangeTypes.ADDED_EXTENDS.ordinal()]++;
			//parent types is also modified
			res.add(AtomicChange.makeTypeChange('M', f.params.get(0), 
					getChildFromFullName(f.params.get(0)),
					getParentFromFullName(f.params.get(0))));
			res.changecount[AtomicChange.ChangeTypes.ADDED_TYPE.ordinal()]++;
			//child types is also modified
			res.add(AtomicChange.makeTypeChange('M', f.params.get(1), 
					getChildFromFullName(f.params.get(1)),
					getParentFromFullName(f.params.get(1))));
			res.changecount[AtomicChange.ChangeTypes.ADDED_TYPE.ordinal()]++;
			break;
		case INHERITEDFIELD:
			res.add(AtomicChange.makeInheritedFieldChange(typ, f.params.get(0), f.params.get(1), f.params.get(2)));
			res.changecount[AtomicChange.ChangeTypes.ADDED_INHERITEDFIELD.ordinal()]++;
			//child type is also modified (parent is modified elsewhere)
			res.add(AtomicChange.makeTypeChange('M', f.params.get(2), 
					getChildFromFullName(f.params.get(2)),
					getParentFromFullName(f.params.get(2))));
			res.changecount[AtomicChange.ChangeTypes.ADDED_TYPE.ordinal()]++;
			break;
		case INHERITEDMETHOD:
			res.add(AtomicChange.makeInheritedMethodChange(typ, f.params.get(0), f.params.get(1), f.params.get(2)));
			res.changecount[AtomicChange.ChangeTypes.ADDED_INHERITEDMETHOD.ordinal()]++;
			//child type is also modified (parent is modified elsewhere)
			res.add(AtomicChange.makeTypeChange('M', f.params.get(2), 
					getChildFromFullName(f.params.get(2)),
					getParentFromFullName(f.params.get(2))));
			res.changecount[AtomicChange.ChangeTypes.ADDED_TYPE.ordinal()]++;
			break;
		case ACCESSES:
			res.add(AtomicChange.makeAccessesChange(typ, f.params.get(0), f.params.get(1)));
			res.changecount[AtomicChange.ChangeTypes.ADDED_ACCESSES.ordinal()]++;
			break;
		case CALLS:
			res.add(AtomicChange.makeCallsChange(typ, f.params.get(0), f.params.get(1)));
			res.changecount[AtomicChange.ChangeTypes.ADDED_CALLS.ordinal()]++;
			break;

		case METHODBODY:
			res.add(AtomicChange.makeMethodBodyChange(f.params.get(0), f.params.get(1)));
			res.changecount[AtomicChange.ChangeTypes.CHANGE_METHODBODY.ordinal()]++;
			//TODO: what else is modified?
			break;
		case METHODSIGNATURE:
			res.add(AtomicChange.makeMethodArgsChange(f.params.get(0), f.params.get(1)));
			res.changecount[AtomicChange.ChangeTypes.CHANGE_METHODSIGNATURE.ordinal()]++;
			//TODO: what else is modified?
			break;

		default:
			//catastrophic show-stopping disaster!
			break;
		}
	}
//*/
	//this is the new factbase, input is old factbase
	public ChangeSet diff(FactBase oldfacts) {

		ChangeSet res = new ChangeSet();
		FactBase added = new FactBase(this);
		added.removeAll(oldfacts);
		FactBase deleted = new FactBase(oldfacts);
		deleted.removeAll(this);

		//scan for additive changes (and modifications)
		for (Fact f : added) {
			res.add(AtomicChange.makeChange(Modifier.ADDED, f));
		}
		//scan for depletive changes
		for (Fact f : deleted) {
			res.add(AtomicChange.makeChange(Modifier.DELETED, f));
		}

		return res;
	}

	public void deriveRemoveExternalMethodsCalls() {
		System.out.print("Deriving remove external methods... ");

		//grab all bad method call facts
		FactBase badMethodCalls = new FactBase();
		for (Fact f : this) {
			if (f.type!=Fact.Types.CALLS) continue;
			if (f.params.get(1).startsWith("junit.framework"))
				badMethodCalls.add(f);
		}
		//remove method call facts
		this.removeAll(badMethodCalls);

		System.out.println("OK");
	}
	public void deriveDefaultConstructors() {
		System.out.print("Deriving default constructors... ");

		//for all classes without a constructor, create a default constructor and return

		//Optimize by using small sets instead of whole factbase
		Set<Fact> typefacts = new HashSet<Fact>();
		Set<Fact> methodfacts = new HashSet<Fact>();
		for (Fact f : this) {
			if (f.type==Fact.Types.TYPE)
				typefacts.add(f);
			else if (f.type==Fact.Types.METHOD)
				methodfacts.add(f);
		}

		//iterate over types
		for (Fact f : typefacts) {
			boolean found = false;
			if (f.getKind()==Fact.Kind.INTERFACE) continue;
			for (Fact f2 : methodfacts) {
				if (f2.params.get(1).startsWith("<init>(") && f2.params.get(2).equals(f.params.get(0))) { 
					found = true; break;
				}
			}
			if (!found) {
				Fact constfact = Fact.makeMethodFact(f.params.get(0)+"#<init>()", 
						"<init>()", f.params.get(0), Fact.Visibility.PUBLIC, f.filename, f.startposition, 1);
				Fact returnfact = Fact.makeReturnsFact(f.params.get(0)+"#<init>()", "void", 
						f.filename, f.startposition, 1) ;
				this.add(constfact);
				this.add(returnfact);
			}
		}
		System.out.println("OK");

	}
	public void deriveInheritedMembers() {
		//Init tyruba frontend
		FrontEnd fe = new FrontEnd(true, new File(MetaInfo.fdbDir), true,
						null, true, false);
		fe.setCacheSize(FrontEnd.defaultPagerCacheSize);

		//Read facts into fe
		try {
			fe.load(MetaInfo.included1kb.getAbsolutePath());
			fe.load(MetaInfo.derivationRules.getAbsolutePath());
		} catch (Exception e) {
			System.err.println(e);
			return;
		}
		for (Fact f : this) {
			boolean add = false;
			
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
					fe.parse(f+".");
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
		query = "inheritedmethod(?ms, ?t1, ?t2)";
		try {
			exp = fe.makeExpression(query);
			es = fe.frameQuery(exp);
			while (es.status() == ElementSource.ELEMENT_READY) {
				Frame frame = (Frame) es.nextElement();
				Fact f = Fact.makeInheritedMethodFact(frame.get(ms).toString(), frame.get(t1).toString(), frame.get(t2).toString(), "", 0, 0);
				add(f);
			}
		} catch (Exception e) {
		}
		//Add inherited fields
		query = "inheritedfield(?ms, ?t1, ?t2)";
		try {
			exp = fe.makeExpression(query);
			es = fe.frameQuery(exp);
			while (es.status() == ElementSource.ELEMENT_READY) {
				Frame frame = (Frame) es.nextElement();
				Fact f = Fact.makeInheritedFieldFact(frame.get(ms).toString(), frame.get(t1).toString(), frame.get(t2).toString(), "", 0, 0);
				add(f);
			}
		} catch (Exception e) {
		}
	}

	private void deriveInheritedMembers_best() {
		FactTree ft = new FactTree(this);
		ft.printTree();

		//from each node, propagate downwards
		FactBase newfacts = new FactBase();
		for (FactTree.FactTreeNode root : ft.roots) {
			climbTree(newfacts, ft, root);
		}
		newfacts.print(System.out);
		this.addAll(newfacts);
	}
	private static void climbTree(FactBase fb, FactTree ft, FactTree.FactTreeNode myclass) {
		if (myclass.supernode!=null) {
			FactBase newfacts =makeInheritedMembersFacts(myclass.supernode, myclass);
			myclass.facts.addAll(newfacts);
			fb.addAll(newfacts);
		}
		//climb children
		for (FactTree.FactTreeNode child : myclass.subnodes) {
			climbTree(fb, ft, child);
		}
	}
	private static FactBase makeInheritedMembersFacts(FactTree.FactTreeNode superclass, FactTree.FactTreeNode subclass) {
		FactBase res = new FactBase();

		for (Fact f : superclass.facts) {
			if (f.type==Types.METHOD) {
				if (f.getVisibility()==Fact.Visibility.PRIVATE) continue;
				if (f.params.get(1).startsWith("<init>(")) continue;
				boolean found = false;
				for (Fact subfact : subclass.facts) {
					if (subfact.type==Types.METHOD) {
						if (subfact.params.get(1).equals(f.params.get(1))) {
							found = true;
							continue;
						}
					}
				}
				if (found) continue;
				Fact newfact = Fact.makeInheritedMethodFact(f.params.get(1), superclass.typename,  
						subclass.typename, f.filename, f.startposition, f.length);
				res.add(newfact);
			}
			if (f.type==Types.FIELD) {
				if (f.getVisibility()==Fact.Visibility.PRIVATE) continue;
				boolean found = false;
				for (Fact subfact : subclass.facts) {
					if (subfact.type==Types.FIELD) {
						if (subfact.params.get(1).equals(f.params.get(1))) {
							found = true;
							continue;
						}
					}
				}
				if (found) continue;
				Fact newfact = Fact.makeInheritedFieldFact(f.params.get(1), superclass.typename,  
						subclass.typename, f.filename, f.startposition, f.length);
				res.add(newfact);
			}
			if (f.type==Types.INHERITEDMETHOD) {
				if (f.params.get(1).startsWith("<init>(")) continue;
				boolean found = false;
				for (Fact subfact : subclass.facts) {
					if (subfact.type==Types.METHOD) {
						if (subfact.params.get(1).equals(f.params.get(0))) {
							found = true;
							continue;
						}
					}
				}
				if (found) continue;
				Fact newfact = Fact.makeInheritedMethodFact(f.params.get(0), f.params.get(1),  
						subclass.typename, f.filename, f.startposition, f.length);
				res.add(newfact);
			}
			if (f.type==Types.INHERITEDFIELD) {
				boolean found = false;
				for (Fact subfact : subclass.facts) {
					if (subfact.type==Types.FIELD) {
						if (subfact.params.get(1).equals(f.params.get(0))) {
							found = true;
							continue;
						}
					}
				}
				if (found) continue;
				Fact newfact = Fact.makeInheritedFieldFact(f.params.get(0), f.params.get(1),  
						subclass.typename, f.filename, f.startposition, f.length);
				res.add(newfact);
			}
		}

		return res;
	}
	public void removeFactsOfFile(String file) {
		HashSet<Fact> erase = new HashSet<Fact>();
		for (Fact f : this) {
			if (f.filename.equals(file)) {
				erase.add(f);
			}
		}
		this.removeAll(erase);
	}
	
	public FactBase findRelevantSet(int numHops, Set<String> names) {
		FactBase res = new FactBase();
		Set<String> allNames = new HashSet<String>(names);
		Set<String> currNames = new HashSet<String>();
		Set<String> nextNames = new HashSet<String>(names);
		for (int n=0; n<numHops; ++n) {
			currNames = nextNames;
			nextNames = new HashSet<String>();
			for (Fact f : this) {
				for (int i=0; i<f.params.size(); ++i) {
					if (res.contains(f)) continue;
					if (currNames.contains(f.params.get(i))) {
						res.add(f);
						for (int j=0; j<f.params.size(); ++j) {
							String newname = f.params.get(j);
							if (!allNames.contains(newname)) {
								nextNames.add(newname);
								allNames.add(newname);
							}
						}
						break;
					}
				}
			}
		}
		return res;
	}
}
