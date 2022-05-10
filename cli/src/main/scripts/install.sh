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
VERSION="0.10.0"
LINUX_DIST="https://github.com/okta/okta-cli/releases/download/okta-cli-tools-${VERSION}/okta-cli-linux-${VERSION}-x86_64.zip"
DARWIN_DIST="https://github.com/okta/okta-cli/releases/download/okta-cli-tools-${VERSION}/okta-cli-macos-${VERSION}-x86_64.zip"

function echoerr { echo "$@" 1>&2; }

function download {

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
    curl -L "$URL" | funzip > $INSTALL_DIR/okta
  else
    wget -O- "$URL" | funzip > $INSTALL_DIR/okta
  fi

  chmod 755 $INSTALL_DIR/okta
  echo $INSTALL_DIR/okta
}

function install {

  DOWNLOAD_LOCATION=$1

  { # try
    mkdir -p $HOME/bin &&
    mv -f $DOWNLOAD_LOCATION $HOME/bin

  } || { # catch
     echoerr
     echoerr "Failed to install the Okta CLI. Please run the following command manually:"
     echoerr "  mv -f $DOWNLOAD_LOCATION $HOME/bin"
     exit 1
  }

  # check if okta is on the path
  LOCATION=$(command -v okta)

  PATH_UPDATED=false
  if [[ ! "$LOCATION" == *"$HOME/bin/okta:"* ]]; then
    [ -f "$HOME/.bash_profile" ] && updateBashProfilePath && PATH_UPDATED=true
    [ -f "$HOME/.bashrc" ] && updateBashRcPath && PATH_UPDATED=true
    [ -f "$HOME/.zshrc" ] && updateZshPath && PATH_UPDATED=true

    if [[ ! "$PATH_UPDATED" == "true" ]]; then
      echoerr 'Failed to add $HOME/bin/okta to your path.'
      exit 1
    fi
  fi
}

function updateBashPath {
  # shellcheck disable=SC2016
  (( $# == 1 )) || { printf 'Usage: updateBashPath $bashPath\n' >&2; return 1; }

  local bashPath=$1
  { # try
    grep -q 'export PATH=$HOME/bin:$PATH' "$bashPath" || echo -e '\nexport PATH=$HOME/bin:$PATH' >> "$bashPath"
  } || { # catch
    echoerr
    echoerr "Failed to add $HOME/bin to PATH. Please update your $bashPath by running the following command:"
    echoerr "  export PATH=\$HOME/bin:\$PATH >> $bashPath"
  }
}

function updateBashProfilePath {
  updateBashPath "$HOME/.bash_profile"
}

function updateBashRcPath {
  updateBashPath "$HOME/.bashrc"
}

function updateZshPath {
  { # try
    grep -q 'export PATH=$HOME/bin:$PATH' ~/.zshrc || echo -e '\nexport PATH=$HOME/bin:$PATH' >> ~/.zshrc
  } || { # catch
    echoerr
    echoerr 'Failed to add $HOME/bin to PATH. Please update your ~/.zshrc by running the following command:'
    echoerr '  export PATH=$HOME/bin:$PATH >> ~/.zshrc'
  }
}

DOWNLOAD_LOCATION=$(download)
install $DOWNLOAD_LOCATION
export PATH=$HOME/bin:$PATH

# test the CLI
LOCATION=$(command -v okta)
echo "Okta CLI installed to $LOCATION, open a new terminal to use it!"
okta --version
