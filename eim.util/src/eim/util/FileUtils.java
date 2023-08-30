package eim.util;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.oomph.setup.Installation;
import org.eclipse.oomph.setup.Workspace;

public class FileUtils {

	public static String getFileExtension(Path path) {
		String fileName = path.getFileName().toString();
		String[] extensions = fileName.split("\\.");
		return extensions[extensions.length - 1];
	}

	public static String getBasename(String s) {

		String separator = System.getProperty("file.separator");
		String filename;

		// Remove the path up to the filename.
		int lastSeparatorIndex = s.lastIndexOf(separator);
		if (lastSeparatorIndex == -1) {
			filename = s;
		} else {
			filename = s.substring(lastSeparatorIndex + 1);
		}

		// Remove the extension.
		int extensionIndex = filename.lastIndexOf(".");
		if (extensionIndex == -1)
			return filename;

		return filename.substring(0, extensionIndex);
	}

	/**
	 * Compares two installations by their path
	 * 
	 * @param inst1
	 * @param inst2
	 * @return boolean, true if they paths are equal, false otherwise
	 */
	public static boolean checkIfURIequals(Installation inst1, Installation inst2) {
		Path path1 = Paths.get(inst1.eResource().getURI().toFileString());
		Path path2 = Paths.get(inst2.eResource().getURI().toFileString());

		boolean result = false;
		if (path1.compareTo(path2) == 0) {
			result = true;
		}
		return result;
	}

	/**
	 * Compares two workspaces by their path
	 * 
	 * @param workspace1
	 * @param workspace2
	 * @return boolean, true if they paths are equal, false otherwise
	 */
	public static boolean checkIfURIequals(Workspace inst1, Workspace inst2) {
		Path path1 = Paths.get(inst1.eResource().getURI().toFileString());
		Path path2 = Paths.get(inst2.eResource().getURI().toFileString());

		boolean result = false;
		if (path1.compareTo(path2) == 0) {
			result = true;
		}
		return result;
	}
}
