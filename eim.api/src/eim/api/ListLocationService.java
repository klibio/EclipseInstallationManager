package eim.api;

import java.util.Map;

import org.eclipse.oomph.setup.Installation;
import org.eclipse.oomph.setup.Workspace;
import org.eclipse.oomph.util.Pair;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface ListLocationService {
	
	public void listLocations(String locationFile);
	
	public Map<Integer, Pair<Installation, Workspace>> getLocationEntries();
}
