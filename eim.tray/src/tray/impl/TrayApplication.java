package tray.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import org.eclipse.oomph.setup.Installation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eim.api.EIMService;
import eim.api.LocationCatalogEntry;

@Component(immediate = true)
public class TrayApplication {

	@Reference
	private EIMService eclService;

	private LinkedList<LocationCatalogEntry> locationEntries;
	private Logger logger = LoggerFactory.getLogger(TrayApplication.class);
	private LinkedHashMap<LocationCatalogEntry, LinkedList<LocationCatalogEntry>> installationGroupedMap = new LinkedHashMap<>();;
	public boolean dispose = false;

	@Activate
	public void activate(BundleContext context) {
		dataInitialization();
		createMappedInstallationEntries();
		createDisplay();
		
		try {
			logger.debug("Shutting down the OSGi framework");
			context.getBundle(0).stop();
		} catch (BundleException e) {
			logger.debug("Something went wrong shutting down the OSGi framework");
			e.printStackTrace();
		}
	}

	private void dataInitialization() {
		logger.debug("Loading data");
		eclService.listLocations(null);
		locationEntries = eclService.getLocationEntries();
	}

	private void createDisplay() {
		logger.debug("Starting to create UI");
		Display display = new Display();
		Shell shell = new Shell(display);
		Image image = new Image(display, 16, 16);
		Image image2 = new Image(display, 16, 16);
		GC gc = new GC(image2);
		gc.setBackground(display.getSystemColor(SWT.COLOR_DARK_RED));
		gc.fillRectangle(image2.getBounds());
		gc.dispose();
		final Tray tray = display.getSystemTray();
		if (tray == null) {
			logger.error("The system tray is not available!");
		} else {
			final TrayItem item = new TrayItem(tray, SWT.NONE);
			item.setToolTipText("Eclipse Installation Manager");
			item.addListener(SWT.Show, event -> System.out.println("show"));
			item.addListener(SWT.Hide, event -> System.out.println("hide"));
			item.addListener(SWT.DefaultSelection, event -> System.out.println("default selection"));
			final Menu menu = new Menu(shell, SWT.POP_UP);

			installationGroupedMap.forEach((installation, workspaceList) -> {
				if (workspaceList.size() == 1) {
					MenuItem mi = new MenuItem(menu, SWT.WRAP | SWT.PUSH);
					LocationCatalogEntry workspaceCatalogEntry = workspaceList.get(0);
					Integer launchNumber = workspaceCatalogEntry.getID();
					String itemLabel = launchNumber + " # " + installation.getInstallationFolderName() + " # "
							+ workspaceCatalogEntry.getWorkspaceFolderName();
					mi.setText(itemLabel);
					mi.addListener(SWT.Selection, event -> eclService.startEntry(workspaceCatalogEntry));
				} else {
					MenuItem mi = new MenuItem(menu, SWT.CASCADE);
					mi.setText(installation.getInstallationFolderName());
					Menu subMenu = new Menu(shell, SWT.DROP_DOWN);
					mi.setMenu(subMenu);

					for (LocationCatalogEntry entry : workspaceList) {
						MenuItem subMenuItem = new MenuItem(subMenu, SWT.PUSH);
						Integer launchNumber = entry.getID();
						subMenuItem.setText(launchNumber + " # " + entry.getWorkspaceFolderName());
						subMenuItem.addListener(SWT.Selection, event -> eclService.startEntry(entry));
					}
					mi.addListener(SWT.MouseHover, event -> subMenu.setVisible(true));
				}
			});
			final Menu subMenu = new Menu(shell, SWT.POP_UP);
			MenuItem quitApp = new MenuItem(subMenu, SWT.WRAP | SWT.PUSH);
			quitApp.setText("Quit");
			quitApp.addListener(SWT.Selection, event -> dispose());
			
			item.addListener(SWT.Selection, event -> menu.setVisible(true));
			item.addListener(SWT.MenuDetect, event -> subMenu.setVisible(true));

			// TODO: Add Settings Menu on SWT.MenuDetect for refresh of the catalog
			item.setImage(image2);
			item.setHighlightImage(image);
		}
		logger.debug("Waiting for disposal");
		while (!dispose) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		logger.debug("Disposing and exiting");
		image.dispose();
		image2.dispose();
		display.dispose();
	}

	private void dispose() {
		this.dispose = true;
	}

	private void createMappedInstallationEntries() {
		logger.debug("creating installation - workspaces map");

		LinkedHashMap<LocationCatalogEntry, LinkedList<LocationCatalogEntry>> installationMap = new LinkedHashMap<>();
		LinkedList<LocationCatalogEntry> installations = new LinkedList<>();

		locationEntries.forEach(locationCatalogEntry -> {
			if (!checkIfListContainsInstallation(locationCatalogEntry, installations)) {
				logger.debug("List does not contain locationCatalogEntry "
						+ locationCatalogEntry.getInstallationFolderName());
				installations.add(locationCatalogEntry);
			}
		});

		for (LocationCatalogEntry installationEntry : installations) {
			LinkedList<LocationCatalogEntry> mappedWorkspaces = new LinkedList<>();
			locationEntries.forEach(locationCatalogEntry -> {
				if (checkIfInstallationURIequals(installationEntry.getInstallation(),
						locationCatalogEntry.getInstallation())) {
					mappedWorkspaces.add(locationCatalogEntry);
				}
			});
			installationMap.put(installationEntry, mappedWorkspaces);
		}

		installationGroupedMap.putAll(installationMap);

	}

	private boolean checkIfListContainsInstallation(LocationCatalogEntry entry,
			LinkedList<LocationCatalogEntry> installationList) {
		Installation installation1 = entry.getInstallation();
		boolean result = false;

		for (LocationCatalogEntry listEntry : installationList) {
			if (checkIfInstallationURIequals(installation1, listEntry.getInstallation())) {
				result = true;
			}
		}
		return result;
	}

	private boolean checkIfInstallationURIequals(Installation inst1, Installation inst2) {
		Path path1 = Paths.get(inst1.eResource().getURI().toFileString());
		Path path2 = Paths.get(inst2.eResource().getURI().toFileString());

		boolean result = false;
		if (path1.compareTo(path2) == 0) {
			result = true;
		}
		return result;
	}

}
