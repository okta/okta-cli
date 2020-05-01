#!/bin/bash
#
# Copyright 2017-Present Okta, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# ignore unused var in this script as this script defines common bits for our other scripts
# shellcheck disable=SC2034

# deploy snapshot from ONLY this branch
SNAPSHOT_BRANCH="master"
PROJECT_NAME="okta-maven-plugin"

# Get the slug from the env var, or parse the 'origin' remote
REPO_SLUG=${REPO_SLUG:-$(git remote get-url origin | sed 's_.*\:__; s_.*github.com/__; s_\.git__')}
PULL_REQUEST=${PULL_REQUEST:-true} # default to true
BRANCH=${BRANCH:-"$(git rev-parse --abbrev-ref HEAD)"}
CRON=${CRON:-false}
RUN_ITS=${RUN_ITS:-false} # run the ITs if we have an ENV_VARS are set

# we only deploy from a given branch NOT for pull requests, and ONLY when we can run the ITs
# and do NOT deploy releases, only snapshots right now
if [ "$BRANCH" = "$SNAPSHOT_BRANCH" ] && [ "$PULL_REQUEST" = false ] && [ "$RUN_ITS" = true ] && [ ! "$IS_RELEASE" = true ]; then
    DEPLOY=true
fi
DEPLOY=${DEPLOY:-false}

# print the props so it is easier to debug on CI or locally.
echo "REPO_SLUG: ${REPO_SLUG}"
echo "PULL_REQUEST: ${PULL_REQUEST}"
echo "BRANCH: ${BRANCH}"
echo "IS_RELEASE: ${IS_RELEASE}"
echo "RUN_ITS: ${RUN_ITS}"
echo "CRON: ${CRON}"

# all the prep is done, lets run the build!
MVN_CMD="./mvnw -s src/ci/settings.xml -B -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn -V"
