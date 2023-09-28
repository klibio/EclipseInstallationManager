package tray.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eim.api.EIMService;
import eim.api.LocationCatalogEntry;
import eim.util.PreferenceUtils;
import eim.util.SystemUtils;

@Component(immediate = true)
public class TrayApplication {

	private IEclipsePreferences properties = InstanceScope.INSTANCE.getNode("tray.impl");
	private Preferences eimPrefs = properties.node("eim.prefs");
	private Bundle bundle = FrameworkUtil.getBundle(this.getClass());
	private BundleContext bc = bundle.getBundleContext();
	@Reference
	private EIMService eclService;

	@Reference
	private DataProvider dataController;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL)
	private ManagementView managementView;

	private Logger logger = LoggerFactory.getLogger(TrayApplication.class);
	private LinkedHashMap<LocationCatalogEntry, LinkedList<LocationCatalogEntry>> installationGroupedMap;
	public boolean dispose = false;
	private Display display;
	private Tray tray;

	@Activate
	public void activate(BundleContext context) {
		logger.debug("Activating TrayApplication component");
		installationGroupedMap = dataController.getInstallationMap();

		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				createDisplay();
			}

		});
	}

	/*
	 * Creates the basis for the UI, including the TrayItem
	 */
	public void createDisplay() {
		logger.debug("Starting to create UI");
		display = Display.getDefault();
		Shell shell = new Shell(display);
		Image trayIcon = null;
		try {
			trayIcon = new Image(display, bundle.getEntry("/icons/EIM-Color_512x.png").openStream());
		} catch (IOException e) {
			logger.error("Something went wrong loading the Icon from the Bundle.");
			e.printStackTrace();
		}

		tray = display.getSystemTray();
		if (tray == null) {
			logger.error("The system tray is not available!");
		} else {
			// Start creating Tray Item
			final TrayItem item = new TrayItem(tray, SWT.NONE);
			item.setToolTipText("Eclipse Installation Manager");

			// End TrayItem

			// Start MainMenu
			Menu menu = createMainMenu(shell);
			// End MainMenu

			// Start right click menu
			final Menu subMenu = new Menu(shell, SWT.POP_UP);

			// Add menu item to allow entries into the main menu with a single
			// installation-workspace assignment
			MenuItem switchSingleEntries = new MenuItem(subMenu, SWT.CHECK);
			switchSingleEntries.setText("Allow single mapped entries");
			boolean currentSetting = eimPrefs.getBoolean("allow.single.entries", false);

			switchSingleEntries.setSelection(currentSetting);
			switchSingleEntries.addListener(SWT.Selection, event -> changeSingleEntrySetting(shell));

			// Add menuitem to set Eclipse Installer Location
			MenuItem setInstallerLocation = new MenuItem(subMenu, SWT.CHECK);
			if (PreferenceUtils.checkIfPreferenceKeyExists("eclipse.installer.path", eimPrefs)) {
				setInstallerLocation.setSelection(true);
			}
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

			if (SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_LINUX) {
				item.addSelectionListener(new SelectionListener() {

					@Override
					public void widgetSelected(SelectionEvent e) {
						if ((e.stateMask & SWT.ALT) != 0) {
							startEclipseInstaller(shell);
						} else {
							menu.setVisible(true);
						}

					}

					@Override
					public void widgetDefaultSelected(SelectionEvent e) {
						// Do nothing, because default selection is not part of the MacOS Menu Bar behavior

					}
				});
			} else {
				item.addListener(SWT.Selection, event -> menu.setVisible(true));
				item.addListener(SWT.DefaultSelection, event -> startEclipseInstaller(shell));
			}
			item.addListener(SWT.MenuDetect, event -> subMenu.setVisible(true));

			item.setImage(trayIcon);
			item.setHighlightImage(trayIcon);
		}
		logger.debug("Waiting for disposal");
		while (!dispose) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		logger.debug("Disposing and exiting");
		trayIcon.dispose();
		display.dispose();

		try {
			logger.debug("Shutting down the OSGi framework");
			bc.getBundle(0).stop();
			bundle.stop();
		} catch (BundleException e) {
			logger.debug("Something went wrong shutting down the OSGi framework");
			e.printStackTrace();
		}
	}

	private void changeSingleEntrySetting(Shell shell) {
		boolean currentSetting = eimPrefs.getBoolean("allow.single.entries", false);
		PreferenceUtils.savePreference("allow.single.entries", String.valueOf(!currentSetting), eimPrefs);
		refresh(shell);
	}

	private void openManagementView() {
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				managementView.showOverviewMenu();
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
				eclService.startProcess(installerPath.toString(), null, null);
			} else {
				logger.error("The Eclipse Installer Path is no longer available. Please choose another one!");
				spawnInstallerDialog(shell);
			}
		} catch (Exception e) {
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
		if (SystemUtils.IS_OS_MAC) {
			Path macExecutable = Paths.get(result).resolve("Contents/MacOS/eclipse-inst");
			result = macExecutable.toString();
		}
		if (result == null) {
			logger.debug("Choose dialog closed, not saving anything!");
		} else {
			PreferenceUtils.savePreference("eclipse.installer.path", result, eimPrefs);
		}

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

			if (eimPrefs.getBoolean("allow.single.entries", false)) {
				// If it is a single item, create a MenuItem in the Toplevel Menu
				if (workspaceList.size() == 1) {
					MenuItem mi = new MenuItem(menu, SWT.WRAP | SWT.PUSH);
					LocationCatalogEntry workspaceCatalogEntry = workspaceList.get(0);
					Integer launchNumber = workspaceCatalogEntry.getID();
					String installationName = installation.getInstallationName();
					if (installationName.equals("installation")) {
						installationName = installation.getInstallationFolderName();
					}
					String itemLabel = launchNumber + " # " + installationName + " # "
							+ workspaceCatalogEntry.getWorkspaceFolderName();
					mi.setText(itemLabel);
					mi.addListener(SWT.Selection, event -> eclService.startEntry(workspaceCatalogEntry, true));
					mi.addListener(SWT.MenuDetect, event -> eclService.startEntry(workspaceCatalogEntry, false));

				} else {
					MenuItem mi = new MenuItem(menu, SWT.CASCADE);
					String name = installation.getInstallationName();
					if (name.equals("installation")) {
						mi.setText(installation.getInstallationFolderName());
					} else {
						mi.setText(name);
					}
					Menu subMenu = new Menu(shell, SWT.DROP_DOWN);
					mi.setMenu(subMenu);

					for (LocationCatalogEntry entry : workspaceList) {
						MenuItem subMenuItem = new MenuItem(subMenu, SWT.PUSH);
						Integer launchNumber = entry.getID();
						subMenuItem.setToolTipText(entry.getInstallationPath().toString());
						if (entry.getWorkspaceName().equals("ws") || entry.getWorkspaceName().equals("workspace")) {
							subMenuItem.setText(launchNumber + " # " + entry.getWorkspaceFolderName());
						} else {
							subMenuItem.setText(launchNumber + " # " + entry.getWorkspaceName());
						}

						subMenuItem.addListener(SWT.Selection, event -> eclService.startEntry(entry, true));
					}
					new MenuItem(subMenu, SWT.HORIZONTAL | SWT.SEPARATOR);
					MenuItem openWithoutWorkspace = new MenuItem(subMenu, SWT.PUSH);
					openWithoutWorkspace.setText("Let me choose...");
					openWithoutWorkspace.addListener(SWT.Selection,
							event -> eclService.startEntry(installation, false));
					mi.addListener(SWT.MouseHover, event -> subMenu.setVisible(true));
				}
			} else {
				MenuItem mi = new MenuItem(menu, SWT.CASCADE);
				String name = installation.getInstallationName();
				if (name.equals("installation")) {
					mi.setText(installation.getInstallationFolderName());
				} else {
					mi.setText(name);
				}
				Menu subMenu = new Menu(shell, SWT.DROP_DOWN);
				mi.setMenu(subMenu);

				for (LocationCatalogEntry entry : workspaceList) {
					MenuItem subMenuItem = new MenuItem(subMenu, SWT.PUSH);
					Integer launchNumber = entry.getID();
					subMenuItem.setToolTipText(entry.getInstallationPath().toString());
					if (entry.getWorkspaceName().equals("ws") || entry.getWorkspaceName().equals("workspace")) {
						subMenuItem.setText(launchNumber + " # " + entry.getWorkspaceFolderName());
					} else {
						subMenuItem.setText(launchNumber + " # " + entry.getWorkspaceName());
					}

					subMenuItem.addListener(SWT.Selection, event -> eclService.startEntry(entry, true));
				}
				new MenuItem(subMenu, SWT.HORIZONTAL | SWT.SEPARATOR);
				MenuItem openWithoutWorkspace = new MenuItem(subMenu, SWT.PUSH);
				openWithoutWorkspace.setText("Let me choose...");
				openWithoutWorkspace.addListener(SWT.Selection, event -> eclService.startEntry(installation, false));
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
		logger.debug("Refreshing location catalog entries!");
		dataController.refreshData();
		shell.dispose();
		tray.dispose();
		createDisplay();
	}

	public void setInstallationMap(
			LinkedHashMap<LocationCatalogEntry, LinkedList<LocationCatalogEntry>> installationMap) {
		this.installationGroupedMap = installationMap;
	}
}