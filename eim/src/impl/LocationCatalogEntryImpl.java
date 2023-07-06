package impl;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.oomph.setup.Installation;
import org.eclipse.oomph.setup.Workspace;

import eim.api.LocationCatalogEntry;

public class LocationCatalogEntryImpl extends LocationCatalogEntry {

	private Integer id;
	private Installation installation;
	private Workspace workspace;
	private String[] tags;
	private Path installationPath;
	private Path workspacePath;
	private String installationFolderName;
	private String workspaceFolderName;

	public LocationCatalogEntryImpl(Integer id, Installation installation, Workspace workspace, String[] tags) {
		super(id, installation, workspace, tags);
		this.id = id;
		this.installation = installation;
		this.workspace = workspace;
		this.tags = tags;
		Path fullInstallationPath = Paths.get(installation.eResource().getURI().toFileString());
		Path fullWorkspacePath = Paths.get(workspace.eResource().getURI().toFileString());
		String osProp = System.getProperty("os.name");
		if (osProp.matches(".*Mac.*")) {
			Path installPath = fullInstallationPath.getParent().getParent().getParent().getParent().getParent();
			this.installationPath = installPath;
		} else {
			this.installationPath = fullInstallationPath.getParent().getParent().getParent();
		}
		this.workspacePath = fullWorkspacePath.getParent().getParent().getParent().getParent();
		this.installationFolderName = installationPath.getParent().toFile().getName();
		this.workspaceFolderName = workspacePath.toFile().getName();
	}

	@Override
	public int getID() {
		return id;
	}

	@Override
	public Installation getInstallation() {
		return installation;
	}

	@Override
	public Workspace getWorkspace() {
		return workspace;
	}

	@Override
	public Path getInstallationPath() {
		return installationPath;
	}

	@Override
	public Path getWorkspacePath() {
		return workspacePath;
	}

	@Override
	public String getInstallationFolderName() {
		return installationFolderName;
	}

	@Override
	public String getWorkspaceFolderName() {
		return workspaceFolderName;
	}

	@Override
	public String[] getTags() {
		return tags;
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

	@Override
	public int compareTo(LocationCatalogEntry anotherInstance) {
		int id2 = anotherInstance.getID();
		return (id < id2) ? -1 : ((id == id2) ? 0 : 1);
	}
}
