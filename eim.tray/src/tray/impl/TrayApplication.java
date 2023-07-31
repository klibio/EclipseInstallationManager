package tray.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eim.api.LocationCatalogEntry;
import eim.util.PreferenceUtils;
import eim.util.SystemUtils;

public class TrayApplication {

	IEclipsePreferences properties = InstanceScope.INSTANCE.getNode("tray.impl");
	Preferences eimPrefs = properties.node("eim.prefs");

	private UIAppController dataController;

	private Logger logger = LoggerFactory.getLogger(TrayApplication.class);
	private LinkedHashMap<LocationCatalogEntry, LinkedList<LocationCatalogEntry>> installationGroupedMap;
	public boolean dispose = false;

	@Activate
	public void activate(BundleContext context) {
		createDisplay();
	}

	/*
	 * Creates the basis for the UI, including the TrayItem
	 */
	private void createDisplay() {
		logger.debug("Starting to create UI");
		Display display = new Display();
		Shell shell = new Shell(display);
		Image image = new Image(display, 16, 16);
		Bundle bundle = FrameworkUtil.getBundle(this.getClass());
		Image trayIcon = null;
		try {
			trayIcon = new Image(display, bundle.getEntry("/icons/EIM-Color_512x.png").openStream());
		} catch (IOException e) {
			logger.error("Something went wrong loading the Icon from the Bundle.");
			e.printStackTrace();
		}

		final Tray tray = display.getSystemTray();
		if (tray == null) {
			logger.error("The system tray is not available!");
		} else {
			// Start creating Tray Item
			final TrayItem item = new TrayItem(tray, SWT.NONE);
			item.setToolTipText("Eclipse Installation Manager");
			item.addListener(SWT.Show, event -> System.out.println("show"));
			item.addListener(SWT.Hide, event -> System.out.println("hide"));
			item.addListener(SWT.DefaultSelection, event -> startEclipseInstaller(shell));

			// End TrayItem

			// Start MainMenu
			Menu menu = createMainMenu(shell);
			// End MainMenu

			// Start right click menu
			final Menu subMenu = new Menu(shell, SWT.POP_UP);

			// Add menuitem to set Eclipse Installer Location
			MenuItem setInstallerLocation = new MenuItem(subMenu, SWT.WRAP | SWT.PUSH);
			setInstallerLocation.setText("Set Eclipse Installer Location");
			setInstallerLocation.addListener(SWT.Selection, event -> spawnInstallerDialog(shell));

			// Add button to refresh catalog
			MenuItem refreshApp = new MenuItem(subMenu, SWT.WRAP | SWT.PUSH);
			refreshApp.setText("Refresh Entries");
			refreshApp.addListener(SWT.Selection, event -> refresh(shell));

			// Add the button to Quit the application
			MenuItem quitApp = new MenuItem(subMenu, SWT.WRAP | SWT.PUSH);
			quitApp.setText("Quit");
			quitApp.addListener(SWT.Selection, event -> dispose());
			// End right click menu

			item.addListener(SWT.Selection, event -> menu.setVisible(true));
			item.addListener(SWT.MenuDetect, event -> subMenu.setVisible(true));

			item.setImage(trayIcon);
			item.setHighlightImage(trayIcon);
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

	private void openManagementView() {
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				dataController.openManagementView();
			}
			
		});
	}

	/**
	 * Fetches preference and executes if that preference exists
	 * 
	 * @param shell Shell to attach the UI to
	 */
	private void startEclipseInstaller(Shell shell) {
		try {
			if (PreferenceUtils.checkIfPreferenceKeyExists("eclipse.installer.path", eimPrefs)) {
				Path installerPath = Paths.get(eimPrefs.get("eclipse.installer.path", null));
				dataController.startProcess(installerPath.toString(), null, null);
			} else {
				logger.error("The Eclipse Installer Path is no longer available. Please choose another one!");
				spawnInstallerDialog(shell);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Starts the FileDialog to set the Eclipse Installer preference
	 * 
	 * @param shell
	 */
	private void spawnInstallerDialog(Shell shell) {
		FileDialog selectInstallerLocation = new FileDialog(shell, SWT.OPEN);
		if (SystemUtils.IS_OS_WINDOWS) {
			selectInstallerLocation.setFilterExtensions(new String[] { "*.exe" });
		}
		selectInstallerLocation.setFilterPath(null);
		String result = selectInstallerLocation.open();

		PreferenceUtils.savePreference("eclipse.installer.path", result, eimPrefs);
	}

	/*
	 * Sets a flag to quit the application
	 */
	private void dispose() {
		this.dispose = true;
	}

	/**
	 * Creates the main pop-up menu for the tray item
	 * 
	 * @param shell
	 * @return the menu created
	 */
	private Menu createMainMenu(Shell shell) {
		final Menu menu = new Menu(shell, SWT.POP_UP);

		MenuItem openOverview = new MenuItem(menu, SWT.WRAP | SWT.PUSH | SWT.BOLD);
		openOverview.setText("Open Management Overview");
		openOverview.addListener(SWT.Selection, e -> openManagementView());

		new MenuItem(menu, SWT.HORIZONTAL | SWT.SEPARATOR);

		// Create a new MenuItem for each installation-workspace pair
		installationGroupedMap.forEach((installation, workspaceList) -> {

			// If it is a single item, create a MenuItem in the Toplevel Menu
			if (workspaceList.size() == 1) {
				MenuItem mi = new MenuItem(menu, SWT.WRAP | SWT.PUSH);
				LocationCatalogEntry workspaceCatalogEntry = workspaceList.get(0);
				Integer launchNumber = workspaceCatalogEntry.getID();
				String itemLabel = launchNumber + " # " + installation.getInstallationFolderName() + " # "
						+ workspaceCatalogEntry.getWorkspaceFolderName();
				mi.setText(itemLabel);
				mi.addListener(SWT.Selection, event -> dataController.startEntry(workspaceCatalogEntry));
				// Create a SubMenu if more than 1 workspace is assigned to the installation
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
					subMenuItem.addListener(SWT.Selection, event -> dataController.startEntry(entry));
				}
				mi.addListener(SWT.MouseHover, event -> subMenu.setVisible(true));
			}
		});

		return menu;
	}

	/**
	 * Refreshes the data and the UI (by recreation)
	 * 
	 * @param shell
	 */
	private void refresh(Shell shell) {
		Display display = shell.getDisplay();
		logger.debug("Refreshing location catalog entries!");
		dataController.refreshData();
		display.dispose();
		createDisplay();
	}

	public TrayApplication(UIAppController controller) {
		this.dataController = controller;
	}

	public void setInstallationMap(
			LinkedHashMap<LocationCatalogEntry, LinkedList<LocationCatalogEntry>> installationMap) {
		this.installationGroupedMap = installationMap;
	}
}