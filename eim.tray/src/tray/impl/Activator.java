package tray.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import eim.api.EIMService;

public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		EIMService eimService = context.getService(context.getServiceReference(EIMService.class));
		DataController controller = new DataController(eimService);
		TrayApplication trayApp = new TrayApplication(eimService, controller);
		trayApp.activate(context);

	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}

}
