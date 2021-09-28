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
/*
 * This visitor traverses an AST tree to collect structural facts about the AST
 * Facts collected include:
 * - Class name
 * - Class parent
 * - Methods in class (incl formal params)
 * - Method body (as string)
 */

package changetypes;

import java.io.PrintStream;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import changetypes.Fact.Kind;
import changetypes.Fact.Visibility;

public class ASTVisitorAtomicChange extends ASTVisitor {

	//Parse results
	public FactBase facts;

	//temp vars
	private Stack<IMethodBinding> mtbStack = new Stack<IMethodBinding>();
	private Stack<ITypeBinding> itbStack = new Stack<ITypeBinding>();
	private String filename;

	public ASTVisitorAtomicChange(String filename) {
		facts = new FactBase();
		this.filename = filename;
	}
	
	public void printFacts(PrintStream out) {
		facts.print(out);
	}

	private static String removeParameters(String name) {
		int index = name.indexOf('<');
		if (index <= 0)
			return name;
		else {
			return name.substring(0, index);
		}
	}

	private static Visibility getModifier(IBinding ib) {
		if ((ib.getModifiers() & Modifier.PUBLIC)>0) return Visibility.PUBLIC;
		else if ((ib.getModifiers() & Modifier.PROTECTED)>0) return Visibility.PROTECTED;
		else if ((ib.getModifiers() & Modifier.PRIVATE)>0) return Visibility.PRIVATE;
		else return Visibility.PACKAGE;
	}

	private static String getQualifiedName(ITypeBinding itb) {
		if (itb.isPrimitive()) {
			return itb.getName();
		} else if (itb.isArray()) {
			try {
				String suffix = "";
				for (int i=0; i<itb.getDimensions(); ++i) { suffix+="[]"; }
			return getQualifiedName(itb.getElementType())+suffix;
			} catch (NullPointerException e) {
				return null;
			}
		} else if (itb.isNullType()) {
			return "null";
		} else if (itb.isClass() || itb.isInterface()) {
			if (itb.isNested()) {
				String name = itb.getName();
				if (itb.isAnonymous()) {
					//first check if already inside an anon class
					String binname = itb.getBinaryName();
					int index = binname.indexOf('$');
					name = binname.substring(index+1, binname.length());
				}
				return getQualifiedName(itb.getDeclaringClass())+"#"+name;
			} else {
				try {
				String pkg = itb.getPackage().getName();
				String name = itb.getName();
//				if (!name.startsWith("Class<"))
				name = removeParameters(itb.getName());
				return pkg+"%."+name;
				} catch (NullPointerException e) {
					return null;
				}
			}
		} else {
			return "java.lang%.Object";	//default name when all else fails
		}
	}

	private static String getQualifiedName(IVariableBinding ivb) {
		try {
			String name = ivb.getName();
			return getQualifiedName(ivb.getDeclaringClass())+"#"+name;
		} catch (NullPointerException e) {
			return "";
		}
	}

	private static String getQualifiedName(IMethodBinding imb) {
		return getQualifiedName(imb.getDeclaringClass())+"#"+getSimpleName(imb);
	}

	private static String getSimpleName(ITypeBinding itb) {
		if (itb.isNested()) {
			if (itb.isAnonymous()) {
				String binname = itb.getBinaryName();
				int index = binname.indexOf('$');
				String name = binname.substring(index+1, binname.length());
				return getSimpleName(itb.getDeclaringClass())+"#"+name;
			} else {
				return getSimpleName(itb.getDeclaringClass())+"#"+itb.getName();
			}
		} else {
			return itb.getName();
		}
	}
	
	private static String getSimpleName(IMethodBinding imb) {
		try {
//			imb = imb.getMethodDeclaration();
			String name = imb.getName();
			if (imb.isConstructor())
				name = "<init>";
			String args = "";
			for (ITypeBinding itb : imb.getParameterTypes()) {
				if (args.length()>0) args+=",";
				args += getQualifiedName(itb);
			}
			args = "(" + args + ")";
			return name+args;
		} catch (NullPointerException e) {
			return "";
		}
	}

	public boolean visit(PackageDeclaration node) {
		try {
			facts.add(Fact.makePackageFact(node.getName().toString(), filename, node.getStartPosition(), node.getLength()));
		} catch (Exception e){
			System.err.println("Cannot resolve bindings for package "+node.getName().toString());
		}
		return false;
	}

	public boolean visit(TypeDeclaration node) {
		ITypeBinding itb = node.resolveBinding();
		itbStack.push(itb);

		//make class facts
		try {
			facts.add(Fact.makeTypeFact(getQualifiedName(itb), getSimpleName(itb), 
					itb.getPackage().getName(), itb.isInterface()?Kind.INTERFACE:Kind.CLASS,
							filename, node.getStartPosition(), node.getLength()));
		} catch (Exception e){
			System.err.println("Cannot resolve bindings for class "+node.getName().toString());
		}

		//make inner type facts for self
		try {
			if (itb.isNested()) {
				facts.add(Fact.makeTypeInTypeFact(getQualifiedName(itb), getQualifiedName(itb.getDeclaringClass()),
						filename, node.getStartPosition(), 1));
			}
		} catch (Exception e) {
			System.err.println("Cannot resolve inner type for anonymous class "+itb.getName());
		}

		//make super type facts
		try {
			ITypeBinding itb2  = itb.getSuperclass();
			if (itb.getSuperclass()!=null) {
				facts.add(Fact.makeExtendsFact(getQualifiedName(itb2), getQualifiedName(itb),
						filename, node.getStartPosition(), 1));
			}
			if (node.isInterface()) {
				for (ITypeBinding i2 : itb.getInterfaces()) {
					facts.add(Fact.makeExtendsFact(getQualifiedName(i2), getQualifiedName(itb),
							filename, node.getStartPosition(), 1));
				}
			} else {
				for (ITypeBinding i2 : itb.getInterfaces()) {
					facts.add(Fact.makeImplementsFact(getQualifiedName(i2), getQualifiedName(itb),
							filename, node.getStartPosition(), 1));
				}
			}
		} catch (Exception e) {
			System.err.println("Cannot resolve super class bindings for class "+node.getName().toString());
		}

		return true;
	}
	public void endVisit(TypeDeclaration node) {
		itbStack.pop();
	}
	public boolean visit(AnonymousClassDeclaration node) {
		ITypeBinding itb = node.resolveBinding();
		itbStack.push(itb);

		//make class facts
		try {
			facts.add(Fact.makeTypeFact(getQualifiedName(itb), getSimpleName(itb), 
					itb.getPackage().getName(), itb.isInterface()?Kind.INTERFACE:Kind.CLASS,
							filename, node.getStartPosition(), node.getLength()));
		} catch (Exception e){
			System.err.println("Cannot resolve bindings for anonymous class "+itb.getName());
		}

		//make inner type facts for self
		try {
			if (itb.isNested()) {
				facts.add(Fact.makeTypeInTypeFact(getQualifiedName(itb), getQualifiedName(itb.getDeclaringClass()),
						filename, node.getStartPosition(), 1));
			}
		} catch (Exception e) {
			System.err.println("Cannot resolve inner type for anonymous class "+itb.getName());
		}

		//make super type facts
		try {
			try {
				facts.add(Fact.makeExtendsFact(getQualifiedName(itb.getSuperclass()), getQualifiedName(itb),
						filename, node.getStartPosition(), 1));
			} catch (NullPointerException e) {
				return false;
			}
			for (ITypeBinding i2 : itb.getInterfaces()) {
				try {
					facts.add(Fact.makeImplementsFact(getQualifiedName(i2), getQualifiedName(itb),
							filename, node.getStartPosition(), 1));
				} catch (NullPointerException e) {
					return false;
				}
			}
		} catch (Exception e) {
			System.err.println("Cannot resolve super class bindings for anonymous class "+itb.getName());
		}

		//make fields facts
		try {
			for (IVariableBinding ivb : itb.getDeclaredFields()) {
				Visibility visibility = getModifier(ivb);
//				facts.add(Fact.makeFieldFact(getQualifiedName(ivb), ivb.getName(), getQualifiedName(itb), visibility));
//				facts.add(Fact.makeFieldTypeFact(getQualifiedName(ivb), getQualifiedName(ivb.getType())));
			}
		} catch (Exception e) {
			System.err.println("Cannot resolve field bindings for anonymous class "+itb.getName());
		}

		return true;
	}
	public void endVisit(AnonymousClassDeclaration node) {
		itbStack.pop();
	}
	public boolean visit(FieldDeclaration node) {
		if (itbStack.size()==0) 
			return false;	//not in a class

		ITypeBinding itb = itbStack.peek();
		List<VariableDeclarationFragment> vars = node.fragments();

		//make fields facts
		try {
			for (VariableDeclarationFragment varnode : vars) {
				IVariableBinding ivb = varnode.resolveBinding();
				Visibility visibility = getModifier(ivb);
				facts.add(Fact.makeFieldFact(getQualifiedName(ivb), ivb.getName(), getQualifiedName(itb), visibility,
						filename, varnode.getStartPosition(), varnode.getLength()));
				facts.add(Fact.makeFieldTypeFact(getQualifiedName(ivb), getQualifiedName(ivb.getType()),
						filename, varnode.getStartPosition(), varnode.getLength()));
				}
		} catch (Exception e) {
			System.err.println("Cannot resolve field bindings for '"+node.toString()+"'");
		}

		return true;
	}
	public boolean visit(MethodDeclaration node) {

		IMethodBinding mtb = node.resolveBinding();
		mtbStack.push(mtb);

		//make method fact
		try {
			Visibility visibility = getModifier(mtb);
			facts.add(Fact.makeMethodFact(getQualifiedName(mtb), getSimpleName(mtb), 
					getQualifiedName(mtb.getDeclaringClass()), visibility,
					filename, node.getStartPosition(), node.getLength()));
		} catch (Exception e) {
			System.err.println("Cannot resolve return method bindings for method "+node.getName().toString());
		}

		//make return type fact
		try {
			String returntype = getQualifiedName(mtb.getReturnType());
			facts.add(Fact.makeReturnsFact(getQualifiedName(mtb), returntype,
					filename, node.getStartPosition(), 1));
		} catch (Exception e) {
			System.err.println("Cannot resolve return type bindings for method "+node.getName().toString());
		}

		//TODO: Method body and method args facts
/*		
		String name = node.getName().getIdentifier();
		String argstr = "";
		for (Object o : node.parameters()) {
			String paraname = ((SingleVariableDeclaration)o).getName().toString();
			String paratype = ((SingleVariableDeclaration)o).getType().toString();
			if (!argstr.contentEquals("")) argstr+=",";
			argstr += paratype+":"+paraname;
		}
		name+="("+argstr+")";
		argstr += "->"+returntype;
		facts.add(Fact.makeMethodFact(currclassfull+"."+name, name, currclassfull));
		facts.add(Fact.makeMethodArgsFact(currclassfull+"."+name, argstr));
		String bodystring = node.getBody()!=null?node.getBody().toString():"";
		facts.add(Fact.makeMethodBodyFact(currclassfull+"."+name, bodystring));
*/

		return true;
	}
	public void endVisit(MethodDeclaration node) {
		mtbStack.pop();
	}
	public boolean visit(FieldAccess node) {
		IVariableBinding ivb = node.resolveFieldBinding();

		if (mtbStack.isEmpty()) //not part of a method
			return true;

		IMethodBinding mtb = mtbStack.peek();
		
		//make field access fact
		try {
			//special case: if field access is on length field of an array, ignore
			if (node.getName().toString().equals("length") && ivb.getDeclaringClass()==null) {
				//continue
			} else { //otherwise proceed as normal
				facts.add(Fact.makeAccessesFact(getQualifiedName(node.resolveFieldBinding()), getQualifiedName(mtb),
						filename, node.getStartPosition(), node.getLength()));
			}
		} catch (Exception e) {
			System.err.println("Cannot resolve field access \""+node.getName().toString()+"\"");
		}
		return true;
	}
	public boolean visit(SimpleName node) {
		if (mtbStack.isEmpty() && !itbStack.isEmpty()) { //not part of a method
			return false;
/*			try {
				return visitName(node.resolveBinding(), 
						anonClassName.equals("")?getQualifiedName(itb):anonClassName);
			} catch (Exception e) {
				System.err.println("Cannot resolve simple name \""+node.getFullyQualifiedName().toString()+"\"");
				return false;
			}
*/
		} else if (!mtbStack.isEmpty()){
			if (node.getIdentifier().equals("length")) 
				return false;
			try {
				return visitName(node.resolveBinding(), getQualifiedName(mtbStack.peek()), node);
			} catch (Exception e) {
				System.err.println("Cannot resolve simple name \""+node.getFullyQualifiedName().toString()+"\"");
				return false;
			}
		}
		return false;
	}
	public boolean visit(QualifiedName node) {
		if (mtbStack.isEmpty() && !itbStack.isEmpty()) { //not part of a method
			return false;
		} else if (!mtbStack.isEmpty()){
			if (node.getName().getIdentifier().equals("length")) {
				return true;
			}
			try {
				return visitName(node.resolveBinding(), getQualifiedName(mtbStack.peek()), node);
			} catch (Exception e) {
				System.err.println("Cannot resolve qualified name \""+node.getFullyQualifiedName().toString()+"\"");
				return false;
			}
		}
		return false;
	}
	private boolean visitName(IBinding ib, String ownerFullName, ASTNode node) throws Exception {
		switch (ib.getKind()) {
		case IBinding.VARIABLE:
			IVariableBinding ivb = (IVariableBinding)ib; 
			if (ivb.isField()) {
				facts.add(Fact.makeAccessesFact(getQualifiedName(ivb),ownerFullName,
						filename, node.getStartPosition(), node.getLength()));
			}
			break;
		default: break;
		}
		return true;

/*
		IJavaElement ije = null;
		ije = ib.getJavaElement();
		if (ije==null) return false;
		try {
			//if reference to a field
			if (ije instanceof ResolvedBinaryField || ije instanceof ResolvedSourceField) {
				NamedMember nm = (NamedMember) ije;
				String fieldname = nm.getDeclaringType().getPackageFragment().getElementName()+
										"%."+nm.getDeclaringType().getTypeQualifiedName('#')+
										"#"+nm.getElementName();
				facts.add(Fact.makeAccessesFact(fieldname,
						ownerFullName));
			} else if (ije instanceof ResolvedBinaryMethod || ije instanceof ResolvedSourceMethod) { //if reference is to a method
				//do nothing for now
			}
		} catch (Exception e) {
			System.out.println("No good 2!"+ib.getName());
			return false;
		}
		return false;
*/
	}
	public boolean visit(MethodInvocation node) {
		IMethodBinding mmtb = node.resolveMethodBinding();

		if (mtbStack.isEmpty()) //not part of a method
			return true;

		//make field access fact
		try {
//* I do not know if this fix is necessary. Check JQuery behavior. For now use a quick hack.
			if (node.getExpression()!=null &&
					mmtb.getDeclaringClass().getQualifiedName().startsWith("java.awt.geom.Path2D")
/*					&& (mmtb.getName().equals("getPathIterator") || 
							mmtb.getName().equals("lineTo") || 
							mmtb.getName().equals("moveTo") || 
							mmtb.getName().equals("closePath"))*/) {
				Expression e = node.getExpression();
				ITypeBinding itb = e.resolveTypeBinding();
				facts.add(Fact.makeCallsFact(getQualifiedName(mtbStack.peek()), 
						getQualifiedName(itb)+"#"+getSimpleName(mmtb),
						filename, node.getStartPosition(), node.getLength()));
			} else {
				facts.add(Fact.makeCallsFact(getQualifiedName(mtbStack.peek()), getQualifiedName(mmtb),
						filename, node.getStartPosition(), node.getLength()));
			}
//*/
//			facts.add(Fact.makeCallsFact(getQualifiedName(mtbStack.peek()), 
//					getQualifiedName(mmtb)));
		} catch (Exception e) {
			System.err.println("Cannot resolve method invocation \""+node.getName().toString()+"\"");
		}
		return true;
	}
	public boolean visit(SuperMethodInvocation node) {
		IMethodBinding mmtb = node.resolveMethodBinding();

		if (mtbStack.isEmpty()) //not part of a method
			return true;

		//make field access fact
		try {
			facts.add(Fact.makeCallsFact(getQualifiedName(mtbStack.peek()), getQualifiedName(mmtb),
					filename, node.getStartPosition(), node.getLength()));
		} catch (Exception e) {
			System.err.println("Cannot resolve method invocation \""+node.getName().toString()+"\"");
		}
		return true;
	}
	public boolean visit(ClassInstanceCreation node) {
		IMethodBinding mmtb = node.resolveConstructorBinding();

		if (mtbStack.isEmpty()) //not part of a method
			return true;

		//make field access fact
		try {
			facts.add(Fact.makeCallsFact(getQualifiedName(mtbStack.peek()), getQualifiedName(mmtb),
					filename, node.getStartPosition(), node.getLength()));
		} catch (Exception e) {
			System.err.println("Cannot resolve class instance creation \""+node.getName().toString()+"\"");
		}
		return true;
	}
	public boolean visit(ConstructorInvocation node) {
		IMethodBinding mmtb = node.resolveConstructorBinding();

		if (mtbStack.isEmpty()) //not part of a method
			return true;

		//make field access fact
		try {
			facts.add(Fact.makeCallsFact(getQualifiedName(mtbStack.peek()), getQualifiedName(mmtb),
					filename, node.getStartPosition(), node.getLength()));
		} catch (Exception e) {
			System.err.println("Cannot resolve constructor invocation in \""+"\"");
		}
		return true;
	}
	public boolean visit(SuperConstructorInvocation node) {
		IMethodBinding mmtb = node.resolveConstructorBinding();

		if (mtbStack.isEmpty()) //not part of a method
			return true;

		//make field access fact
		try {
			facts.add(Fact.makeCallsFact(getQualifiedName(mtbStack.peek()), getQualifiedName(mmtb),
					filename, node.getStartPosition(), node.getLength()));
		} catch (Exception e) {
			System.err.println("Cannot resolve super constructor invocation in \""+"\"");
		}
		return true;
	}
}
