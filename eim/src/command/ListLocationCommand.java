package command;

import org.apache.felix.service.command.Descriptor;
import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import eim.api.ListLocationService;

@Component(property = { "osgi.command.scope=zEIM",  
"osgi.command.function=listLocations" }, service = ListLocationCommand.class)
@ConsumerType
public class ListLocationCommand {
	
	private ListLocationService listService;
	
	@Reference
	public void bindListLocationService(ListLocationService listSvc) {
		this.listService = listSvc;
	}
	
	@Descriptor("List all available installations and workspaces done with the Eclipse Installer")
	public void listLocations() {
		listService.listLocations(null);
	}
	
	@Descriptor("List all available installations and workspaces from a specific locations file")
	public void listLocations(String locationFile) {
		listService.listLocations(locationFile);
	}
}
