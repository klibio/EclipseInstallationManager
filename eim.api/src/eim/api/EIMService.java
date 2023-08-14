package eim.api;

import java.util.LinkedList;

import org.eclipse.oomph.setup.Installation;
import org.eclipse.oomph.setup.Workspace;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface EIMService {
	Process startProcess(String command, String workingDir, String[] args);

	void startEntry(LocationCatalogEntry entryToExecute);
	
	public void listLocations(String locationFile);
	
	public LinkedList<LocationCatalogEntry> getLocationEntries();
	
	public void refreshLocations();
	
	public void renameWorkspace(Workspace workspace, String name);
	public void renameInstallation(Installation installation, String name);
}
