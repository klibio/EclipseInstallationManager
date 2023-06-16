package eim.pop;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.knowhowlab.osgi.testing.assertions.BundleAssert.assertBundleState;
import static org.knowhowlab.osgi.testing.assertions.OSGiAssert.getBundleContext;
import static org.knowhowlab.osgi.testing.assertions.ServiceAssert.assertServiceAvailable;
import static org.knowhowlab.osgi.testing.assertions.ServiceAssert.assertServiceUnavailable;
import static org.knowhowlab.osgi.testing.utils.BundleUtils.findBundle;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.oomph.setup.Installation;
import org.eclipse.oomph.setup.LocationCatalog;
import org.eclipse.oomph.setup.Workspace;
import org.eclipse.oomph.setup.impl.SetupFactoryImpl;
import org.eclipse.oomph.setup.internal.core.util.SetupCoreUtil;
import org.eclipse.oomph.util.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import eim.api.EclipseService;
import eim.api.ListLocationService;

public class EIMTest {

	private final String SERVICE_BUNDLE = "eim.impl";
	private final BundleContext context = FrameworkUtil.getBundle(EIMTest.class).getBundleContext();
	private static String command;

	@TempDir
	static Path directory = Paths.get(System.getProperty("user.dir"), "tempDirEIMtest");

	@BeforeAll
	public static void setUp() {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("windows")) {
			command = "C:/Windows/System32/notepad.exe";
		} else {
			command = "/bin/bash";
		}
		System.out.format("Using %s as tempDir", directory);
		ResourceSet resourceSet = SetupCoreUtil.createResourceSet();
		SetupFactoryImpl factory = new SetupFactoryImpl();

		// create own installations
		Installation installation1 = factory.createInstallation();
		Installation installation2 = factory.createInstallation();
		String inst1 = directory.resolve("installation1").resolve("installation.setup").toFile().getAbsolutePath();
		String inst2 = directory.resolve("installation2").resolve("installation.setup").toFile().getAbsolutePath();

		URI inst1URI = URI.createFileURI(inst1);
		Resource resource1 = resourceSet.createResource(inst1URI);
		resource1.getContents().add(installation1);

		URI inst2URI = URI.createFileURI(inst2);
		Resource resource2 = resourceSet.createResource(inst2URI);
		resource2.getContents().add(installation2);

		// create own workspaces
		Workspace workspace1 = factory.createWorkspace();
		Workspace workspace2 = factory.createWorkspace();
		String ws1 = directory.resolve("ws1").resolve("workspace.setup").toFile().getAbsolutePath();
		String ws2 = directory.resolve("ws2").resolve("workspace.setup").toFile().getAbsolutePath();

		URI ws1URI = URI.createFileURI(ws1);
		Resource wsResource1 = resourceSet.createResource(ws1URI);
		wsResource1.getContents().add(workspace1);

		URI ws2URI = URI.createFileURI(ws2);
		Resource wsResource2 = resourceSet.createResource(ws2URI);
		wsResource2.getContents().add(workspace2);

		// save setup files
		try {
			resource1.save(null);
			resource2.save(null);
			wsResource1.save(null);
			wsResource2.save(null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Path inst1Path = Paths.get(inst1);
		Path inst2Path = Paths.get(inst2);
		Path ws1Path = Paths.get(ws1);
		Path ws2Path = Paths.get(ws2);

		// make sure temporary files were actually created
		assertTrue(Files.exists(inst1Path));
		assertTrue(Files.exists(inst2Path));
		assertTrue(Files.exists(ws1Path));
		assertTrue(Files.exists(ws2Path));

		// create own locations catalog

		LocationCatalog locationCatalog = factory.createLocationCatalog();
		String catalog = directory.resolve("locations.setup").toFile().getAbsolutePath();
		URI catalogURI = URI.createFileURI(catalog);
		EMap<Installation, EList<Workspace>> installations = locationCatalog.getInstallations();

		EList<Workspace> inst1Workspaces = new BasicEList<>();
		inst1Workspaces.add(workspace1);

		EList<Workspace> inst2Workspaces = new BasicEList<>();
		inst2Workspaces.add(workspace1);
		inst2Workspaces.add(workspace2);

		installations.put(installation1, inst1Workspaces);
		installations.put(installation2, inst2Workspaces);

		Resource catalogResource = resourceSet.createResource(catalogURI);
		catalogResource.getContents().add(locationCatalog);
		try {
			catalogResource.save(null);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Path locationsCatalogPath = Paths.get(catalog);
		assertTrue(Files.exists(locationsCatalogPath));

	}

	@Test
	public void testLifeCycle() throws Exception {
		assertBundleState(Bundle.ACTIVE, SERVICE_BUNDLE);
		assertServiceAvailable(EclipseService.class);
		findBundle(getBundleContext(), SERVICE_BUNDLE).stop();
		assertServiceUnavailable(EclipseService.class, 1, TimeUnit.SECONDS);
		findBundle(getBundleContext(), SERVICE_BUNDLE).start();
		EclipseService eclService = ServiceUtils.getService(context, EclipseService.class);
		ListLocationService listLocService = ServiceUtils.getService(context, ListLocationService.class);
		assertNotNull(eclService);
		assertNotNull(listLocService);
	}

	@Test
	public void testStartProcess() {
		EclipseService eclService = ServiceUtils.getService(context, EclipseService.class);
		String workingDir = System.getProperty("user.home");
		System.out.format("#DEBUG-TEST# launching process %s in working dir %s\n", command, workingDir);
		Process p = eclService.startProcess(command, workingDir, null);
		System.out.println("#DEBUG-TEST# process info: " + p.info());
		long parentPID = getPidViaRuntimeMXBean();
		long pid = p.pid();

		Optional<ProcessHandle> processHandle = ProcessHandle.of(parentPID);
		processHandle.ifPresent(process -> {
			process.children().forEach(child -> {
				System.out.format("#DEBUG-TEST# child-process %s # info %s\n", child.pid(), child.info());
			});
			assertTrue(process.children().filter(c -> pid == c.pid()).findFirst().isPresent());
		});
	}

	@Test
	public void testListLocation() {
		ListLocationService listLocService = ServiceUtils.getService(context, ListLocationService.class);
		listLocService.listLocations(directory.resolve("locations.setup").toFile().getAbsolutePath());

		Map<Integer, Pair<Installation, Workspace>> entries = listLocService.getLocationEntries();
		assertTrue(entries.size() > 0);
		assertTrue(entries.size() == 3);
	}

	@AfterAll
	public static void cleanUp() {
		long parentPID = getPidViaRuntimeMXBean();
		Optional<ProcessHandle> processHandle = ProcessHandle.of(parentPID);
		processHandle.ifPresent(process -> {
			process.children().forEach(child -> {
				child.destroy();
			});
		});
	}

	private static Long getPidViaRuntimeMXBean() {
		RuntimeMXBean rtb = ManagementFactory.getRuntimeMXBean();
		String processName = rtb.getName();
		Long result = null;
		Pattern pattern = Pattern.compile("^([0-9]+)@.+$", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(processName);
		if (matcher.matches()) {
			result = Long.valueOf(matcher.group(1));
		}
		System.out.format("#DEBUG-TEST# getPidViaRuntimeMXBean() process %s with pid %s\n", processName, result);
		return result;
	}

}