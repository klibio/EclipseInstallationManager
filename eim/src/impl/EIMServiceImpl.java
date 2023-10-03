package impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
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
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eim.api.EIMService;
import eim.api.LocationCatalogEntry;
import eim.util.FileUtils;
import eim.util.SystemUtils;

/**
 * This class controls all aspects of the application's execution
 */
@Component
public class EIMServiceImpl implements EIMService {

	private static final Logger logger = LoggerFactory.getLogger(EIMServiceImpl.class);
	private LinkedList<LocationCatalogEntry> locationEntries = new LinkedList<>();
	private String locationFile;
	
	/**
	 * Tries to start a process with the Java ProcessBuilder with a specified command and working directory.
	 * Arguments are passed in key=value form.
	 * 
	 * @param command      Command that is passed to the ProcessBuilder
	 * @param workingDir   Directory which is designated as the working directory for the started process
	 * @param args 		   Additional arguments in key=value style
	 */
	@Override
	public Process startProcess(String command, String workingDir, String[] args) {
		Map<String, String> arguments = new HashMap<String, String>();

		ProcessBuilder pb = new ProcessBuilder();
		if (workingDir != null) {
			pb.directory(Paths.get(workingDir).toFile());
		}
		Map<String, String> env = pb.environment();

		if (SystemUtils.IS_OS_WINDOWS) {
			env.put("TEMP", System.getenv("TEMP"));
			env.put("SYSTEMDRIVE", System.getenv("SYSTEMDRIVE"));
		}

		if (args != null && args.length > 0) {
			for (String string : args) {
				String[] argument = string.split("=");
				env.put(argument[0], argument[1]);
				arguments.put(argument[0], argument[1]);
			}
		}
		if (arguments.containsKey("ws")) {
			pb.command(command, "-data", arguments.get("ws"));
		} else {
			pb.command(command);
		}
		try {
			return pb.start();
		} catch (IOException e) {
			logger.error(
					"There was a problem starting the the process!\n" + "Command: " + command + "\n" + e.getMessage());
			e.printStackTrace();
			return null;
		}

	}
	
	/**
	 * Starts a specific LocationCatalogEntry with the startProcess method.
	 * 
	 * @param entryToExecute   		The LocationCatalogEntry which is supposed to be started.
	 * @param startWithWorkspace	Sets a boolean if this entry should be started with the ws Parameter, or not.
	 * 								False lets the user choose a workspace at installation startup
	 */
	@Override
	public void startEntry(LocationCatalogEntry entryToExecute, boolean startWithWorkspace) {
		if (entryToExecute == null) {
			logger.error("The entry could not be found in the list.");
		} else {
			Path installationPath = entryToExecute.getInstallationPath();
			logger.debug("Installation path is " + installationPath.toAbsolutePath().toString());
			Path workspacePath = entryToExecute.getWorkspacePath();
			logger.debug("Workspace path is " + workspacePath.toAbsolutePath().toString());

			System.getProperty("eclipse.launcher.name");

			Path executablePath = null;
			try {
				executablePath = determineExecutable(installationPath);
			} catch (IOException e) {
				logger.error("An error occurred during determination of the executable!");
				e.printStackTrace();
			}
			
			ArrayList<String> args = new ArrayList<String>();
			if(startWithWorkspace) {
				args.add("ws=" + workspacePath.toString());
			}
			String[] simpleArray = new String[args.size()];
			args.toArray(simpleArray);

			logger.debug("Starting " + executablePath.toString() + " in working directory "
					+ installationPath.toString() + " with arguments\n " + Arrays.toString(simpleArray));
			startProcess(executablePath.toString(), installationPath.toString(), simpleArray);
		}
	}
	
	/**
	 * Lists all LocationCatalog entries in the log, after refreshing the data structure used to store the entries.
	 * 
	 * @param locationFile    Path to a Location Catalog from the Oomph Setup model. If null, the standard Eclipse Installer
	 * 						  path will be used.
	 */
	@Override
	public void listLocations(String locationFile) {
		this.locationFile = locationFile;
		File file;
		if (locationFile == null || locationFile.isBlank()) {
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
		} catch (RuntimeException e) {
			logger.error("The given file " + locationFile
					+ " could not be loaded!");
			e.printStackTrace();
		}

		// print map details
		for (LocationCatalogEntry entry : locationEntries) {
			Integer key = entry.getID();
			Path instPath = entry.getInstallationPath();
			Path wrkspcPath = entry.getWorkspacePath();
			System.out.format("Launch entry Number %s\n" + "Location: <%s>\n" + "Workspace: <%s>\n", key, instPath,
					wrkspcPath);
		}

	}
	
	/**
	 * Loads the given Resource into an EMF Resource.
	 * @param file				  The path to the resource that is loaded. Needs to be compliant with the Oomph Setup Model.
	 * @return 					  The resource that has been loaded.
	 * @throws RuntimeException   If the resource could not be loaded properly.
	 */
	private Resource loadLocationResource(File file) throws RuntimeException {
		URI uri = URI.createFileURI(file.getAbsolutePath());
		logger.debug("Loading locations catalog from " + uri);

		ResourceSet resourceSet = SetupCoreUtil.createResourceSet();

		Resource resource = resourceSet.getResource(uri, true);
		logger.debug("Loaded locations catalog from " + uri);

		return resource;
	}
	
	/**
	 * Given a Location Catalog resource, all installation entries are fetched and for each mapped workspace a LocationCatalogEntry
	 * is created. All entries are stored in the locationEntries LinekdList
	 * @param file				  Path to the LocationCatalog that the entries are needed from.
	 * @throws RuntimeException	  If loading the resource fails.
	 */
	public void fetchEntries(File file) throws RuntimeException {
		LinkedList<LocationCatalogEntry> entryListTemp = new LinkedList<>();
		try {
			Resource resource = loadLocationResource(file);

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
								Resource workspaceResource = wrkspc.eResource();
								if (workspaceResource != null) {
									LocationCatalogEntry entry = new LocationCatalogEntryImpl(i, inst, wrkspc, null);
									logger.debug("Added entry " + entry.getInstallationFolderName() + " with workspace "
											+ entry.getWorkspaceFolderName());
									entryListTemp.add(entry);
									i++;
								} else {
									logger.info("A workspace entry in the catalog seems to have been deleted...");
								}
							}
						} else {
							logger.info("An installation in the catalog seems to have been deleted...");
						}
					}
					locationEntries = entryListTemp;
				} else {
					logger.error("The given file is not a LocationCatalog!");
				}
			}

		} catch (RuntimeException exception) {
			exception.printStackTrace();
		}

	}

	public LinkedList<LocationCatalogEntry> getLocationEntries() {
		return locationEntries;
	}
	
	/**
	 * Reloads the LocationCatalog and refetched all its entries. Alos updates the data structure.
	 */
	@Override
	public void refreshLocations() {
		listLocations(this.locationFile);
	}
	
	/**
	 * Determines the name of the executable file, based on the name if the ini file.
	 * Differentiates the ini files path based on the operating system
	 * @param installationPath		The path of the Eclipse product installation.
	 * @return						The path of the executable
	 * @throws IOException			if the search fr the ini failed.
	 */
	private Path determineExecutable(Path installationPath) throws IOException {
		// Default values for paths work with Windows and Linux
		Path executablePath = installationPath;
		Path iniPath = installationPath;
		logger.debug("Trying to find the correct executable");

		// Change defaults if on MacOS
		if (SystemUtils.IS_OS_MAC) {
			logger.debug("MacOS detected, adjusting Paths");
			iniPath = installationPath.resolve("Contents/Eclipse/");
			executablePath = installationPath.resolve("Contents/MacOS/");
		}

		String executableName = getIniName(iniPath);

		// Windows require .exe file ending
		if (SystemUtils.IS_OS_WINDOWS) {
			logger.debug("Windows detected, adjusting executable");
			executablePath = executablePath.resolve(executableName + ".exe");
		} else {
			executablePath = executablePath.resolve(executableName);
		}

		return executablePath;
	}
	
	/**
	 * Resolves the name of the ini file in this directory. If multiple are found an error message is shot.
	 * Result will be null in that case.
	 * @param path				Path in which to search for the ini file
	 * @return					The name of the ini filae without the file ending.
	 * @throws IOException		If path is not a directory, or the FileStream fails.
	 */
	private String getIniName(Path path) throws IOException {
		String result = null;
		if (!Files.isDirectory(path)) {
			throw new IllegalArgumentException(path.toString() + " must be a directory!");
		}

		DirectoryStream<Path> stream = Files.newDirectoryStream(path);
		LinkedList<Path> inis = new LinkedList<>();

		Iterator<Path> iniFileIterator = stream.iterator();
		while (iniFileIterator.hasNext()) {
			Path filePath = iniFileIterator.next();
			if (FileUtils.getFileExtension(filePath.getFileName()).equals("ini")) {
				inis.add(filePath);
				logger.debug("Found ini file " + filePath);
			}
		}
		stream.close();
		if (inis.size() != 1) {
			logger.error("None or Multiple inis found, is this the right directory?");
		} else {
			result = FileUtils.getBasename(inis.getFirst().getFileName().toString());
		}

		return result;
	}
	
	/**
	 * Rename a workspace Resource inside the data structure and inside the resource xml file.
	 * 
	 * @param entry 	The LocationCatalogEntry for which the installation is to be renamed
	 * @param name		The new name
	 */
	@Override
	public void renameWorkspace(LocationCatalogEntry entry, String name) {
		logger.debug("Renaming workspace " + entry.getWorkspace().getName() + " to " + name);
		Workspace workspace = entry.getWorkspace();
		workspace.setName(name);
		URI uri;
		if(SystemUtils.IS_OS_WINDOWS) {
			uri = URI.createFileURI(entry.getWorkspacePath()
					.resolve(".metadata\\.plugins\\org.eclipse.oomph.setup\\workspace.setup").toString());
		} else {
			uri = URI.createFileURI(entry.getWorkspacePath()
					.resolve(".metadata/.plugins/org.eclipse.oomph.setup/workspace.setup").toString());
		}
		
		Resource workspaceResource = workspace.eResource();
		workspaceResource.setURI(uri);

		try {
			workspaceResource.save(null);
		} catch (IOException e) {
			logger.error("Saving the new workspace setup file failed!");
			e.printStackTrace();
		}
	}
	/**
	 * Rename an installation Resource inside the data structure and inside the resources xml file.
	 * 
	 * @param entry 	The LocationCatalogEntry for which the workspace is to be renamed
	 * @param name		The new name
	 */
	@Override
	public void renameInstallation(LocationCatalogEntry entry, String name) {
		Installation installation = entry.getInstallation();
		installation.setName(name);
		URI uri;
		if(SystemUtils.IS_OS_MAC) {
			uri = URI.createFileURI(entry.getInstallationPath()
					.resolve("contents/Eclipse/configuration/org.eclipse.oomph.setup/installation.setup").toString());
		} else if (SystemUtils.IS_OS_WINDOWS) {
			uri = URI.createFileURI(entry.getInstallationPath()
					.resolve("configuration\\org.eclipse.oomph.setup\\installation.setup").toString());
		} else {
			uri = URI.createFileURI(entry.getInstallationPath()
					.resolve("configuration/org.eclipse.oomph.setup/installation.setup").toString());
		}
		
		Resource installationResource = installation.eResource();
		installationResource.setURI(uri);
		
		logger.debug("Renaming installation " + installation.getName() + " to " + name + "\n"
				+ "At location " + uri);
		
		try {
			installationResource.save(null);
		} catch (IOException e) {
			logger.error("Saving the new installation setup file failed!");
			e.printStackTrace();
		}
	}
	
	/**
	 * Deletes a specified Path recursively, including all files and directories.
	 * 
	 * @param path		The path to be deleted.
	 */
	@Override
	public void deletePath(Path path) {
		try {
			Files.walk(path)
				.map(Path::toFile)
				.sorted(Comparator.reverseOrder())
				.forEach(File::delete);
		} catch (IOException e) {
			logger.error("Failed deleting the contents of " + path.toString() + ". Please make sure there are no files left over!");
			e.printStackTrace();
		}
	}

}
