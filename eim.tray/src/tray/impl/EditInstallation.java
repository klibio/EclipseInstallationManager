package tray.impl;

import java.util.Collections;
import java.util.LinkedList;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import eim.api.LocationCatalogEntry;

@Component
public class EditInstallation {
	private Display display = Display.getDefault();
	private Shell shell = new Shell(display, SWT.DIALOG_TRIM | SWT.TITLE
			|SWT.BORDER |SWT.APPLICATION_MODAL);
	private LocationCatalogEntry entryToModify;

	public void showModifyEntryView() {
		GridLayout shellLayout = new GridLayout(2, false);
		GridData shellData = new GridData(GridData.FILL_HORIZONTAL);
		shell.setSize(1200, 600);
		shell.setLayout(shellLayout);
		shell.setLayoutData(shellData);
		shell.setText("Modify Entry " + entryToModify.getName());
		
		Label titleLabel = new Label(shell, SWT.NONE);
		titleLabel.setFont(new Font(display, "Roboto", 16, SWT.NORMAL));
		titleLabel.setText("Modify entry " + entryToModify.getName());
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
		enterNewName.setText(entryToModify.getName());

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
		okButton.addListener(SWT.Selection,new Listener() {

			@Override
			public void handleEvent(Event event) {
				saveChanges(enterNewName.getText());
				
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
	
	@Activate
	public void activate(BundleContext context) {

		
	}
	
	private void saveChanges(String text) {
		System.out.println(text);
	}
	
	private void cancelButton() {
		shell.dispose();
	}

}

