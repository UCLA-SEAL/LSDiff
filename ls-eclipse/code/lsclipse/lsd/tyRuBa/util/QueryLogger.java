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
 * Created on Jul 15, 2004
 */
package tyRuBa.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import tyRuBa.engine.RBCompoundTerm;
import tyRuBa.engine.RBConjunction;
import tyRuBa.engine.RBCountAll;
import tyRuBa.engine.RBDisjunction;
import tyRuBa.engine.RBExistsQuantifier;
import tyRuBa.engine.RBExpression;
import tyRuBa.engine.RBFindAll;
import tyRuBa.engine.RBIgnoredVariable;
import tyRuBa.engine.RBModeSwitchExpression;
import tyRuBa.engine.RBNotFilter;
import tyRuBa.engine.RBPair;
import tyRuBa.engine.RBPredicateExpression;
import tyRuBa.engine.RBQuoted;
import tyRuBa.engine.RBTemplateVar;
import tyRuBa.engine.RBTestFilter;
import tyRuBa.engine.RBTuple;
import tyRuBa.engine.RBUniqueQuantifier;
import tyRuBa.engine.RBVariable;
import tyRuBa.engine.visitor.ExpressionVisitor;
import tyRuBa.engine.visitor.TermVisitor;

public abstract class QueryLogger {
	
    public QueryLogger() {}
    
    public abstract void close(); 
    
    public abstract void logQuery(RBExpression query);
}
