package tray.impl;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eim.api.EIMService;
import eim.api.LocationCatalogEntry;

@Component(service = EditCatalogEntryView.class)
public class EditCatalogEntryView {
	private Logger logger = LoggerFactory.getLogger(EditCatalogEntryView.class);

	private Display display = Display.getDefault();
	private Shell shell;
	private LocationCatalogEntry entryToModify;
	private Bundle bundle = FrameworkUtil.getBundle(this.getClass());
	
	@Reference
	private EIMService eimService;
	

	public void showModifyEntryView(LocationCatalogEntry entry, String modifyTarget) {
		entryToModify = entry;
		String name = null;
		if(modifyTarget.equals("installation")) {
			name = entry.getInstallationName();
		} else if (modifyTarget.equals("workspace")) {
			name = entry.getWorkspaceName();
		}
		GridLayout shellLayout = new GridLayout(2, false);
		GridData shellData = new GridData(GridData.FILL_HORIZONTAL);
		shell = new Shell(display, SWT.DIALOG_TRIM | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL);
		shell.setSize(1200, 600);
		shell.setLayout(shellLayout);
		shell.setLayoutData(shellData);
		shell.setText("Modify Entry " + name);
		try {
			Image taskBarImage = new Image(display, bundle.getEntry("icons/EIM-Color_512x.png").openStream());
			shell.setImage(taskBarImage);
		} catch (Exception e) {
			logger.error("Error loading the EIM icon!");
			e.printStackTrace();
		}

		Label titleLabel = new Label(shell, SWT.NONE);
		titleLabel.setFont(new Font(display, "Roboto", 16, SWT.NORMAL));
		titleLabel.setText("Modify entry " + name);
		titleLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));

		Label separator = new Label(shell, SWT.SEPARATOR | SWT.SHADOW_OUT | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		Composite nameComposite = new Composite(shell, SWT.None);
		nameComposite.setLayout(shellLayout);
		nameComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 2));

		Label nameLabel = new Label(nameComposite, SWT.NONE);
		nameLabel.setFont(new Font(display, "Roboto", 13, SWT.NORMAL));
		nameLabel.setText("Name:");
		nameLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

		Text enterNewName = new Text(nameComposite, SWT.FLAT | SWT.SINGLE | SWT.BORDER);
		enterNewName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		enterNewName.setText(name);

		Composite settingsComposite = new Composite(shell, SWT.NONE);
		settingsComposite.setLayout(new GridLayout(1, false));
		settingsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 5));

		Label settingsLabel = new Label(settingsComposite, SWT.NONE);
		settingsLabel.setFont(new Font(display, "Roboto", 13, SWT.NORMAL));
		settingsLabel.setText("Installation Arguments:");
		settingsLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false));

		Table settings = new Table(settingsComposite, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
		settings.setLinesVisible(true);
		settings.setHeaderVisible(true);
		settings.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		TableLayout tableLayout = new TableLayout();
		tableLayout.addColumnData(new ColumnWeightData(50));
		tableLayout.addColumnData(new ColumnWeightData(50));

		settings.setLayout(tableLayout);

		TableColumn argument = new TableColumn(settings, SWT.NONE);
		argument.setText("VM-Argument");
		TableColumn argumentValue = new TableColumn(settings, SWT.NONE);
		argumentValue.setText("Argument Value");

		TableItem item = new TableItem(settings, SWT.NONE);
		item.setText(0, "Not yet");
		item.setText(1, "implemented :(");

		settings.getColumn(0).pack();
		settings.getColumn(1).pack();

		settingsComposite.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				Rectangle area = settingsComposite.getClientArea();
				for (TableColumn column : settings.getColumns()) {
					column.setWidth(area.width / 2);

				}

			}

		});

		Label separatorEnd = new Label(shell, SWT.SEPARATOR | SWT.SHADOW_OUT | SWT.HORIZONTAL);
		separatorEnd.setLayoutData(new GridData(SWT.FILL, SWT.END, true, false, 2, 1));

		Button okButton = new Button(shell, SWT.FLAT);
		okButton.setText("Save");
		okButton.setLayoutData(new GridData(SWT.END, SWT.END, true, false, 1, 1));
		okButton.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event event) {
				saveChanges(entryToModify, enterNewName.getText(), modifyTarget);

			}
		});

		Button cancelButton = new Button(shell, SWT.FLAT);
		cancelButton.setText("Cancel");
		cancelButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.END, true, false, 1, 1));

		cancelButton.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event event) {
				cancelButton();
			}
		});

		shell.pack();
		shell.open();

	}

	private void saveChanges(LocationCatalogEntry entry, String newName, String modifyTarget) {
		if(modifyTarget.equals("installation")) {
			eimService.renameInstallation(entry, newName);
			entry.setInstallationName(newName);
		} else if(modifyTarget.equals("workspace")) {
			eimService.renameWorkspace(entry, newName);
			entry.setWorkspaceName(newName);
		}
		shell.dispose();
	}

	private void cancelButton() {
		shell.dispose();
	}

}
