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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class WS {
    public static Set<String> findChangedFilesByDiff(String project1, String project2) throws IOException, CoreException {
    	Set<String> res = new HashSet<String>();
    	IWorkspaceRoot ws = ResourcesPlugin.getWorkspace().getRoot();

    	String proj1root;
    	String proj2root;
    	Set<IFile> proj1files = null;
    	Set<IFile> proj2files = null;

    	//find all files in proj1
		proj1files = getIFiles(project1);
		proj1root = ws.getProject(project1).getFullPath().toString();
			
    	//find all files in proj2
		proj2files = getIFiles(project2);
		proj2root = ws.getProject(project2).getFullPath().toString();

		//find files which occur in both projects and are different
		@SuppressWarnings("unused")
		Set<IFile> files = new HashSet<IFile>();
		for (IFile file1 : proj1files) {
			//convert file path in project 1 to path for project 2
			IPath relativepath = file1.getProjectRelativePath();
			IFile file2 = null;
			//search for file in project 2
			for (IFile currfile : proj2files) {
				if (currfile.getProjectRelativePath().equals(relativepath)) {
					file2 = currfile;
					break;
				}
			}
			//do not add if this file appears in project 2 and is identical
			if (file2!=null) {	//this file does appears in project 2
				//do diff on the two files
				String source1 = IO.readStreamToString(file1.getContents());
				String source2 = IO.readStreamToString(file2.getContents());
				if (source1.equals(source2))
					continue; //these files have identical contents
			}
			//add relative path to result
			res.add(relativepath.toPortableString());
		}
		for (IFile file1 : proj2files) {
			//convert file path in project 1 to path for project 2
			IPath relativepath = file1.getProjectRelativePath();
			IFile file2 = null;
			//search for file in project 2
			for (IFile currfile : proj1files) {
				if (currfile.getProjectRelativePath().equals(relativepath)) {
					file2 = currfile;
					break;
				}
			}
			//do not add if this file appeared in project 1
			if (file2!=null) {	//this file does appears in project 1
				continue;
			}
			//add relative path to result
			res.add(relativepath.toPortableString());
		}

    	return res;
    }
	public static Set<String> getFiles(String projname) {
    	IWorkspaceRoot ws = ResourcesPlugin.getWorkspace().getRoot();
		IProject proj = ws.getProject(projname);
		IFile f = proj.getFile(new Path("src/geometry/Point.java"));
		try {
			IResource reses[] = proj.members(true);
			int x = reses.length;
		} catch (CoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		boolean ex = f.exists();
		boolean op = proj.isOpen();
		final Set<String> files = new HashSet<String>(); 
		try {
			proj.accept(new IResourceVisitor() {
				public boolean visit(IResource arg0) throws CoreException {
					if (arg0.getType()==IResource.FILE) {
						if (arg0.getName().endsWith(".java"))
							files.add(((IFile) arg0).getProjectRelativePath().toPortableString());
						return false;
					}
					return true;
				}
			});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return files;
	}
	public static Set<IFile> getIFiles(String projname) {
    	IWorkspaceRoot ws = ResourcesPlugin.getWorkspace().getRoot();
		IProject proj = ws.getProject(projname);
		final Set<IFile> files = new HashSet<IFile>(); 
		try {
			if (!proj.isOpen())
				proj.open(null);
			proj.accept(new IResourceVisitor() {
				public boolean visit(IResource arg0) throws CoreException {
					if (arg0.getType()==IResource.FILE) {
						if (arg0.getName().endsWith(".java"))
							files.add((IFile) arg0);
						return false;
					}
					return true;
				}
			});
		} catch (Exception e) {
			int x = 14;
			
		}
		return files;
	}

}
