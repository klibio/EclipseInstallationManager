package eim.api;

import java.nio.file.Path;
import java.util.LinkedList;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface EIMService {
	Process startProcess(String command, String workingDir, String[] args);

	void startEntry(LocationCatalogEntry entryToExecute);

	public void listLocations(String locationFile);

	public LinkedList<LocationCatalogEntry> getLocationEntries();

	public void refreshLocations();

	public void renameWorkspace(LocationCatalogEntry entry, String name);

	public void renameInstallation(LocationCatalogEntry entry, String name);
	
	public void deleteWorkspace(Path workspacePath);
	
	public void deleteInstallation(Path installationPath);
}
