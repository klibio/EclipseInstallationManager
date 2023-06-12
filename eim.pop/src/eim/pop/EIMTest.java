package eim.pop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.knowhowlab.osgi.testing.assertions.BundleAssert.assertBundleState;
import static org.knowhowlab.osgi.testing.assertions.OSGiAssert.getBundleContext;
import static org.knowhowlab.osgi.testing.assertions.ServiceAssert.assertServiceAvailable;
import static org.knowhowlab.osgi.testing.assertions.ServiceAssert.assertServiceUnavailable;
import static org.knowhowlab.osgi.testing.utils.BundleUtils.findBundle;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.oomph.setup.Installation;
import org.eclipse.oomph.setup.LocationCatalog;
import org.eclipse.oomph.setup.Workspace;
import org.eclipse.oomph.setup.impl.SetupFactoryImpl;
import org.eclipse.oomph.util.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.apache.commons.io.FileUtils;

import eim.api.EclipseService;
import eim.api.ListLocationService;

public class EIMTest {

	private final String SERVICE_BUNDLE = "eim.impl";
	private final BundleContext context = FrameworkUtil.getBundle(EIMTest.class).getBundleContext();
	private static String command;

	@TempDir
	static Path directory = Paths.get(System.getProperty("user.home"), "resources");

	@BeforeAll
	public static void setUp() {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("windows")) {	
			command = "C:/Windows/System32/notepad.exe";
		} else {
			command = "/bin/bash";
		}
		File source = Paths.get("resource/").toFile();
		File target = directory.toFile();
		
		try {
			FileUtils.copyDirectory(source, target);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		SetupFactoryImpl setupFactory = new SetupFactoryImpl();
		LocationCatalog catalog = setupFactory.createLocationCatalog();
		

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
		Process p = eclService.startProcess(command, System.getProperty("user.home"), null);
		long parentPID = getPidViaRuntimeMXBean();
		long pid = p.pid();

		Optional<ProcessHandle> processHandle = ProcessHandle.of(parentPID);
		processHandle.ifPresent(process -> {
			process.children().forEach(child -> {
				assertEquals(pid, child.pid());
			});
		});
	}

	@Test
	public void testListLocation() {
		ListLocationService listLocService = ServiceUtils.getService(context, ListLocationService.class);
		listLocService.listLocations("resources/testLocationCatalog.setup");
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
		return result;
	}

}