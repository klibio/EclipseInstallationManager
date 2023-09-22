
# <img src="./assets/EIM-Color_512x.png" width="10%"> Eclipse Installation Manager
This application enables a developer to manage all Eclipse Product installations done via the official Eclipse Installer in an easy and convenient way.

## Table of contents
- **[Eclipse Installation Manager Homepage](https://eim.a7exschin.dev)**
- [Table of contents](#table-of-contents)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Usage and capabilities](#usage-and-capabilities)
- [Screenshots](#screenshots)

## Prerequisites

Eclipse Application installations done with the Eclipse Installer. Alternatively you can add them manually by following the [instructions](https://github.com/A7exSchin/bootPalladio) in the readme of the bootPalladio project.

## Installation

Currently the release is done as a executable java archive. This will change later.
Visit the [Releases Page](https://github.com/A7exSchin/EclipseInstallationManager/releases) and download the latest `eim.tray.<platform>.jar` for your platform.

Start the tray application by executing
```
java -jar eim.tray.<platform>.jar
```

For macOS the command differs slightly:

```
java -XstartOnFirstThread -jar eim.tray.cocoa.<arch>_<version>.jar
```

## Usage and Capabilities

Feature list:
- Easy quick access to all local installations. Simply click on the tray icon and select the installation you want to start, with the assigned workspace.
- List all installations and workspaces and search for them by opening the Management view.
- Change how installations are named within the Eclipse Installation Manager and delete them if they are not needed anymore. Just click the pencil or trash can icon in the Management view.
- Double click to open the Eclipse Installer. If it is not set, the Eclipse Installation Manager will ask you to select the path to the Eclipse Installer.
- Right click the tray icon to open the context menu and configure or quit the application

Please check out the [Github Discussion](https://github.com/A7exSchin/EclipseInstallationManager/discussions/29) for more information about the current state and future plans.

## Screenshots

![Tray Application](doc/assets/images/simpleScreenshot.png)
![Overview](doc/assets/images/managementView.png)

![Confirm Delete](doc/assets/images/confirmDelete.png)

![Modify Entry](doc/assets/images/modifyEntry.png)![Confirm Delete Parent Folder](doc/assets/images/confirmDeleteParent.png)



## Additional packages
Alongside the tray application there are additional packages available:
1. The Eclipse Installation Manager API, which can be implemented yourself.
2. The reference implementation for the Eclipse Installation Manager Service which includes a command line interface. 
