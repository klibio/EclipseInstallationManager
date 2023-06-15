#!/bin/bash
# activate bash checks for unset vars, pipe fails
set -eauo pipefail
SCRIPT=$(readlink -f "$0")
SCRIPT_DIR=$(dirname "$SCRIPT")


version=$(cat version)
apiVersion=$(cat apiVersion)
tag=v${version}
branch=$(echo ${GITHUB_REF#refs/heads/})

# rename jars to include the version
mv ${GITHUB_WORKSPACE}/eim/generated/distributions/executable/eim.jar ${GITHUB_WORKSPACE}/eim/generated/distributions/executable/eim_${version}.jar
mv ${GITHUB_WORKSPACE}/eim.api/generated/eim.api.jar ${GITHUB_WORKSPACE}/eim.api/generated/eim.api_${apiVersion}.jar

if [[ ${branch} = "main" ]]; then
    gh release create ${tag}\
        --latest \
        --title "Eclipse Installation Manager (EIM) v${version}" \
        --generate-notes \
        ${GITHUB_WORKSPACE}/eim/generated/distributions/executable/eim_${version}.jar \
        ${GITHUB_WORKSPACE}/eim.api/generated/eim.api_${apiVersion}.jar
else
    echo "# Skipping release because the branch is not main"
fi