package tray.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import org.eclipse.oomph.setup.Installation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
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
		Bundle bundle = FrameworkUtil.getBundle(this.getClass());
		Image trayIcon = null;
		try {
			trayIcon = new Image(display, bundle.getEntry("/icon/EIM-Color_512x.png").openStream());
		} catch (IOException e) {
			logger.error("Something went wrong loading the Icon from the Bundle.");
			e.printStackTrace();
		}

		final Tray tray = display.getSystemTray();
		if (tray == null) {
			logger.error("The system tray is not available!");
		} else {
			final TrayItem item = new TrayItem(tray, SWT.NONE);
			item.setToolTipText("Eclipse Installation Manager");
			item.addListener(SWT.Show, event -> System.out.println("show"));
			item.addListener(SWT.Hide, event -> System.out.println("hide"));
			item.addListener(SWT.DefaultSelection, event -> System.out.println("default selection"));

			Menu menu = createMainMenu(shell);

			final Menu subMenu = new Menu(shell, SWT.POP_UP);

			// Add button to refresh catalog
			MenuItem refreshApp = new MenuItem(subMenu, SWT.WRAP | SWT.PUSH);
			refreshApp.setText("Refresh Entries");
			refreshApp.addListener(SWT.Selection, event -> refresh(shell));

			// Add the button to Quit the application
			MenuItem quitApp = new MenuItem(subMenu, SWT.WRAP | SWT.PUSH);
			quitApp.setText("Quit");
			quitApp.addListener(SWT.Selection, event -> dispose());

			item.addListener(SWT.Selection, event -> menu.setVisible(true));
			item.addListener(SWT.MenuDetect, event -> subMenu.setVisible(true));

			item.setImage(trayIcon);
			item.setHighlightImage(image);
		}
		logger.debug("Waiting for disposal");
		while (!dispose) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		logger.debug("Disposing and exiting");
		image.dispose();
		trayIcon.dispose();
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

		installationGroupedMap = installationMap;

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

	private Menu createMainMenu(Shell shell) {
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
					subMenuItem.setToolTipText(entry.getInstallationPath().toString());
					subMenuItem.setText(launchNumber + " # " + entry.getWorkspaceFolderName());
					subMenuItem.addListener(SWT.Selection, event -> eclService.startEntry(entry));
				}
				mi.addListener(SWT.MouseHover, event -> subMenu.setVisible(true));
			}
		});

		return menu;
	}

	private void refresh(Shell shell) {
		Display display = shell.getDisplay();
		logger.debug("Refreshing location catalog entries!");
		eclService.refreshLocations();
		this.locationEntries = eclService.getLocationEntries();
		createMappedInstallationEntries();

		// due to app not being in focus dispose and recreate
		display.dispose();
		createDisplay();
	}

}
