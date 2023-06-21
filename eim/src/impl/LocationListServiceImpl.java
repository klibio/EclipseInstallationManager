package impl;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.oomph.setup.Installation;
import org.eclipse.oomph.setup.LocationCatalog;
import org.eclipse.oomph.setup.Workspace;
import org.eclipse.oomph.setup.internal.core.util.SetupCoreUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eim.api.ListLocationService;
import org.eclipse.oomph.util.Pair;

@Component
public class LocationListServiceImpl implements ListLocationService {
	
	private static Logger logger = LoggerFactory.getLogger(LocationListServiceImpl.class);
	private Map<Integer, Pair<Installation, Workspace>> executionMap = new HashMap<>();

	@Override
	public void listLocations(String locationFile) {
		File file;
		if(locationFile == null || locationFile.isBlank()) {
			logger.debug("Utilizing the default locations.setup file of the Eclipse Installer.");
			String oomphhome = System.getProperty("user.home", System.getenv("HOME"))
					.concat("/.eclipse/org.eclipse.oomph.setup");
			String setupfile = oomphhome + "/setups/locations.setup";
			file = new File(setupfile);
		} else {
			logger.debug("Loading locations catalog from " + locationFile);
			file = new File(locationFile);
		}
		
		// refresh map entries
		try {
			fetchEntries(file);
		} catch (Exception e) {
			logger.error("The given file " + locationFile + " is not a valid locationCatalog.\n Please check if that is the correct file!");
			e.printStackTrace();
		}
		
		//print map details
		for (Map.Entry<Integer, Pair<Installation, Workspace>> entry : executionMap.entrySet()) {
			Integer key = entry.getKey();
			Pair<Installation, Workspace> val = entry.getValue();
			Path instPath = Paths.get(val.getElement1().eResource().getURI().toFileString()).getParent().getParent()
					.getParent();
			Path wrkspcPath = Paths.get(val.getElement2().eResource().getURI().toFileString()).getParent().getParent().getParent()
					.getParent();
			System.out.format("Launch entry Number %s\n"
					+ "Location: <%s>\n"
					+ "Workspace: <%s>\n", key, instPath,
					wrkspcPath);
		}

	}

	private Resource loadLocationResource(File file) throws RuntimeException {
		URI uri = URI.createFileURI(file.getAbsolutePath());
		logger.debug("Loading locations catalog from " + uri);

		ResourceSet resourceSet = SetupCoreUtil.createResourceSet();

		Resource resource = resourceSet.getResource(uri, true);
		logger.debug("Loaded locations catalog from " + uri);
		
		return resource;
	}

	public void fetchEntries(File file) throws Exception {
		Map<Integer, Pair<Installation, Workspace>> executionMapTemp = new HashMap<>();
		try {
			Resource resource = loadLocationResource(file);
			
			// iterate contents of the loaded resource.
			int i = 1;
			for (EObject eObject : resource.getContents()) {
				if (eObject instanceof LocationCatalog) {
					LocationCatalog catalog = (LocationCatalog) eObject;
					System.out.println("# Installations");
					Iterator<Entry<Installation, EList<Workspace>>> instIterator = catalog.getInstallations().iterator();
					while (instIterator.hasNext()) {
						Entry<Installation, EList<Workspace>> instEntry = instIterator.next();
						Installation inst = instEntry.getKey();
						Resource eResource = inst.eResource();
						if (eResource != null) {
							EList<Workspace> wrkspcList = instEntry.getValue();
							for (Workspace wrkspc : wrkspcList) {
								Pair<Installation, Workspace> entry = new Pair<Installation, Workspace>(inst, wrkspc);
								executionMapTemp.put(i++, entry);
							}
						} else {
							System.out.format("## installation <%s>\n", inst.getName());
						}
					}
					executionMap = executionMapTemp;
				} else {
					throw new Exception("The given file is not a LocationCatalog!");
				}
			}
			
		} catch (RuntimeException exception) {
			exception.printStackTrace();
		}

		
	}

	public Map<Integer, Pair<Installation, Workspace>> getLocationEntries() {
		return executionMap;
	}
}
