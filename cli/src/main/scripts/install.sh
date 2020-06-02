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

set -e

LINUX_DIST="https://raw.githubusercontent.com/oktadeveloper/okta-maven-plugin/cli-test-dist/okta-cli-ubuntu-latest-x86_64.zip"
DARWIN_DIST="https://raw.githubusercontent.com/oktadeveloper/okta-maven-plugin/cli-test-dist/okta-cli-macos-latest-x86_64.zip"

function echoerr { echo "$@" 1>&2; }

function download {

  if [[ ! ":$PATH:" == *":/usr/local/bin:"* ]]; then
    echoerr "Your path is missing /usr/local/bin, you need to add this to use this installer."
    exit 1
  fi

  if [ "$(uname)" == "Darwin" ]; then
    OS=darwin
    URL=$DARWIN_DIST
  elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
    OS=linux
    URL=$LINUX_DIST
  else
    echoerr "This installer is only supported on Linux and MacOS"
    exit 1
  fi

  ARCH="$(uname -m)"
  if [ "$ARCH" == "x86_64" ]; then
    ARCH=x64
  else
    echoerr "unsupported arch: $ARCH"
    exit 1
  fi

  INSTALL_DIR=$(mktemp -d "${TMPDIR:-/tmp}/okta.XXXXXXXXX")
  if [ $(command -v curl) ]; then
    curl "$URL" | funzip > $INSTALL_DIR/okta
  else
    wget -O- "$URL" | funzip > $INSTALL_DIR/okta
  fi

  echo $INSTALL_DIR/okta
}

function install {

  DOWNLOAD_LOCATION=$1

  { # try
    mkdir -p /usr/local/lib/okta/bin &&
    mv -f $DOWNLOAD_LOCATION /usr/local/lib/okta/bin/ &&
    chmod 755 /usr/local/lib/okta/bin/okta
  } || { # catch
     echoerr
     echoerr "Failed install the okta cli, run the following commands manually:"
     echoerr "  sudo mkdir -p /usr/local/lib/okta/bin"
     echoerr "  sudo mv $DOWNLOAD_LOCATION /usr/local/lib/okta/bin/"
     echoerr "  sudo chmod 755 /usr/local/lib/okta/bin/okta"
     echoerr "  sudo ln -sf /usr/local/lib/okta/bin/okta /usr/local/bin/okta"
     exit 1
  }

  { # try
    # delete old okta bin if exists
    # rm -f $(command -v okta) || true
    ln -sf /usr/local/lib/okta/bin/okta /usr/local/bin/okta
  } || { # catch
    echoerr
    echoerr "Failed link the okta cli, run the following command manually:"
    echoerr "  sudo ln -sf /usr/local/lib/okta/bin/okta /usr/local/bin/okta"
    exit 1
  }
}

DOWNLOAD_LOCATION=$(download)
install $DOWNLOAD_LOCATION

# test the CLI
LOCATION=$(command -v okta)
echo "Okta CLI installed to $LOCATION"
okta --version
