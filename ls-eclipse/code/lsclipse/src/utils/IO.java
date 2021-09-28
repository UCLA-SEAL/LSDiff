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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

public class IO {

	public static String readStreamToString(InputStream in) throws IOException {
		StringBuffer out = new StringBuffer();
		byte[] b = new byte[4096];
		for (int n; (n = in.read(b)) != -1;) {
			out.append(new String(b, 0, n));
		}
		return out.toString();
	}

	public static void writeStreamToFile(InputStream is, File file) {
		try {
			OutputStream out = new FileOutputStream(file);
			byte buf[] = new byte[1024];
			int len;
			while ((len = is.read(buf)) > 0)
				out.write(buf, 0, len);
			out.close();
			is.close();
		} catch (IOException e) {
		}
	}

	public static void copyFile(File sourceFile, File destFile)
			throws IOException {
		if (!destFile.exists()) {
			destFile.createNewFile();
		}

		FileChannel source = null;
		FileChannel destination = null;
		try {
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		} finally {
			if (source != null) {
				source.close();
			}
			if (destination != null) {
				destination.close();
			}
		}
	}
	public static void appendToFile(String s, String path) {
		try {
			//First check if path exists -- if not, create it
			File f2KBfile = new File(path);
			File dir = f2KBfile.getParentFile();
			if (!dir.exists()) {
				dir.mkdirs();
			}
			//write away!
			BufferedWriter lsd2kbfile = new BufferedWriter(new FileWriter(path, true));
			lsd2kbfile.append(s);
			lsd2kbfile.close();
		} catch (IOException e) {
		}
		}
	public static void writeToFile(String s, String path) {
		try {
			//First check if path exists -- if not, create it
			File f2KBfile = new File(path);
			File dir = f2KBfile.getParentFile();
			if (!dir.exists()) {
				dir.mkdirs();
			}
			//write away!
			BufferedWriter lsd2kbfile = new BufferedWriter(new FileWriter(path));
			lsd2kbfile.write(s);
			lsd2kbfile.close();
		} catch (IOException e) {
		}
		}

}
