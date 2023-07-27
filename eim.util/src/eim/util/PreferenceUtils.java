package eim.util;

import java.util.Arrays;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreferenceUtils {
	
	private static Logger logger = LoggerFactory.getLogger(PreferenceUtils.class);
	
	/**
	 * Checks if a preference in the eimPrefs preferences already exists
	 * @param key 
	 * @return True or False
	 */
	public static boolean checkIfPreferenceKeyExists(String key, Preferences prefs) {
		boolean result = false;
		try {
			String[] keys = prefs.keys();
			if (Arrays.asList(keys).contains(key)) {
				result = true;
			}
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}

		return result;
	}
	
	/**
	 * Saves a preference to the EIM Preferences 
	 * @param key for the new preference
	 * @param pref preference value
	 */
	public static void savePreference(String key, String pref, Preferences prefs) {
		logger.debug("Saving preference " + pref + " to settings.");
		prefs.put(key, pref);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			logger.error("Something went wrong writing the setting " + key);
			e.printStackTrace();
		}
	}
	
}
