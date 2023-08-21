---
layout: default
title: "Eclipse Installation Manager"
permalink: /
---
<p align="center">
    <img src="assets/images/simpleScreenshot.png"
        alt="Screenshot of the Application"
        style="float: center; margin-right: 10px;" />
</p>
<p align="center">
    <img src="assets/images/managementView.png"
        alt="Screenshot of the Management View"
        style="float: center; margin-right: 10px;" />
</p>

## Prerequisites
To be able to manage any Eclipse Installation, they need to be done with the official Eclipse Installer. EIM currently relies heavily on metadata provided by the Eclipse Installer and functionality to add installations manually is planned, but not yet ready.

Please visit the project on Github for any ideas, issues or discussions surrounding the Eclipse Installation Manager.

## The tray application
Start the tray application by downloading the version which matches your platform and execute
```
java -jar eim.tray.<platform>.jar
```

Please note, that if you are on MacOS, you need to execute 

```
java -XstartOnFirstThread -jar eim.tray.cocoa.<arch>_<version>.jar
```

## How to run console application

Download the latest `eim.jar` from the releases page.

Execute the JAR with

```
java -jar eim.jar
```

## Current Features

- List all Eclipse installations and workspaces
- Start Eclipse
- Open workspace in Eclipse

Please check out the [Github Discussion](https://github.com/A7exSchin/EclipseInstallationManager/discussions/29) for more information about the current state and future plans.

## Additional packages
Alongside the tray application there are additional packages available:
1. The Eclipse Installation Manager API, which can be implemented yourself.
2. The reference implementation for the Eclipse Installation Manager Service which includes a command line interface. 

