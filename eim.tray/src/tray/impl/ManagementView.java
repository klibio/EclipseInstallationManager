package tray.impl;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eim.api.EIMService;
import eim.api.LocationCatalogEntry;

@Component(service = ManagementView.class)
public class ManagementView {
	private Logger logger = LoggerFactory.getLogger(ManagementView.class);

	private Display display = Display.getDefault();
	private Shell shell;
	private Bundle bundle = FrameworkUtil.getBundle(this.getClass());
	private Text searchBar;
	private ScrolledComposite scrolledCompositeFirstTab;
	private ScrolledComposite scrolledCompositeSecondTab;

	private LinkedHashMap<LocationCatalogEntry, LinkedList<LocationCatalogEntry>> installations;
	LinkedHashMap<LocationCatalogEntry, LinkedList<LocationCatalogEntry>> workspaces;
	private LinkedList<LocationCatalogEntry> uniqueInstallations;
	private LinkedList<LocationCatalogEntry> uniqueWorkspaces;
	private LinkedList<LocationCatalogEntry> shownFirstTabItems;
	private LinkedList<LocationCatalogEntry> shownSecondTabItems;
	private Color lightBlue = new Color(new RGB(158, 180, 240));
	private Color white = new Color(new RGB(255, 255, 255));
	private Color lightGray = new Color(new RGB(240, 240, 240));

	@Reference(cardinality = ReferenceCardinality.OPTIONAL)
	private DataProvider dataController;

	@Reference
	private EIMService eclService;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL)
	private EditCatalogEntryView editInstallationView;

	@Activate
	public void activate(BundleContext context) {
		logger.debug("Activating ManagementView component");
	}

	public void showOverviewMenu() {

		if (dataController.equals(null)) {
			logger.error("Controller is null!");
			throw new NullPointerException();
		}
		installations = dataController.getInstallationMap();
		uniqueInstallations = dataController.getInstallations();
		uniqueWorkspaces = dataController.getWorkspaces();

		shownFirstTabItems = new LinkedList<>();
		shownFirstTabItems.addAll(uniqueInstallations);

		shownSecondTabItems = new LinkedList<>();
		shownSecondTabItems.addAll(uniqueWorkspaces);

		// Create the Display and Shell
		shell = new Shell(display);
		shell.setText("Eclipse Installation Manager");
		shell.setSize(800, 500);
		shell.setLayout(new GridLayout(1, false)); // Single column layout
		try {
			Image taskBarImage = new Image(display, bundle.getEntry("icons/EIM-Color_512x.png").openStream());
			shell.setImage(taskBarImage);
		} catch (Exception e) {
			logger.error("Error loading the EIM icon!");
			e.printStackTrace();
		}

		// Create the search bar and add it to the shell's top position
		searchBar = new Text(shell, SWT.SEARCH | SWT.BORDER);
		searchBar.setFont(new Font(display, "Roboto", 16, SWT.NORMAL));
		searchBar.setMessage("Search...");
		searchBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		// Create a TabFolder to hold the tabs
		TabFolder tabFolder = new TabFolder(shell, SWT.NONE);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Create the "Installations" tab
		TabItem tab1 = new TabItem(tabFolder, SWT.NONE);
		tab1.setText("Installations");

		// Create the "Workspaces" tab
		TabItem tab2 = new TabItem(tabFolder, SWT.NONE);
		tab2.setText("Workspaces");

		// Add ModifyListener to the search bar (called whenever the text changes)
		searchBar.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				filterLists();
			}
		});
		scrolledCompositeFirstTab = createScrolledComposites(tabFolder);
		scrolledCompositeSecondTab = createScrolledComposites(tabFolder);
		tab1.setControl(scrolledCompositeFirstTab);
		tab2.setControl(scrolledCompositeSecondTab);

		generateFirstTabContents();
		generateSecondTabContents();

		// Set the default selected tab to "Workspaces"
		tabFolder.setSelection(tab1);

		shell.open();
	}

	private static ScrolledComposite createScrolledComposites(Composite parent) {
		ScrolledComposite scrolled = new ScrolledComposite(parent, SWT.V_SCROLL);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		scrolled.setAlwaysShowScrollBars(true);
		scrolled.setMinSize(500, 700);

		scrolled.addListener(SWT.Resize, event -> {
			final int width = scrolled.getClientArea().width;
			scrolled.setMinSize(scrolled.getContent().computeSize(width, SWT.DEFAULT));
		});

		return scrolled;
	}

	private void filterLists() {
		String searchQuery = searchBar.getText().toLowerCase();
		filterList(uniqueInstallations, searchQuery.isEmpty() ? null : searchQuery, 1);
		filterList(uniqueWorkspaces, searchQuery.isEmpty() ? null : searchQuery, 2);
	}

	private void filterList(LinkedList<LocationCatalogEntry> originalList, String searchQuery, int tabNumber) {
		LinkedList<LocationCatalogEntry> filteredList = new LinkedList<>();
		if (searchQuery == null) {
			filteredList.addAll(originalList);
		} else {
			for (LocationCatalogEntry catalogEntry : originalList) {
				if (tabNumber == 1
						&& (catalogEntry.getInstallationFolderName().toLowerCase().contains(searchQuery)
						|| catalogEntry.getInstallationName().toLowerCase().contains(searchQuery))) {
					filteredList.add(catalogEntry);
				} else if (tabNumber == 2
						&& (catalogEntry.getWorkspaceFolderName().toLowerCase().contains(searchQuery)
						|| catalogEntry.getWorkspaceName().toLowerCase().contains(searchQuery))) {
					filteredList.add(catalogEntry);
				}
			}
		}

		if (tabNumber == 1) {
			// Get rid of old Composite
			scrolledCompositeFirstTab.getContent().dispose();

			// Update externalList with filtered Items
			shownFirstTabItems = filteredList;

			generateFirstTabContents();

			int width = scrolledCompositeFirstTab.getClientArea().width;
			scrolledCompositeFirstTab
					.setMinSize(scrolledCompositeFirstTab.getContent().computeSize(width, SWT.DEFAULT));
			scrolledCompositeFirstTab.layout(true, true);
			scrolledCompositeFirstTab.requestLayout();
		} else if (tabNumber == 2) {
			// Get rid of old Composite
			scrolledCompositeSecondTab.getContent().dispose();

			// Update externalList with filtered Items
			shownSecondTabItems = filteredList;

			generateSecondTabContents();

			int width = scrolledCompositeSecondTab.getClientArea().width;
			scrolledCompositeSecondTab
					.setMinSize(scrolledCompositeSecondTab.getContent().computeSize(width, SWT.DEFAULT));
			scrolledCompositeSecondTab.layout(true, true);
			scrolledCompositeSecondTab.requestLayout();
		}

	}

	/*
	 * Generates the content for the first tab, based on the list firstTabItems
	 */
	private void generateFirstTabContents() {
		Composite installationTabComposite = new Composite(scrolledCompositeFirstTab, SWT.NONE);
		installationTabComposite.setLayout(new GridLayout(1, false));
		installationTabComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		installationTabComposite.setBackground(new Color(new RGB(240, 240, 240)));

		installationTabComposite.requestLayout();

		scrolledCompositeFirstTab.setContent(installationTabComposite);

		for (LocationCatalogEntry entry : shownFirstTabItems) {
			String name = entry.getInstallationName();
			String path = entry.getInstallationPath().toAbsolutePath().toString();

			Composite listItemComposite = new Composite(installationTabComposite, SWT.NONE);
			GridLayout listItemLayout = new GridLayout(2, false); // Set GridLayout with 2 columns
			listItemLayout.marginWidth = 5; // Remove the default margin
			listItemLayout.horizontalSpacing = 10; // Add spacing between the items
			listItemComposite.setLayout(listItemLayout);
			GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.grabExcessHorizontalSpace = true;
			listItemComposite.setLayoutData(gridData); // Set to fill horizontally

			Composite labelComposite = new Composite(listItemComposite, SWT.NONE);
			labelComposite.setLayout(new GridLayout(1, false));
			labelComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BEGINNING, true, false));
			labelComposite.setBackground(white);

			Label nameLabel = new Label(labelComposite, SWT.NULL);
			nameLabel.setFont(new Font(display, "Roboto", 16, SWT.NORMAL));

			if (name.equals("installation")) {
				nameLabel.setText(entry.getInstallationFolderName());
			} else {
				nameLabel.setText(name);
			}

			nameLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			// ToolBar for the Buttons on the right
			ToolBar tools = new ToolBar(listItemComposite, SWT.FLAT);
			tools.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
			tools.setBackground(white);

			ToolItem editItem = new ToolItem(tools, SWT.PUSH);
			try {
				Image editPen = new Image(display, bundle.getEntry("icons/edit.png").openStream());
				editItem.setImage(editPen);
			} catch (IOException e) {
				logger.error("Failed loading edit.png");
				e.printStackTrace();
			}
			editItem.addListener(SWT.Selection, new Listener() {

				@Override
				public void handleEvent(Event event) {
					editInstallationView.showModifyEntryView(entry, "installation");
				}

			});

			ToolItem deleteItem = new ToolItem(tools, SWT.PUSH);
			try {
				Image trashCan = new Image(display, bundle.getEntry("icons/trashCan.png").openStream());
				deleteItem.setImage(trashCan);
			} catch (IOException e) {
				logger.error("Failed loading trashCan.png");
				e.printStackTrace();
			}

			Label descrLabel = new Label(labelComposite, SWT.NULL);
			descrLabel.setFont(new Font(display, "Roboto", 10, SWT.NORMAL));
			descrLabel.setText(path);
			descrLabel.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, false));
			descrLabel.setForeground(new Color(new RGB(112, 115, 125)));

			listItemComposite.setBackground(white);
			nameLabel.setBackground(white);
			descrLabel.setBackground(white);

			Listener changeColorEnterListener = new Listener() {
				@Override
				public void handleEvent(Event event) {
					listItemComposite.setBackground(lightBlue);
					labelComposite.setBackground(lightBlue);
					nameLabel.setBackground(lightBlue);
					tools.setBackground(lightBlue);
					descrLabel.setBackground(lightBlue);
					listItemComposite.setCursor(new Cursor(display, SWT.CURSOR_HAND));
					// nameLabel.setCursor(new Cursor(display, SWT.CURSOR_HAND));
				}
			};

			Listener changeColorExitListener = new Listener() {
				@Override
				public void handleEvent(Event event) {
					listItemComposite.setBackground(white);
					labelComposite.setBackground(white);
					tools.setBackground(white);
					nameLabel.setBackground(white);
					descrLabel.setBackground(white);
				}
			};
			Listener changeColorDeleteEnterListener = new Listener() {

				@Override
				public void handleEvent(Event event) {
					try {
						Image trashCan = new Image(display, bundle.getEntry("icons/trashCan.png").openStream());
						deleteItem.setImage(trashCan);
					} catch (Exception e) {
						logger.error("Failed loading trashCan.png");
						e.printStackTrace();
					}
					listItemComposite.setBackground(lightBlue);
					labelComposite.setBackground(lightBlue);
					nameLabel.setBackground(lightBlue);
					tools.setBackground(lightBlue);
					descrLabel.setBackground(lightBlue);
					listItemComposite.setCursor(new Cursor(display, SWT.CURSOR_HAND));
				}
			};

			listItemComposite.addListener(SWT.MouseEnter, changeColorEnterListener);
			listItemComposite.addListener(SWT.MouseExit, changeColorExitListener);
			labelComposite.addListener(SWT.MouseEnter, changeColorEnterListener);
			labelComposite.addListener(SWT.MouseExit, changeColorExitListener);
			nameLabel.addListener(SWT.MouseEnter, changeColorEnterListener);
			nameLabel.addListener(SWT.MouseExit, changeColorExitListener);
			descrLabel.addListener(SWT.MouseEnter, changeColorEnterListener);
			descrLabel.addListener(SWT.MouseExit, changeColorExitListener);
			tools.addListener(SWT.MouseEnter, changeColorDeleteEnterListener);

			// Begin Content
			Composite content = new Composite(listItemComposite, SWT.BORDER | SWT.CENTER);
			content.setLayout(new GridLayout(1, false));
			GridData data = new GridData(SWT.FILL, SWT.FILL, false, false);
			content.setLayoutData(data);
			content.setBackground(lightGray);

			MouseListener mouseClickListener = new MouseListener() {
				private boolean doubleClick;

				@Override
				public void mouseUp(MouseEvent e) {
					// TODO Auto-generated method stub

				}

				@Override
				public void mouseDown(MouseEvent e) {
					doubleClick = false;

					// Set doubleclick time to 150ms
					int time = Display.getDefault().getDoubleClickTime() - 350;
					Display.getDefault().timerExec(time, new Runnable() {
						public void run() {
							if (!doubleClick) {
								if (e.button == 3) {
									System.out.println("Single Right Click Event wubwub!");
								} else {
									System.out.println("Single Left Click Event woooooo!");
									data.exclude = !data.exclude;
									content.setVisible(!data.exclude);
									content.getParent().pack();
									content.layout();
									content.getParent().requestLayout();
									int width = scrolledCompositeFirstTab.getClientArea().width;
									scrolledCompositeFirstTab.setMinSize(
											scrolledCompositeFirstTab.getContent().computeSize(width, SWT.DEFAULT));
									scrolledCompositeFirstTab.layout(true, true);
									scrolledCompositeFirstTab.requestLayout();
								}

							}
						}
					});

				}

				@Override
				public void mouseDoubleClick(MouseEvent e) {
					doubleClick = true;
					System.out.println("DOUBLE CLICK YEAH!");

				}
			};
			listItemComposite.addMouseListener(mouseClickListener);
			nameLabel.addMouseListener(mouseClickListener);
			descrLabel.addMouseListener(mouseClickListener);
			LinkedList<LocationCatalogEntry> mappedWorkspaces = new LinkedList<>();
			for (Entry<LocationCatalogEntry, LinkedList<LocationCatalogEntry>> mapEntry : installations.entrySet()) {
				LocationCatalogEntry installation = mapEntry.getKey();
				LinkedList<LocationCatalogEntry> workspaceList = mapEntry.getValue();
				if (installation.getID() == entry.getID()) {
					mappedWorkspaces.addAll(workspaceList);
				}
			}

			for (LocationCatalogEntry workspace : mappedWorkspaces) {
				String workspaceName = workspace.getWorkspaceName();
				if(workspaceName.equals("workspace")) {
					workspaceName = workspace.getWorkspaceFolderName();
				}
				String workspacePath = workspace.getWorkspacePath().toAbsolutePath().toString();

				Composite contentItemComposite = new Composite(content, SWT.NONE);
				GridLayout contentItemLayout = new GridLayout(2, false); // Set GridLayout with 2 columns
				contentItemLayout.marginWidth = 5; // Remove the default margin
				contentItemLayout.horizontalSpacing = 10; // Add spacing between the items
				contentItemComposite.setLayout(contentItemLayout);
				GridData contentGridData = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL);
				gridData.grabExcessHorizontalSpace = true;
				contentItemComposite.setLayoutData(contentGridData); // Set to fill horizontally
				contentItemComposite.setBackground(lightGray);

				Composite contentLabelComposite = new Composite(contentItemComposite, SWT.NONE);
				contentLabelComposite.setLayout(new GridLayout(1, false));
				contentLabelComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BEGINNING, true, false));
				contentLabelComposite.setBackground(lightGray);

				Label contentNameLabel = new Label(contentLabelComposite, SWT.NULL);
				contentNameLabel.setFont(new Font(display, "Roboto", 16, SWT.NORMAL));

				if (workspaceName.equals("ws")) {
					contentNameLabel.setText(workspace.getWorkspaceFolderName());
				} else {
					contentNameLabel.setText(workspaceName);
				}

				contentNameLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				contentNameLabel.setBackground(lightGray);

				// ToolBar for the Buttons on the right
				ToolBar contentTools = new ToolBar(contentItemComposite, SWT.FLAT);
				contentTools.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
				contentTools.setBackground(lightGray);
				
				ToolItem editWorkspaceItem = new ToolItem(contentTools, SWT.PUSH);
				try {
					Image editPen = new Image(display, bundle.getEntry("icons/edit.png").openStream());
					editWorkspaceItem.setImage(editPen);
				} catch (IOException e) {
					logger.error("Failed loading edit.png");
					e.printStackTrace();
				}
				editWorkspaceItem.addListener(SWT.Selection, new Listener() {

					@Override
					public void handleEvent(Event event) {
						editInstallationView.showModifyEntryView(entry, "workspace");
					}

				});
				
				ToolItem contentDeleteItem = new ToolItem(contentTools, SWT.PUSH);
				try {
					Image trashCan = new Image(display, bundle.getEntry("icons/trashCan.png").openStream());
					contentDeleteItem.setImage(trashCan);
				} catch (IOException e1) {
					logger.error("Failed loading trashCan.png");
					e1.printStackTrace();
				}

				Label contentDescriptionLabel = new Label(contentLabelComposite, SWT.NULL);
				contentDescriptionLabel.setFont(new Font(display, "Roboto", 10, SWT.NORMAL));
				contentDescriptionLabel.setText(workspacePath);
				contentDescriptionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, false));
				contentDescriptionLabel.setForeground(new Color(new RGB(112, 115, 125)));
				contentDescriptionLabel.setBackground(lightGray);

				Listener changeColorEnterSubListener = new Listener() {

					@Override
					public void handleEvent(Event event) {
						contentItemComposite.setBackground(lightBlue);
						contentLabelComposite.setBackground(lightBlue);
						contentNameLabel.setBackground(lightBlue);
						contentTools.setBackground(lightBlue);
						contentDescriptionLabel.setBackground(lightBlue);
						contentItemComposite.setCursor(new Cursor(display, SWT.CURSOR_HAND));
					}
				};

				Listener changeColorExitSubListener = new Listener() {

					@Override
					public void handleEvent(Event event) {
						contentItemComposite.setBackground(lightGray);
						contentLabelComposite.setBackground(lightGray);
						contentTools.setBackground(lightGray);
						contentNameLabel.setBackground(lightGray);
						contentDescriptionLabel.setBackground(lightGray);
					}
				};
				Listener changeColorDeleteEnterSubListener = new Listener() {

					@Override
					public void handleEvent(Event event) {
						contentItemComposite.setBackground(lightBlue);
						contentLabelComposite.setBackground(lightBlue);
						contentNameLabel.setBackground(lightBlue);
						contentTools.setBackground(lightBlue);
						contentDescriptionLabel.setBackground(lightBlue);
						contentItemComposite.setCursor(new Cursor(display, SWT.CURSOR_HAND));
					}
				};

				contentItemComposite.addListener(SWT.MouseEnter, changeColorEnterSubListener);
				contentItemComposite.addListener(SWT.MouseExit, changeColorExitSubListener);
				contentNameLabel.addListener(SWT.MouseEnter, changeColorEnterSubListener);
				contentNameLabel.addListener(SWT.MouseExit, changeColorExitSubListener);
				contentDeleteItem.addListener(SWT.MouseEnter, changeColorEnterSubListener);
				contentDeleteItem.addListener(SWT.MouseExit, changeColorExitSubListener);
				contentDescriptionLabel.addListener(SWT.MouseEnter, changeColorEnterSubListener);
				contentDescriptionLabel.addListener(SWT.MouseExit, changeColorExitSubListener);
				contentTools.addListener(SWT.MouseEnter, changeColorDeleteEnterSubListener);
				MouseListener mouseSubClickListener = new MouseListener() {
					private boolean doubleClick;

					@Override
					public void mouseUp(MouseEvent e) {
						// TODO Auto-generated method stub

					}

					@Override
					public void mouseDown(MouseEvent e) {
						doubleClick = false;

						// Set doubleclick time to 150ms
						int time = Display.getDefault().getDoubleClickTime() - 350;
						Display.getDefault().timerExec(time, new Runnable() {
							public void run() {
								if (!doubleClick) {
									if (e.button == 3) {
										System.out.println("Single Right Click Event SUB MENU wubwub!");
									} else {
										eclService.startEntry(entry);
									}

								}
							}
						});

					}

					@Override
					public void mouseDoubleClick(MouseEvent e) {
						doubleClick = true;
						System.out.println("SUB MENU DOUBLE CLICK YEAH!");

					}
				};
				contentItemComposite.addMouseListener(mouseSubClickListener);
				contentNameLabel.addMouseListener(mouseSubClickListener);
				contentDescriptionLabel.addMouseListener(mouseSubClickListener);

			}
			data.exclude = !data.exclude;
			content.setVisible(!data.exclude);
			content.getParent().pack();
			content.layout();
			content.getParent().requestLayout();
		}
	}

	private void generateSecondTabContents() {
		Composite secondTabComposite = new Composite(scrolledCompositeSecondTab, SWT.NONE);
		secondTabComposite.setLayout(new GridLayout(1, false));
		secondTabComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		secondTabComposite.setBackground(new Color(new RGB(240, 240, 240)));

		secondTabComposite.requestLayout();

		scrolledCompositeSecondTab.setContent(secondTabComposite);

		for (LocationCatalogEntry entry : shownSecondTabItems) {
			String workspaceName = entry.getWorkspaceName();
			if(workspaceName.equals("workspace")) {
				workspaceName = entry.getWorkspaceFolderName();
			}
			String path = entry.getWorkspacePath().toAbsolutePath().toString();

			Composite listItemComposite = new Composite(secondTabComposite, SWT.NONE);
			GridLayout listItemLayout = new GridLayout(2, false); // Set GridLayout with 2 columns
			listItemLayout.marginWidth = 5; // Remove the default margin
			listItemLayout.horizontalSpacing = 10; // Add spacing between the items
			listItemComposite.setLayout(listItemLayout);
			GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.grabExcessHorizontalSpace = true;
			listItemComposite.setLayoutData(gridData); // Set to fill horizontally

			Composite labelComposite = new Composite(listItemComposite, SWT.NONE);
			labelComposite.setLayout(new GridLayout(1, false));
			labelComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BEGINNING, true, false));
			labelComposite.setBackground(white);

			Label nameLabel = new Label(labelComposite, SWT.NULL);
			nameLabel.setFont(new Font(display, "Roboto", 16, SWT.NORMAL));
			nameLabel.setText(workspaceName);
			nameLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			// ToolBar for the Buttons on the right
			ToolBar tools = new ToolBar(listItemComposite, SWT.FLAT);
			tools.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
			tools.setBackground(white);
			
			ToolItem editWorkspaceItem = new ToolItem(tools, SWT.PUSH);
			try {
				Image editPen = new Image(display, bundle.getEntry("icons/edit.png").openStream());
				editWorkspaceItem.setImage(editPen);
			} catch (IOException e) {
				logger.error("Failed loading edit.png");
				e.printStackTrace();
			}
			editWorkspaceItem.addListener(SWT.Selection, new Listener() {

				@Override
				public void handleEvent(Event event) {
					editInstallationView.showModifyEntryView(entry, "workspace");
				}

			});
			
			ToolItem deleteItem = new ToolItem(tools, SWT.PUSH);
			try {
				Image trashCanLightGray = new Image(display, bundle.getEntry("icons/trashCan.png").openStream());
				deleteItem.setImage(trashCanLightGray);
			} catch (Exception e) {
				logger.error("Failed loading trashCan.png");
				e.printStackTrace();
			}

			Label descrLabel = new Label(labelComposite, SWT.NULL);
			descrLabel.setFont(new Font(display, "Roboto", 10, SWT.NORMAL));
			descrLabel.setText(path);
			descrLabel.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, false));
			descrLabel.setForeground(new Color(new RGB(112, 115, 125)));

			listItemComposite.setBackground(white);
			nameLabel.setBackground(white);
			descrLabel.setBackground(white);
			Listener changeColorEnterListener = new Listener() {

				@Override
				public void handleEvent(Event event) {
					listItemComposite.setBackground(lightBlue);
					labelComposite.setBackground(lightBlue);
					nameLabel.setBackground(lightBlue);
					tools.setBackground(lightBlue);
					descrLabel.setBackground(lightBlue);
					listItemComposite.setCursor(new Cursor(display, SWT.CURSOR_HAND));
				}
			};

			Listener changeColorExitListener = new Listener() {

				@Override
				public void handleEvent(Event event) {
					listItemComposite.setBackground(white);
					labelComposite.setBackground(white);
					tools.setBackground(white);
					nameLabel.setBackground(white);
					descrLabel.setBackground(white);
				}
			};
			Listener changeColorDeleteEnterListener = new Listener() {

				@Override
				public void handleEvent(Event event) {
					listItemComposite.setBackground(lightBlue);
					labelComposite.setBackground(lightBlue);
					nameLabel.setBackground(lightBlue);
					tools.setBackground(lightBlue);
					descrLabel.setBackground(lightBlue);
					listItemComposite.setCursor(new Cursor(display, SWT.CURSOR_HAND));
				}
			};

			listItemComposite.addListener(SWT.MouseEnter, changeColorEnterListener);
			listItemComposite.addListener(SWT.MouseExit, changeColorExitListener);
			labelComposite.addListener(SWT.MouseEnter, changeColorEnterListener);
			labelComposite.addListener(SWT.MouseExit, changeColorExitListener);
			nameLabel.addListener(SWT.MouseEnter, changeColorEnterListener);
			nameLabel.addListener(SWT.MouseExit, changeColorExitListener);
			descrLabel.addListener(SWT.MouseEnter, changeColorEnterListener);
			descrLabel.addListener(SWT.MouseExit, changeColorExitListener);
			tools.addListener(SWT.MouseEnter, changeColorDeleteEnterListener);

		}
	}

}