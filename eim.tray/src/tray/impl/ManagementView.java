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

	private static Color NATIVE_BACKGROUND = getNativeBackgroundColor();
	private static Color LIST_BACKGROUND = getListBackgroundColor();
	private static Color SELECTION_COLOR = getSelectionColor();
	private static Color FOREGROUND_COLOR = getForegroundColor();
	private Image editPen;
	private Image trashCan;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL)
	private DataProvider dataController;

	@Reference
	private EIMService eclService;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL)
	private EditCatalogEntryView editInstallationView;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL)
	private ConfirmDeletePrompt deletePrompt;

	@Activate
	public void activate(BundleContext context) {
		logger.debug("Activating ManagementView component");
		try {
			editPen = new Image(display, bundle.getEntry("icons/edit.png").openStream());
			trashCan = new Image(display, bundle.getEntry("icons/trashCan.png").openStream());
		} catch (IOException e) {
			logger.error("Failed loading icons!");
			e.printStackTrace();
		}
	}

	/**
	 * Sets up the data and UI elements for the displayed data.
	 */
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
		// shell.setBackground(NATIVE_BACKGROUND);
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
		// searchBar.setBackground(NATIVE_BACKGROUND);
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

		scrolledCompositeFirstTab.addListener(SWT.Resize, event -> {
			final int width = scrolledCompositeFirstTab.getClientArea().width;
			scrolledCompositeFirstTab
					.setMinSize(scrolledCompositeFirstTab.getContent().computeSize(width, SWT.DEFAULT));
		});

		scrolledCompositeSecondTab.addListener(SWT.Resize, event -> {
			final int width = scrolledCompositeSecondTab.getClientArea().width;
			scrolledCompositeSecondTab
					.setMinSize(scrolledCompositeSecondTab.getContent().computeSize(width, SWT.DEFAULT));
		});

		generateFirstTabContents();
		generateSecondTabContents();

		// Set the default selected tab to "Workspaces"
		tabFolder.setSelection(tab1);

		shell.open();
	}

	/**
	 * Creates a scrolled Composite for a parent composite which follows resize
	 * events.
	 * 
	 * @param parent The parent composite of the ScrolledComposite.
	 * @return the newly created ScrolledComposite
	 */
	private static ScrolledComposite createScrolledComposites(Composite parent) {
		ScrolledComposite scrolled = new ScrolledComposite(parent, SWT.V_SCROLL);
		// scrolled.setBackground(NATIVE_BACKGROUND);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		scrolled.setAlwaysShowScrollBars(true);
		scrolled.setMinSize(500, 700);

		return scrolled;
	}

	/**
	 * Starts the filtering of both tabs
	 */
	private void filterLists() {
		String searchQuery = searchBar.getText().toLowerCase();
		filterList(uniqueInstallations, searchQuery.isEmpty() ? null : searchQuery, 1);
		filterList(uniqueWorkspaces, searchQuery.isEmpty() ? null : searchQuery, 2);
	}

	/**
	 * Filters the specified list based on a given search query. The tabNumber
	 * specifies which resource is to be filtered.
	 * 
	 * @param originalList The list data structure that is to be filtered.
	 * @param searchQuery  The search query with which to filter
	 * @param tabNumber    This number indicates which resource to filter.
	 */
	private void filterList(LinkedList<LocationCatalogEntry> originalList, String searchQuery, int tabNumber) {
		LinkedList<LocationCatalogEntry> filteredList = new LinkedList<>();
		if (searchQuery == null) {
			filteredList.addAll(originalList);
		} else {
			for (LocationCatalogEntry catalogEntry : originalList) {
				if (tabNumber == 1 && (catalogEntry.getInstallationFolderName().toLowerCase().contains(searchQuery)
						|| catalogEntry.getInstallationName().toLowerCase().contains(searchQuery))) {
					filteredList.add(catalogEntry);
				} else if (tabNumber == 2 && (catalogEntry.getWorkspaceFolderName().toLowerCase().contains(searchQuery)
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

		installationTabComposite.requestLayout();

		scrolledCompositeFirstTab.setContent(installationTabComposite);

		for (LocationCatalogEntry entry : shownFirstTabItems) {
			String name = entry.getInstallationName();
			String path = entry.getInstallationPath().toAbsolutePath().toString();

			Composite listItemComposite = new Composite(installationTabComposite, SWT.NONE);
			listItemComposite.setBackground(NATIVE_BACKGROUND);
			GridLayout listItemLayout = new GridLayout(2, false);
			listItemLayout.marginWidth = 5;
			listItemLayout.horizontalSpacing = 10;
			listItemComposite.setLayout(listItemLayout);
			GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.grabExcessHorizontalSpace = true;
			listItemComposite.setLayoutData(gridData);

			Composite labelComposite = new Composite(listItemComposite, SWT.NONE);
			labelComposite.setLayout(new GridLayout(1, false));
			labelComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BEGINNING, true, false));
			labelComposite.setBackground(NATIVE_BACKGROUND);

			Label nameLabel = new Label(labelComposite, SWT.NULL);
			nameLabel.setFont(new Font(display, "Roboto", 16, SWT.NORMAL));
			nameLabel.setForeground(FOREGROUND_COLOR);

			if (name.equals("installation")) {
				nameLabel.setText(entry.getInstallationFolderName());
			} else {
				nameLabel.setText(name);
			}

			nameLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			// ToolBar for the Buttons on the right
			ToolBar tools = new ToolBar(listItemComposite, SWT.FLAT);
			tools.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
			tools.setBackground(NATIVE_BACKGROUND);

			ToolItem editItem = new ToolItem(tools, SWT.PUSH);
			editItem.setImage(editPen);
			editItem.addListener(SWT.Selection, new Listener() {

				@Override
				public void handleEvent(Event event) {
					editInstallationView.showModifyEntryView(entry, "installation");
				}

			});

			ToolItem deleteItem = new ToolItem(tools, SWT.PUSH);
			deleteItem.setImage(trashCan);
			deleteItem.addListener(SWT.Selection, new Listener() {

				@Override
				public void handleEvent(Event event) {
					deletePrompt.open(entry.getInstallationPath());
				}
			});

			Label descrLabel = new Label(labelComposite, SWT.NULL);
			descrLabel.setFont(new Font(display, "Roboto", 10, SWT.NORMAL));
			descrLabel.setText(path);
			descrLabel.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, false));
			descrLabel.setForeground(new Color(new RGB(112, 115, 125)));

			listItemComposite.setBackground(NATIVE_BACKGROUND);
			nameLabel.setBackground(NATIVE_BACKGROUND);
			descrLabel.setBackground(NATIVE_BACKGROUND);

			Listener changeColorEnterListener = new Listener() {
				@Override
				public void handleEvent(Event event) {
					setCompositesLightBlue(listItemComposite, labelComposite, nameLabel, tools, descrLabel);
				}
			};

			Listener changeColorExitListener = new Listener() {
				@Override
				public void handleEvent(Event event) {
					setCompositesWhite(listItemComposite, labelComposite, nameLabel, tools, descrLabel);
				}
			};
			Listener changeColorDeleteEnterListener = new Listener() {

				@Override
				public void handleEvent(Event event) {
					setCompositesLightBlue(listItemComposite, labelComposite, nameLabel, tools, descrLabel);
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
			content.setBackground(LIST_BACKGROUND);

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
								if (e.button != 3) {
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
					eclService.startEntry(entry, false);

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
				if (workspaceName.equals("workspace")) {
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
				contentItemComposite.setBackground(LIST_BACKGROUND);

				Composite contentLabelComposite = new Composite(contentItemComposite, SWT.NONE);
				contentLabelComposite.setLayout(new GridLayout(1, false));
				contentLabelComposite.setLayoutData(new GridData(SWT.LEFT, SWT.BEGINNING, true, false));
				contentLabelComposite.setBackground(LIST_BACKGROUND);

				Label contentNameLabel = new Label(contentLabelComposite, SWT.NULL);
				contentNameLabel.setFont(new Font(display, "Roboto", 16, SWT.NORMAL));

				if (workspaceName.equals("ws")) {
					contentNameLabel.setText(workspace.getWorkspaceFolderName());
				} else {
					contentNameLabel.setText(workspaceName);
				}

				contentNameLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				contentNameLabel.setBackground(LIST_BACKGROUND);

				// ToolBar for the Buttons on the right
				ToolBar contentTools = new ToolBar(contentItemComposite, SWT.FLAT);
				contentTools.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
				contentTools.setBackground(LIST_BACKGROUND);

				ToolItem editWorkspaceItem = new ToolItem(contentTools, SWT.PUSH);
				editWorkspaceItem.setImage(editPen);
				editWorkspaceItem.addListener(SWT.Selection, new Listener() {

					@Override
					public void handleEvent(Event event) {
						editInstallationView.showModifyEntryView(entry, "workspace");
					}

				});

				ToolItem contentDeleteItem = new ToolItem(contentTools, SWT.PUSH);
				contentDeleteItem.setImage(trashCan);
				contentDeleteItem.addListener(SWT.Selection, new Listener() {

					@Override
					public void handleEvent(Event event) {
						deletePrompt.open(workspace.getWorkspacePath());
					}
				});

				Label contentDescriptionLabel = new Label(contentLabelComposite, SWT.NULL);
				contentDescriptionLabel.setFont(new Font(display, "Roboto", 10, SWT.NORMAL));
				contentDescriptionLabel.setText(workspacePath);
				contentDescriptionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, false));
				contentDescriptionLabel.setForeground(new Color(new RGB(112, 115, 125)));
				contentDescriptionLabel.setBackground(LIST_BACKGROUND);

				Listener changeColorEnterSubListener = new Listener() {

					@Override
					public void handleEvent(Event event) {
						setCompositesLightBlue(contentItemComposite, contentLabelComposite, contentNameLabel,
								contentTools, contentDescriptionLabel);
					}
				};

				Listener changeColorExitSubListener = new Listener() {

					@Override
					public void handleEvent(Event event) {
						setCompositesLightGray(contentItemComposite, contentLabelComposite, contentNameLabel,
								contentTools, contentDescriptionLabel);
					}
				};
				Listener changeColorDeleteEnterSubListener = new Listener() {

					@Override
					public void handleEvent(Event event) {
						setCompositesLightBlue(contentItemComposite, contentLabelComposite, contentNameLabel,
								contentTools, contentDescriptionLabel);
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
										eclService.startEntry(entry, true);
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

	/**
	 * Generates the contents for the workspace list tab.
	 */
	private void generateSecondTabContents() {
		Composite secondTabComposite = new Composite(scrolledCompositeSecondTab, SWT.NONE);
		secondTabComposite.setLayout(new GridLayout(1, false));
		secondTabComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		secondTabComposite.setBackground(LIST_BACKGROUND);

		secondTabComposite.requestLayout();

		scrolledCompositeSecondTab.setContent(secondTabComposite);

		for (LocationCatalogEntry entry : shownSecondTabItems) {
			String workspaceName = entry.getWorkspaceName();
			if (workspaceName.equals("workspace")) {
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

			Label nameLabel = new Label(labelComposite, SWT.NULL);
			nameLabel.setFont(new Font(display, "Roboto", 16, SWT.NORMAL));
			nameLabel.setText(workspaceName);
			nameLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			// ToolBar for the Buttons on the right
			ToolBar tools = new ToolBar(listItemComposite, SWT.FLAT);
			tools.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
			tools.setBackground(NATIVE_BACKGROUND);

			ToolItem editWorkspaceItem = new ToolItem(tools, SWT.PUSH);
			editWorkspaceItem.setImage(editPen);
			editWorkspaceItem.addListener(SWT.Selection, new Listener() {

				@Override
				public void handleEvent(Event event) {
					editInstallationView.showModifyEntryView(entry, "workspace");
				}

			});

			ToolItem deleteItem = new ToolItem(tools, SWT.PUSH);
			deleteItem.setImage(trashCan);
			deleteItem.addListener(SWT.Selection, new Listener() {

				@Override
				public void handleEvent(Event event) {
					deletePrompt.open(entry.getWorkspacePath());
				}
			});

			Label descrLabel = new Label(labelComposite, SWT.NULL);
			descrLabel.setFont(new Font(display, "Roboto", 10, SWT.NORMAL));
			descrLabel.setText(path);
			descrLabel.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, false));
			descrLabel.setForeground(new Color(new RGB(112, 115, 125)));

			listItemComposite.setBackground(NATIVE_BACKGROUND);
			nameLabel.setBackground(NATIVE_BACKGROUND);
			descrLabel.setBackground(NATIVE_BACKGROUND);
			labelComposite.setBackground(NATIVE_BACKGROUND);

			Listener changeColorEnterListener = new Listener() {

				@Override
				public void handleEvent(Event event) {
					setCompositesLightBlue(listItemComposite, labelComposite, nameLabel, tools, descrLabel);
				}
			};

			Listener changeColorExitListener = new Listener() {

				@Override
				public void handleEvent(Event event) {
					setCompositesWhite(listItemComposite, labelComposite, nameLabel, tools, descrLabel);
				}
			};
			Listener changeColorDeleteEnterListener = new Listener() {

				@Override
				public void handleEvent(Event event) {
					setCompositesLightBlue(listItemComposite, labelComposite, nameLabel, tools, descrLabel);
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

	private void setCompositesLightBlue(Composite listItemComposite, Composite labelComposite, Label nameLabel,
			ToolBar tools, Label descrLabel) {
		listItemComposite.setBackground(SELECTION_COLOR);
		labelComposite.setBackground(SELECTION_COLOR);
		nameLabel.setBackground(SELECTION_COLOR);
		tools.setBackground(SELECTION_COLOR);
		descrLabel.setBackground(SELECTION_COLOR);
		listItemComposite.setCursor(new Cursor(display, SWT.CURSOR_HAND));
	}

	private void setCompositesWhite(Composite listItemComposite, Composite labelComposite, Label nameLabel,
			ToolBar tools, Label descrLabel) {
		listItemComposite.setBackground(NATIVE_BACKGROUND);
		labelComposite.setBackground(NATIVE_BACKGROUND);
		tools.setBackground(NATIVE_BACKGROUND);
		nameLabel.setBackground(NATIVE_BACKGROUND);
		descrLabel.setBackground(NATIVE_BACKGROUND);
	}

	private void setCompositesLightGray(Composite listItemComposite, Composite labelComposite, Label nameLabel,
			ToolBar tools, Label descrLabel) {
		listItemComposite.setBackground(LIST_BACKGROUND);
		labelComposite.setBackground(LIST_BACKGROUND);
		nameLabel.setBackground(LIST_BACKGROUND);
		tools.setBackground(LIST_BACKGROUND);
		descrLabel.setBackground(LIST_BACKGROUND);
	}

	private static Color getNativeBackgroundColor() {
		Color targetColor = new Color(new RGB(255, 255, 255));
		/*
		 * if (IS_DARK_THEMED) { targetColor = new Color(new RGB(47, 47, 47)); } else {
		 * targetColor = new Color(new RGB(240, 240, 240)); }
		 */
		return targetColor;
	}

	private static Color getSelectionColor() {
		Color targetColor = new Color(new RGB(158, 180, 240));
		/*
		 * if (IS_DARK_THEMED) { targetColor = new Color(new RGB(77, 77, 77)); } else {
		 * targetColor = new Color(new RGB(158, 180, 240)); }
		 */
		return targetColor;
	}

	private static Color getListBackgroundColor() {
		Color targetColor = new Color(new RGB(240, 240, 240));
		/*
		 * if (IS_DARK_THEMED) { targetColor = new Color(new RGB(23, 23, 23)); } else {
		 * targetColor = new Color(new RGB(240, 240, 240)); }
		 */
		return targetColor;
	}

	private static Color getForegroundColor() {
		Color targetColor = new Color(new RGB(0, 0, 0));
		/*
		 * if (IS_DARK_THEMED) { targetColor = new Color(new RGB(255, 255, 255)); } else
		 * { targetColor = new Color(new RGB(0, 0, 0)); }
		 */
		return targetColor;
	}

}
