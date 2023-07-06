#!/bin/bash
# activate bash checks for unset vars, pipe fails
set -eauo pipefail
script=$(readlink -f "$0")
script_dir=$(dirname "$script")

cd ${github_workspace}
pwd

# extract versions from release files
releaseRepo=${github_workspace}/cnf/release/

eimVersion=$(basename -s .jar $(find ${releaseRepo}/eim.impl/ -name 'eim.impl-*') | cut -d'-' -f 2)
apiVersion=$(basename -s .jar $(find ${releaseRepo}/eim.api/ -name 'eim.api-*') | cut -d'-' -f 2)
trayVersion=$(basename -s .jar $(find ${releaseRepo}/eim.tray/ -name 'eim.tray-*') | cut -d'-' -f 2)

tag=v${trayVersion}

# do not try to release if tag already exists and skip gravefully
if [ git show-ref --tags --verify --quiet "refs/tags/${tag}" ]; then
    exit 0
fi

branch=$(echo ${GITHUB_REF#refs/heads/})

# rename jars to include the version
mv ${github_workspace}/eim/generated/gradle/distributions/executable/eim.jar ${github_workspace}/eim/generated/gradle/distributions/executable/eim_${eimVersion}.jar
mv ${github_workspace}/eim.api/generated/gradle/eim.api.jar ${github_workspace}/eim.api/generated/gradle/eim.api_${apiVersion}.jar

# rename all platform versions of the tray application
mv ${github_workspace}/eim.tray/generated/gradle/distributions/executable/eim.tray.win32.x86_64.jar ${github_workspace}/eim.tray/generated/gradle/distributions/executable/eim.tray.win32.x86_64_${trayVersion}.jar
mv ${github_workspace}/eim.tray/generated/gradle/distributions/executable/eim.tray.linux.x86_64.jar ${github_workspace}/eim.tray/generated/gradle/distributions/executable/eim.tray.linux.x86_64_${trayVersion}.jar
mv ${github_workspace}/eim.tray/generated/gradle/distributions/executable/eim.tray.cocoa.aarch64.jar ${github_workspace}/eim.tray/generated/gradle/distributions/executable/eim.tray.cocoa.aarch64_${trayVersion}.jar

if [[ ${branch} = "main" ]]; then
    gh release create ${tag}\
        --latest \
        --title "Eclipse Installation Manager (EIM) ${tag}" \
        --generate-notes \
        ${github_workspace}/eim/generated/gradle/distributions/executable/eim_${eimVersion}.jar \
        ${github_workspace}/eim.api/generated/gradle/eim.api_${apiVersion}.jar \
        ${github_workspace}/eim.tray/generated/gradle/distributions/executable/eim.tray.win32.x86_64_${trayVersion}.jar \
        ${github_workspace}/eim.tray/generated/gradle/distributions/executable/eim.tray.linux.x86_64_${trayVersion}.jar \
        ${github_workspace}/eim.tray/generated/gradle/distributions/executable/eim.tray.cocoa.aarch64_${trayVersion}.jar
else
    echo "# Skipping release because the branch is not main"
fi