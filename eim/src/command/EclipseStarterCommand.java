package command;

import java.util.LinkedList;

import org.apache.felix.service.command.Descriptor;
import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import eim.api.EIMService;
import eim.api.LocationCatalogEntry;

//@formatter:off
@Component(
		service = EclipseStarterCommand.class,
		property = {
				"osgi.command.scope=zEIM",
				"osgi.command.function=startProcess",
				"osgi.command.function=startEntry",
				"osgi.command.function=listLocations"
		})
//@formatter:on
@ConsumerType
public class EclipseStarterCommand {
	private EIMService eclService;

	@Reference
	public void bindEclipseService(EIMService eclsvc) {
		this.eclService = eclsvc;
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
		LinkedList<LocationCatalogEntry> locationEntries = eclService.getLocationEntries();
		LocationCatalogEntry entry = null;
		for (LocationCatalogEntry locationCatalogEntry : locationEntries) {
			if(locationCatalogEntry.getID() == index) {
				entry = locationCatalogEntry;
			}
		}

		eclService.startEntry(entry, true);
	}
	
		
	@Descriptor("List all available installations and workspaces done with the Eclipse Installer")
	public void listLocations() {
		eclService.listLocations(null);
	}
	
	@Descriptor("List all available installations and workspaces from a specific locations file")
	public void listLocations(String locationFile) {
		eclService.listLocations(locationFile);
	}

	
}
