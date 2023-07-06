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

## Prerequisites
To be able to manage any Eclipse Installations, they need to be done with the official Eclipse Installer, for now. The application relies heavily on metadata provided by the Eclipse Installer and functionality to add installations manually is planned, but not yet ready.

Please visit the project on Github for any ideas, issues or discussions surrounding the Eclipse Installation Manager.

## The tray application
Every release also includes a small application, which uses the Eclipse Installation Manager in the form of a Tray Application. This shall be seen as a small test consumer for the tool.
The Tray App will be released with every release of the Eclipse Installation Manager. The version of the manager defines the latest tag.

Start the tray application by downloading the version which matches your platform and execute
```
java -jar eim.tray.<platform>.jar
```

## How to run console application

Download the latest `eim.jar` from the releases page.

Execute the JAR with

```
java -jar eim.jar
```

## Current capabilitites

The EIM is currently accessed by the GoGo Shell via a command. Type `help` to see that the command `startProcess <Command> <Working Directory> <Arguments[]>` is available.
Currently any process can be started with this command, given any working directory and set of arguments.
For example:

```
startProcess "C:\eclipse\eclipse.exe" "C:\eclipse\" "ws=D:\workspaces\projectA"
```

This command will start an Eclipse with the argument `ws`, which results in the Eclipse IDE instance to be started with the given workspace.

The second available command `listLocations` lists available eclipse installations and the mapped workspaces and assignes a number for each installation and workspace combination to be able to launch it. 

With the numbers returned by this command you can execute
```
startEntry <number>
```
to start the installation/workspace entry.

