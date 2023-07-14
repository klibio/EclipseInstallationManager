<img src="./assets/EIM-Color_512x.png" width="25%">

# Eclipse Installation Manager
This repository contains an example workspace including minimal repositories for development of eclipse products.

## How to run tray app

visit the [Releases Page](https://github.com/A7exSchin/EclipseInstallationManager/releases) and download the latest `eim.tray.<platform>.jar` for your platform.

Every release also includes a small application, which uses the Eclipse Installation Manager in the form of a Tray Application. This shall be seen as a small test consumer for the tool.
The Tray App will be released with every release of the Eclipse Installation Manager. The version of the manager defines the latest tag.

Start the tray application by executing
```
java -jar eim.tray.<platform>.jar
```

```
java -XstartOnFirstThread -jar eim.tray.cocoa.<arch>_<version>.jar
```

## Using the console application

Download and execute the `eim.jar` with

```
java -jar eim.jar
```

Enter `help` to see the available commands.

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
