package tray.impl;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EclipseApplication implements IApplication {
	Logger logger = LoggerFactory.getLogger(EclipseApplication.class);
	
	@Override
	public Object start(IApplicationContext context) throws Exception {
		// Purpose is only to enable eclipse osgi services
		logger.debug("Eclipse Services initialized");
		return null;
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		logger.debug("Closing EclipseApplication");
	}

}
