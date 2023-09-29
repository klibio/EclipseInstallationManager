package tray.impl;

import java.util.LinkedHashMap;
import java.util.LinkedList;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.oomph.setup.Installation;
import org.eclipse.oomph.setup.Workspace;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eim.api.EIMService;
import eim.api.LocationCatalogEntry;
import eim.util.FileUtils;

@Component(service=DataProvider.class)
public class DataProvider {

	private LinkedList<LocationCatalogEntry> locationEntries;
	private Logger logger = LoggerFactory.getLogger(DataProvider.class);
	private LinkedHashMap<LocationCatalogEntry, LinkedList<LocationCatalogEntry>> installationGroupedMap = new LinkedHashMap<>();
	private LinkedList<LocationCatalogEntry> uniqueInstallations = new LinkedList<>();
	private LinkedList<LocationCatalogEntry> uniqueWorkspaces = new LinkedList<>();

	IEclipsePreferences properties = InstanceScope.INSTANCE.getNode("tray.impl");
	Preferences eimPrefs = properties.node("eim.prefs");

	@Reference
	private EIMService eclService;

	@Activate
	public void activate() {
		dataInitialization();

		identifyUniqueInstallations();
		identifyUniqueWorkspaces();

		createMappedInstallationEntries();
	}
	
	/**
	 * Puts unique installations into a separate List
	 */
	private void identifyUniqueInstallations() {
		logger.debug("Filling unique installations list.");

		locationEntries.forEach(locationCatalogEntry -> {
			if (!checkIfListContainsInstallation(locationCatalogEntry, uniqueInstallations)) {
				uniqueInstallations.add(locationCatalogEntry);
			}
		});
	}
	/**
	 * Puts unique workspaces into a separate List
	 */
	private void identifyUniqueWorkspaces() {
		logger.debug("Filling unique workspaces list.");

		locationEntries.forEach(locationCatalogEntry -> {
			if (!checkIfListContainsWorkspace(locationCatalogEntry, uniqueWorkspaces)) {
				uniqueWorkspaces.add(locationCatalogEntry);
			}
		});
	}

	/**
	 * Creates a unique map, which maps unique installations to workspaces they were
	 * used with Note: A InstallationEntry can simultaneously represent an
	 * installation and a workspace Type of the Map is <LocationCatalogEntry,
	 * List<LocationCatalogEntry>>
	 */
	private void createMappedInstallationEntries() {
		logger.debug("creating installation - workspaces map");

		LinkedHashMap<LocationCatalogEntry, LinkedList<LocationCatalogEntry>> installationMap = new LinkedHashMap<>();

		for (LocationCatalogEntry installationEntry : uniqueInstallations) {
			LinkedList<LocationCatalogEntry> mappedWorkspaces = new LinkedList<>();
			locationEntries.forEach(locationCatalogEntry -> {
				if (FileUtils.checkIfURIequals(installationEntry.getInstallation(),
						locationCatalogEntry.getInstallation())) {
					mappedWorkspaces.add(locationCatalogEntry);
				}
			});
			installationMap.put(installationEntry, mappedWorkspaces);
		}

		installationGroupedMap = installationMap;

	}

	/**
	 * Helper method which checks if an entry already exists in a given list,
	 * compared by URI.
	 * 
	 * @param entry            A LocationCatalogEntry which is to be searched for in
	 *                         the list
	 * @param installationList the list that should be searched in
	 * @return boolean, True if installationList contains entry, false otherwise
	 */
	private boolean checkIfListContainsInstallation(LocationCatalogEntry entry,
			LinkedList<LocationCatalogEntry> installationList) {
		Installation installation1 = entry.getInstallation();
		boolean result = false;

		for (LocationCatalogEntry listEntry : installationList) {
			if (FileUtils.checkIfURIequals(installation1, listEntry.getInstallation())) {
				result = true;
			}
		}
		return result;
	}

	/**
	 * Helper method which checks if an entry already exists in a given list,
	 * compared by URI.
	 * 
	 * @param entry            A LocationCatalogEntry which is to be searched for in
	 *                         the list
	 * @param installationList the list that should be searched in
	 * @return boolean, True if installationList contains entry, false otherwise
	 */
	private boolean checkIfListContainsWorkspace(LocationCatalogEntry entry,
			LinkedList<LocationCatalogEntry> workspaceList) {
		Workspace workspace = entry.getWorkspace();
		boolean result = false;

		for (LocationCatalogEntry listEntry : workspaceList) {
			if (FileUtils.checkIfURIequals(workspace, listEntry.getWorkspace())) {
				result = true;
			}
		}
		return result;
	}

	/*
	 * Loads data into the service and fetches it
	 */
	private void dataInitialization() {
		logger.debug("Loading data");
		eclService.listLocations(null);
		locationEntries = eclService.getLocationEntries();
	}

	public LinkedHashMap<LocationCatalogEntry, LinkedList<LocationCatalogEntry>> getInstallationMap() {
		return installationGroupedMap;
	}

	public LinkedList<LocationCatalogEntry> getInstallations() {
		return uniqueInstallations;
	}

	public LinkedList<LocationCatalogEntry> getWorkspaces() {
		return uniqueWorkspaces;
	}
	
	/**
	 * Reloads and refreshes the data structures.
	 */
	public void refreshData() {
		eclService.refreshLocations();
		this.locationEntries = eclService.getLocationEntries();
		createMappedInstallationEntries();
	}

}
