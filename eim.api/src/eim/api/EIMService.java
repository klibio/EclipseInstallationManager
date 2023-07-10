package eim.api;

import java.util.LinkedList;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface EIMService {
	Process startProcess(String command, String workingDir, String[] args);

	void startEntry(LocationCatalogEntry entryToExecute);
	
	public void listLocations(String locationFile);
	
	public LinkedList<LocationCatalogEntry> getLocationEntries();
	
	public void refreshLocations();
}
