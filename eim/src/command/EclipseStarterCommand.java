package command;

import java.util.Map;

import org.apache.felix.service.command.Descriptor;
import org.eclipse.oomph.setup.Installation;
import org.eclipse.oomph.setup.Workspace;
import org.eclipse.oomph.util.Pair;
import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import eim.api.EclipseService;
import eim.api.ListLocationService;

@Component(service = EclipseStarterCommand.class, property = { "osgi.command.scope=zEIM",
		"osgi.command.function=startProcess", "osgi.command.function=startEntry" })
@ConsumerType
public class EclipseStarterCommand {
	private EclipseService eclService;
	private ListLocationService listLocSvc;

	@Reference
	public void bindEclipseService(EclipseService eclsvc) {
		this.eclService = eclsvc;
	}

	@Reference
	public void bindLocationService(ListLocationService listLocSvc) {
		this.listLocSvc = listLocSvc;
	}

	@Descriptor("Start a process with a specific command.")
	public void startProcess(String command, String workingDir, String[] args) {
		System.out.println("Executing " + command);
		if (workingDir == "null") {
			workingDir = null;
		}
		if (args[0] == "null") {
			args = null;
		}
		eclService.startProcess(command, workingDir, args);
	}

	@Descriptor("Starts a process from the given list of entries")
	public void startEntry(Integer index) {
		Map<Integer, Pair<Installation, Workspace>> executionEntries = listLocSvc.getLocationEntries();
		eclService.startEntry(index, executionEntries);
	}
}
