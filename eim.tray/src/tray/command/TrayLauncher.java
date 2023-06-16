package tray.command;

import java.lang.reflect.Method;

import org.apache.felix.service.command.Descriptor;
import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.service.component.annotations.Component;

//@formatter:off
@Component(
		property = {
				"osgi.command.scope=zEIMtray",
				"osgi.command.function=launchTray"
		}, service = TrayLauncher.class)
//@formatter:on
@ConsumerType
public class TrayLauncher {
	@Descriptor("Launch Tray Application")
	public void launchTray() {
		String trayClass = "tray.impl.TrayApplication";
		System.out.println("starting tray application " + trayClass);
		try {
			Class<?> clazz = Class.forName(trayClass);
			Method mainMethod = clazz.getDeclaredMethod("main", String[].class);
			mainMethod.invoke(null, (Object) null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
