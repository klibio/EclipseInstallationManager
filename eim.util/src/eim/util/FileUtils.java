package eim.util;

import java.nio.file.Path;

public class FileUtils {
	
	public static String getFileExtension(Path path) {
		String fileName = path.getFileName().toString();
		String[] extensions = fileName.split("\\.");
		return extensions[extensions.length-1];
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
}
