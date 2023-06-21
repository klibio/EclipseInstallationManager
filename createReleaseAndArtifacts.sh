#!/bin/bash
# activate bash checks for unset vars, pipe fails
set -eauo pipefail
script=$(readlink -f "$0")
script_dir=$(dirname "$script")

cd ${github_workspace}
pwd

version=$(cat version)
apiVersion=$(cat apiVersion)
trayVersion="0.1.0"
tag=v${version}
branch=$(echo ${GITHUB_REF#refs/heads/})

# rename jars to include the version
mv ${github_workspace}/eim/generated/gradle/distributions/executable/eim.jar ${github_workspace}/eim/generated/gradle/distributions/executable/eim_${version}.jar
mv ${github_workspace}/eim.api/generated/gradle/eim.api.jar ${github_workspace}/eim.api/generated/gradle/eim.api_${apiVersion}.jar

# rename all platform versions of the tray application
mv ${github_workspace}/eim.tray/generated/gradle/distributions/executable/eim.tray.win32.x86_64.jar ${github_workspace}/eim.tray/generated/gradle/distributions/executable/eim.tray.win32.x86_64_${trayVersion}.jar
mv ${github_workspace}/eim.tray/generated/gradle/distributions/executable/eim.tray.linux.x86_64.jar ${github_workspace}/eim.tray/generated/gradle/distributions/executable/eim.tray.linux.x86_64_${trayVersion}.jar
mv ${github_workspace}/eim.tray/generated/gradle/distributions/executable/eim.tray.cocoa.aarch64.jar ${github_workspace}/eim.tray/generated/gradle/distributions/executable/eim.tray.cocoa.aarch64_${trayVersion}.jar

if [[ ${branch} = "main" ]]; then
    gh release create ${tag}\
        --latest \
        --title "Eclipse Installation Manager (EIM) v${version}" \
        --generate-notes \
        ${github_workspace}/eim/generated/gradle/distributions/executable/eim_${version}.jar \
        ${github_workspace}/eim.api/generated/gradle/eim.api_${apiVersion}.jar \
        ${github_workspace}/eim.tray/generated/gradle/distributions/executable/eim.tray.win32.x86_64_${trayVersion}.jar \
        ${github_workspace}/eim.tray/generated/gradle/distributions/executable/eim.tray.linux.x86_64_${trayVersion}.jar \
        ${github_workspace}/eim.tray/generated/gradle/distributions/executable/eim.tray.cocoa.aarch64_${trayVersion}.jar
else
    echo "# Skipping release because the branch is not main"
fi