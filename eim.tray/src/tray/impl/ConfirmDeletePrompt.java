package tray.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eim.api.EIMService;

@Component(service = ConfirmDeletePrompt.class)
public class ConfirmDeletePrompt {
	private Logger logger = LoggerFactory.getLogger(ConfirmDeletePrompt.class);
	private Bundle bundle = FrameworkUtil.getBundle(this.getClass());
	
	private Path pathToDelete;
	private Path parentPath;
	private String message = "Do you really want to delete the following path?";
	private Display display = Display.getDefault();
	private Shell shell;
	
	private boolean deleted = false;

	@Reference
	private EIMService eimService;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	@Activate
	public void activate() {
		logger.debug("Activating ConfirmDeletePrompt component");
	}

	public void open(Path path) {
		this.pathToDelete = path;
		this.parentPath = path.getParent();
		shell = new Shell(display, SWT.CLOSE | SWT.TITLE);
		shell.setText("Confirm deletion!");
		try {
			Image taskBarImage = new Image(display, bundle.getEntry("icons/EIM-Color_512x.png").openStream());
			shell.setImage(taskBarImage);
		} catch (Exception e) {
			logger.error("Error loading the EIM icon!");
			e.printStackTrace();
		}
		createContents(shell);
		shell.open();
	}
	
	public Shell getShell() {
		return shell;
	}
	
	public boolean getDeleted() {
		return deleted;
	}

	private void createContents(Shell shell) {
		shell.setLayout(new GridLayout(2, true));

		Label label = new Label(shell, SWT.NONE);
		label.setText(message);
		label.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 2, 1));
		Text pathText = new Text(shell, SWT.BORDER);
		pathText.setEditable(false);
		pathText.setEnabled(false);
		pathText.setText(pathToDelete.toString());
		pathText.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 2, 1));
		Button checkboxParentFolder = new Button(shell, SWT.CHECK);
		checkboxParentFolder.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 2, 1));
		checkboxParentFolder.setText("Delete parent folder");

		Composite parentFolderComposite = new Composite(shell, SWT.BORDER);
		parentFolderComposite.setLayout(new GridLayout(2, false));
		GridData contentData = new GridData(SWT.CENTER, SWT.CENTER, true, true, 2, 5);
		parentFolderComposite.setLayoutData(contentData);

		LinkedList<Path> parentFolderContents = getFolderContents(parentPath);

		Image warning = display.getSystemImage(SWT.ICON_WARNING);
		Label warningImage = new Label(parentFolderComposite, SWT.NONE);
		warningImage.setImage(warning);
		warningImage.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1));
		Label contentsLabel = new Label(parentFolderComposite, SWT.NONE);
		if (parentFolderContents.size() == 0) {
			contentsLabel.setText("The root folder is empty!");
		} else {
			contentsLabel.setText("The root folder contains the following directories and files:");
		}

		contentsLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 1, 1));

		for (Path path : parentFolderContents) {
			Label pathLabel = new Label(parentFolderComposite, SWT.NONE);
			pathLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
			if (path.toFile().isFile()) {
				pathLabel.setText("File: " + path.getFileName().toString());
			} else if (path.toFile().isDirectory()) {
				pathLabel.setText("Directory: " + path.getFileName().toString());
			}

		}

		parentFolderComposite.setVisible(false);
		contentData.exclude = true;

		checkboxParentFolder.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Button btn = (Button) e.getSource();
				boolean checked = btn.getSelection();

				if (checked) {
					parentFolderComposite.setVisible(true);
					contentData.exclude = false;
					pathText.setText(parentPath.toString());
				} else {
					parentFolderComposite.setVisible(false);
					contentData.exclude = true;
					pathText.setText(pathToDelete.toString());
				}
				parentFolderComposite.layout();
				parentFolderComposite.pack();
				shell.layout();
				shell.pack();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// Do nothing
			}

		});
		
		Label separatorEnd = new Label(shell, SWT.SEPARATOR | SWT.SHADOW_OUT | SWT.HORIZONTAL);
		separatorEnd.setLayoutData(new GridData(SWT.FILL, SWT.END, true, false, 2, 1));
		
		Label refreshHint = new Label(shell, SWT.NONE);
		refreshHint.setText("Please Refresh the tray application to synchronize the modifications!");
		refreshHint.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 2, 1));
		
		Button okButton = new Button(shell, SWT.PUSH);
		okButton.setLayoutData(new GridData(SWT.END, SWT.END, true, false, 1, 1));
		okButton.setText("Delete");
		okButton.setForeground(new Color(new RGB(100, 100, 100)));
		okButton.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event event) {
				if(checkboxParentFolder.getSelection()) {
					eimService.deletePath(parentPath);
				} else {
					eimService.deletePath(pathToDelete);
				}
				deleted = true;
				shell.close();
			}
		});
		
		Button cancelButton = new Button(shell, SWT.PUSH);
		cancelButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.END, true, false, 1, 1));
		cancelButton.setText("Cancel");
		Font arial = new Font(display, "Arial", 9, SWT.BOLD);
		cancelButton.setFont(arial);
		cancelButton.setBackground(new Color(new RGB(0, 95, 184)));
		cancelButton.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
		cancelButton.addListener(SWT.Selection, new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				deleted = false;
				shell.dispose();
			}
		});

		shell.setDefaultButton(cancelButton);
		shell.pack();
	}

	public LinkedList<Path> getFolderContents(Path path) {
		LinkedList<Path> folderContents = new LinkedList<>();

		try {
			Stream<Path> paths = Files.walk(path, 1);

			paths.filter(p -> (!p.equals(path))).collect(Collectors.toCollection(() -> folderContents));

			paths.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		folderContents.sort(new Comparator<Path>() {

			@Override
			public int compare(Path p1, Path p2) {
				File f1 = p1.toFile();
				File f2 = p2.toFile();
				if (f1.isDirectory() && f2.isFile()) {
					return -1;
				} else if (f1.isDirectory() && f2.isDirectory()) {
					return 0;
				} else if (f1.isFile() && f2.isFile()) {
					return 0;
				}
				return 1;
			}
		});

		return folderContents;
	}
}
