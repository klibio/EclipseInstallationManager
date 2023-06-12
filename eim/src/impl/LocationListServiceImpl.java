package impl;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
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
import org.osgi.service.component.annotations.Component;

import eim.api.ListLocationService;

@Component
public class LocationListServiceImpl implements ListLocationService {

	@Override
	public void listLocations() {
		String oomphhome = System.getProperty("user.home", System.getenv("HOME"))
				.concat("/.eclipse/org.eclipse.oomph.setup");
		String setupfile = oomphhome + "/setups/locations.setup";
		File file = new File(setupfile);
		URI uri = URI.createFileURI(file.getAbsolutePath());
		System.out.println("Loading EMF model " + uri);

		ResourceSet resourceSet = SetupCoreUtil.createResourceSet();

		try {
			// load resource for this file //
			Resource resource = resourceSet.getResource(uri, true);
			System.out.println("Loaded " + uri);

			// iterate contents of the loaded resource.
			int i = 1;
			for (EObject eObject : resource.getContents()) {
				if (eObject instanceof LocationCatalog) {
					LocationCatalog catalog = (LocationCatalog) eObject;
					System.out.println("# Installations");
					Iterator<Entry<Installation, EList<Workspace>>> instIterator = catalog.getInstallations()
							.iterator();
					while (instIterator.hasNext()) {
						Entry<Installation, EList<Workspace>> instEntry = instIterator.next();
						Installation inst = instEntry.getKey();
						Resource eResource = inst.eResource();
						if (eResource != null) {
							EList<Workspace> wrkspcList = instEntry.getValue();
							for (Workspace wrkspc : wrkspcList) {
								URI wrkspcURI = wrkspc.eResource().getURI();
								Path instPath = Paths.get(eResource.getURI().toFileString()).getParent().getParent()
										.getParent();
								Path wrkspcPath = Paths.get(wrkspcURI.toFileString()).getParent().getParent()
										.getParent().getParent();
								System.out.format("adding launch entry %s\necl: <%s>\nwrk: <%s>\n", i++, instPath,
										wrkspcPath);
							}
						} else {
							System.out.format("## installation <%s>\n", inst.getName());
						}
					}
				} else {
					System.out.println("no LocationCatalog found");
				}
			}
		} catch (RuntimeException exception) {
			System.out.println("Problem loading " + uri);
			exception.printStackTrace();
		}

		System.out.println("done");

	}
}
