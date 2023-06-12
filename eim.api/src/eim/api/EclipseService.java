package eim.api;

import java.util.Map;

import org.eclipse.oomph.setup.Installation;
import org.eclipse.oomph.setup.Workspace;
import org.eclipse.oomph.util.Pair;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface EclipseService {
	Process startProcess(String command, String workingDir, String[] args);
	
	void startEntry(Integer index, Map<Integer, Pair<Installation, Workspace>> executionEntries);
}
