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
package utils;

import java.sql.*;
import java.util.Hashtable;

import changetypes.AtomicChange;
import changetypes.AtomicChange.Modifier;
import changetypes.ChangeSet;
import changetypes.Fact;
import changetypes.FactBase;
import changetypes.Utils;

public class DB {

	//Default parameters
	final static String DB_URL = "jdbc:mysql://giraffe.ece.utexas.edu/bevdiff_jmeter";
	final static String DB_LOGIN = "bevdiff";
	final static String DB_PASSWORD = "longneck1";
	final static String TBLNAME_PROGRAM = "program";
	final static String TBLNAME_VERSION = "version";
	final static String TBLNAME_TESTCASE = "testcase";
	final static String TBLNAME_FAILURE = "testfailure";
	final static String TBLNAME_CM = "coveragematrix";
	final static String TBLNAME_CG = "callgraph";
	final static String TBLNAME_CCT = "calltree";
	final static String TBLNAME_DS = "diffstruct";
	final static String TBLNAME_FB = "factbase";

	private int programKey = -1;
	private int testCaseKey = -1;

	Connection myCon;

	//initialize the object for use
	public DB() throws Exception {
//		com.mysql.jdbc.Driver.class.newInstance();
		Class.forName("com.mysql.jdbc.Driver").newInstance();
		myCon = DriverManager.getConnection(DB_URL, DB_LOGIN, DB_PASSWORD);
	}

	public void close() throws Exception {
		myCon.close();
	}

	//write a value to table
	public boolean writeToTable(String tablename, String field, String value) {
		try {
			Statement myStmt = myCon.createStatement();
			myStmt.executeUpdate("insert into "+tablename+" ("+field+") values ("+value+")");
			myStmt.close();
		} catch (SQLException e) {
			System.err.print("SQL Error: insert into "+tablename+" ("+field+") values (\""+value+"\")");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	//write a partial record to table
	public boolean writeToTable(String tablename, String[] fields, String[] values) {
		String fieldstring = "";
		String valuestring = "";
		try {
			Statement myStmt = myCon.createStatement();
			for (int i=0; i<fields.length; ++i) {
				if (i>0) fieldstring+=", ";
				fieldstring+=""+fields[i]+"";
			}
			for (int i=0; i<values.length; ++i) {
				if (i>0) valuestring+=", ";
				valuestring+="\""+values[i]+"\"";
			}
			myStmt.executeUpdate("insert into "+tablename+" ("+fieldstring+") values ("+valuestring+")");
			myStmt.close();
		} catch (SQLException e) {
			System.err.print("SQL Error: insert into "+tablename+" ("+fieldstring+") values ("+valuestring+")");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	//write a complete record to table
	public boolean writeToTable(String tablename, String[] values) {
		String valuestring = "";
		try {
			for (int i=0; i<values.length; ++i) {
				if (i>0) valuestring+=", ";
				valuestring+="\""+values[i]+"\"";
			}
			Statement myStmt = myCon.createStatement();
			myStmt.executeUpdate("insert into "+tablename+" () values ("+valuestring+")");
			myStmt.close();
		} catch (SQLException e) {
			System.err.print("SQL Error: insert into "+tablename+" () values ("+valuestring+")");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private int getProgKey(String progname) {
		try {
			int result = -1;
			ResultSet res = executeQuery("select "+TBLNAME_PROGRAM+".KEY from "+TBLNAME_PROGRAM+" where NAME='"+progname+"'");
			if (res.next()) {
				result = res.getInt("KEY");
			}
			res.close();
			return result;
		}catch (Exception e) {
			return -1;
		}
	}

	private int getVersionKey(String progname1, String progname2) {
		try {
			int result = -1;
			ResultSet res = executeQuery("select "+TBLNAME_VERSION+".KEY from "+TBLNAME_VERSION+", "+
					TBLNAME_PROGRAM+" x, "+TBLNAME_PROGRAM+" y " +
					"where "+TBLNAME_VERSION+".program1=x.key and "+TBLNAME_VERSION+".program2=y.key " +
					"and x.name like '"+progname1+"' and y.name like '"+progname2+"'");
			if (res.next()) {
				result = res.getInt("KEY");
			}
			res.close();
			return result;
		}catch (Exception e) {
			return -1;
		}
	}

	//write a bunch of complete records to table
	private boolean writeToTable(String tablename, String[][] valueset) {
		int start, end;
		int blocksize = 2000;
		for (start=0; start<valueset.length; start+=blocksize) {
			end = Math.min(start+blocksize, valueset.length);
			String mystmt = "insert into "+tablename+" () values ";
			for (int i=start; i<end; ++i) {
				String[] values = valueset[i];
				String valuestring = "";
				for (int j=0; j<values.length; ++j) {
					if (j>0) valuestring+=", ";
					valuestring+="\""+values[j]+"\"";
				}
				if (i>start) mystmt+=", ";
				mystmt+="("+valuestring+")";
			}
			try {
				Statement myStmt = myCon.createStatement();
				myStmt.executeUpdate(mystmt);
				myStmt.close();
			} catch (SQLException e) {
				System.err.print("SQL Error: "+mystmt);
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	//clear table contents
	public boolean clearTable(String tablename) {
		try {
			Statement myStmt = myCon.createStatement();
			myStmt.executeUpdate("TRUNCATE TABLE "+tablename);
			myStmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

	//execute arbitrary SQL command
	public ResultSet executeQuery(String sqlcommand) throws SQLException {
		Statement myStmt = myCon.createStatement();
		return myStmt.executeQuery(sqlcommand);
	}
	
	public boolean executeUpdate(String sqlcommand) {
		try {
			Statement myStmt = myCon.createStatement();
			myStmt.executeUpdate(sqlcommand);
			myStmt.close();
			return true;
		} catch (SQLException e) {
			System.err.print("SQL Error: "+sqlcommand);
			e.printStackTrace();
			return false;
		}
	}

	public ChangeSet readChanges(String program1, String program2) {

		ChangeSet cs = new ChangeSet();

		int verkey = getVersionKey(program1, program2);

			try {
				ResultSet res = executeQuery("select * from "+TBLNAME_DS+" where PROGRAM='"+verkey+"'");
				while (res.next()) {
					AtomicChange.Modifier mod = AtomicChange.Modifier.values()[res.getInt("Mod")];
					Fact.Types type = Fact.Types.values()[res.getInt("Type")];
					String param1 = res.getString("Param1");
					String param2 = res.getString("Param2");
					String param3 = res.getString("Param3");
					String param4 = res.getString("Param4");
					String param5 = res.getString("Param5");
					String param6 = res.getString("Param6");

					switch (type) {
					case PACKAGE:
						cs.add(AtomicChange.makeChange(mod, 
								Fact.makePackageFact(param1, "", 0, 0)));
						break;
					case TYPE:
						cs.add(AtomicChange.makeChange(mod, 
						Fact.makeTypeFact(param1, param2, param3, Fact.Kind.CLASS, "", 0, 0)));
						break;
					case METHOD:
						cs.add(AtomicChange.makeChange(mod, 
						Fact.makeMethodFact(param1, param2, param3, Fact.Visibility.PUBLIC, "", 0, 0)));
					break;
					case FIELD:
						cs.add(AtomicChange.makeChange(mod, 
						Fact.makeFieldFact(param1, param2, param3, Fact.Visibility.PUBLIC, "", 0, 0)));
					break;
					case RETURN:
					break;
					case FIELDOFTYPE:
					break;
					case ACCESSES:
					break;
					case CALLS:
						cs.add(AtomicChange.makeChange(mod, 
						Fact.makeCallsFact(param1, param2, "", 0, 0)));
					break;
					case EXTENDS:
						break;
					case IMPLEMENTS:
						break;
					case INHERITEDFIELD:
					break;
					case INHERITEDMETHOD:
					break;
					case TYPEINTYPE:
					break;
					
					}
				}
					
				res.close();
			} catch (SQLException e) {
				return null;
			}

			return cs;
	}

	public void addChanges(String program, ChangeSet cs) {
		if (!checkProgramExists(program, "")) {
			addProgramInfo(program, "", "");
		}

		int progkey = getProgKey(program);
		if (progkey<0) return;
		
		//clear current program data
//		executeUpdate("delete from "+TBLNAME_DS+" where program="+progkey);

		String valueset[][] = new String[cs.size()][8];
		int counter = 0;
		for (AtomicChange ac : cs) {
			valueset[counter][0]=Integer.toString(progkey);
			valueset[counter][1]=Integer.toString(ac.mod.ordinal());
			valueset[counter][2]=Integer.toString(ac.fact.type.ordinal());
			for (int i=0; i<6; ++i) {
				if (i < ac.fact.params.size()) {
					valueset[counter][i+3]=ac.fact.params.get(i);
				} else {
					valueset[counter][i+3]="";
				}
			}
			counter++;
		}
		writeToTable(TBLNAME_DS, valueset);
	}

	public void addFacts(String program, FactBase fb) {
		if (!checkProgramExists(program, "")) {
			addProgramInfo(program, "", "");
		}

		int progkey = getProgKey(program);
		if (progkey<0) return;
		
		//clear current program data
		executeUpdate("delete from "+TBLNAME_FB+" where program="+progkey);

		int BLOCKSIZE = 500;
		int LIMIT = 300;
		String valueset[][] = new String[BLOCKSIZE][8];
		int counter = 0;
		for (Fact f : fb) {
			valueset[counter][0]=Integer.toString(progkey);
			valueset[counter][1]=Integer.toString(f.type.ordinal());
			for (int i=0; i<6; ++i) {
				if (i < f.params.size()) {
					String s = f.params.get(i);
					if (s.length()>LIMIT)
						s = s.substring(0, LIMIT);
					valueset[counter][i+2]=s;
				} else {
					valueset[counter][i+2]="";
				}
			}
			counter++;
			if (counter>=BLOCKSIZE) {
				writeToTable(TBLNAME_FB, valueset);
				counter=0;
			}
		}
		int l = valueset.length;
		writeToTable(TBLNAME_FB, valueset);
	}

	public boolean checkProgramExists(String programname, String version) {
		try {
			ResultSet res = executeQuery("select * from "+TBLNAME_PROGRAM+" where "
					+"Name='"+programname+"' and "
					+"Version='"+version+"'");
			boolean result = false;
			if (res.next()) result = true;
			res.close();
			return result;
		} catch (SQLException e) {
			return false;
		}
	}
	public void addProgramInfo(String name, String testsuite, String version) {
		String [] val = new String[3];
		val[0]=name;
		val[1]=testsuite;
		val[2]=version;
		String [] att = new String[3];
		att[0]="Name";
		att[1]="TestSuite";
		att[2]="Version";
		writeToTable(TBLNAME_PROGRAM, att, val);
	}
}
