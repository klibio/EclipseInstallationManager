package eim.util;

public class SystemUtils {
	private static final String OS_NAME_WINDOWS_PREFIX = "Windows";
	public static final String OS_NAME = System.getProperty("os.name");
	public static final String OS_VERSION = System.getProperty("os.version");
	public static final boolean IS_OS_WINDOWS = getOsMatchesName(OS_NAME_WINDOWS_PREFIX);
	public static final boolean IS_OS_LINUX = getOsMatchesName("Linux") || getOsMatchesName("LINUX");
	public static final boolean IS_OS_MAC = getOsMatchesName("Mac");

	private static boolean getOsMatchesName(final String osNamePrefix) {
		return isOSNameMatch(OS_NAME, osNamePrefix);
	}

	static boolean isOSMatch(final String osName, final String osVersion, final String osNamePrefix,
			final String osVersionPrefix) {
		if (osName == null || osVersion == null) {
			return false;
		}
		return isOSNameMatch(osName, osNamePrefix) && isOSVersionMatch(osVersion, osVersionPrefix);
	}

	static boolean isOSNameMatch(final String osName, final String osNamePrefix) {
		if (osName == null) {
			return false;
		}
		return osName.startsWith(osNamePrefix);
	}

	static boolean isOSVersionMatch(final String osVersion, final String osVersionPrefix) {
		if (osVersion.isEmpty()) {
			return false;
		}
		// Compare parts of the version string instead of using
		// String.startsWith(String) because otherwise
		// osVersionPrefix 10.1 would also match osVersion 10.10
		final String[] versionPrefixParts = osVersionPrefix.split("\\.");
		final String[] versionParts = osVersion.split("\\.");
		for (int i = 0; i < Math.min(versionPrefixParts.length, versionParts.length); i++) {
			if (!versionPrefixParts[i].equals(versionParts[i])) {
				return false;
			}
		}
		return true;
	}
}
