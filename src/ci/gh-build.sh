#!/bin/bash
#
# Copyright 2020-Present Okta, Inc.
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

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

export BRANCH="${BRANCH:-$(echo $GITHUB_REF | awk 'BEGIN { FS = "/" } ; { print $3 }')}"
export PULL_REQUEST=$(if [ -z "${GITHUB_EVENT_NUMBER}" ]; then echo "false"; else echo "true"; fi)
export CRON=$(if [ "schedule" == "${GITHUB_EVENT_NAME}" ]; then echo "true"; fi)
export RUN_ITS=true

source "${SCRIPT_DIR}/before_install.sh"
"${SCRIPT_DIR}/build.sh"
