package eim.api;

import java.nio.file.Path;

import org.eclipse.oomph.setup.Installation;
import org.eclipse.oomph.setup.Workspace;

abstract public class LocationCatalogEntry implements Comparable<LocationCatalogEntry> {
	Integer id;
	Installation installation;
	Workspace workspace;
	Path installationPath;
	Path workspacePath;
	String installationFolderName;
	String workspaceFolderName;
	String installationName;
	String workspaceName;

	String[] tags;

	public abstract int getID();

	public abstract Installation getInstallation();

	public abstract Workspace getWorkspace();

	public abstract String[] getTags();

	public LocationCatalogEntry(Integer id, Installation installation, Workspace workspace, String[] tags) {
		this.id = id;
		this.installation = installation;
		this.workspace = workspace;
		this.tags = tags;
	}

	public abstract String getInstallationFolderName();

	public abstract String getWorkspaceFolderName();

	public abstract Path getInstallationPath();

	public abstract Path getWorkspacePath();
	
	public abstract String getInstallationName();
	
	public abstract String getWorkspaceName();

	public abstract void setInstallationName(String name);
	
	public abstract void setWorkspaceName(String name);

	@Override
	public int compareTo(LocationCatalogEntry anotherInstance) {
		int id2 = anotherInstance.getID();
		return (id < id2) ? -1 : ((id == id2) ? 0 : 1);
	}

	@Override
	public int hashCode() {
		return this.id;
	}

	@Override
	public boolean equals(Object o) {
		if (getClass() != o.getClass()) {
			return false;
		}
		int result = this.compareTo((LocationCatalogEntry) o);
		if (result == 0) {
			return true;
		}
		return false;

	}
}
