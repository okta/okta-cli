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

{
    set -e
    SUDO=''
    if [ "$(id -u)" != "0" ]; then
      SUDO='sudo'
      echo "This script requires superuser access."
      echo "You will be prompted for your password by sudo."
      # clear any previous sudo permission
      sudo -k
    fi


    # run inside sudo
    $SUDO bash <<SCRIPT
  set -e

  LINUX_DIST="https://raw.githubusercontent.com/oktadeveloper/okta-maven-plugin/cli-test-dist/okta-cli-ubuntu-latest-x86_64.zip"
  DARWIN_DIST="https://raw.githubusercontent.com/oktadeveloper/okta-maven-plugin/cli-test-dist/okta-cli-macos-latest-x86_64.zip"

  echoerr() { echo "\$@" 1>&2; }

  if [[ ! ":\$PATH:" == *":/usr/local/bin:"* ]]; then
    echoerr "Your path is missing /usr/local/bin, you need to add this to use this installer."
    exit 1
  fi

  if [ "\$(uname)" == "Darwin" ]; then
    OS=darwin
    URL=\$DARWIN_DIST
  elif [ "\$(expr substr \$(uname -s) 1 5)" == "Linux" ]; then
    OS=linux
    URL=\$LINUX_DIST
  else
    echoerr "This installer is only supported on Linux and MacOS"
    exit 1
  fi

  ARCH="\$(uname -m)"
  if [ "\$ARCH" == "x86_64" ]; then
    ARCH=x64
  else
    echoerr "unsupported arch: \$ARCH"
    exit 1
  fi

  mkdir -p /usr/local/lib/okta/bin
  cd /usr/local/lib/okta/bin
  rm -rf okta

  echo "Installing CLI from \$URL"
  if [ \$(command -v curl) ]; then
    curl "\$URL" | funzip > okta
  else
    wget -O- "\$URL" | funzip > okta
  fi

  chmod 755 okta

  # delete old heroku bin if exists
  rm -f \$(command -v okta) || true
  rm -f /usr/local/bin/okta
  ln -s /usr/local/lib/okta/bin/okta /usr/local/bin/okta

SCRIPT
  # test the CLI
  LOCATION=$(command -v okta)
  echo "okta installed to $LOCATION"
  okta --version
}