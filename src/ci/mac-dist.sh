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

VERSION=${1:-$VERSION}
CODE_SIGN_EMAIL=""
CODE_SIGN_ID=""

while [ -z "$VERSION" ]; do
  read -p "Version: " VERSION
done

while [ -z "$CODE_SIGN_EMAIL" ]; do
  read -p "Email: " CODE_SIGN_EMAIL
done

while [ -z "$CODE_SIGN_ID" ]; do
  echo "Available Code Sign IDs (use the part in quotes): "
  security find-identity -v -p codesigning
  read -p "Code Sign ID: " CODE_SIGN_ID
done

echo "VERSION: $VERSION"
echo "CODE_SIGN_EMAIL: $CODE_SIGN_EMAIL"
echo "CODE_SIGN_ID: $CODE_SIGN_ID"

DIST_ZIP_NAME="okta-cli-macos-${VERSION}-x86_64.zip"

# Grab the mac dist 'okta' binary, unzip it, or if you built on a mac:
pushd target/checkout/cli/target

# back up okta bin (just in case)
#cp okta okta.bak

# sign the file
#codesign -s "$CODE_SIGN_ID" --timestamp --options runtime okta
# timestamp is needed for notarize
# options runtime enabled "hardend runtime" needed for notarize

# notary request must requires a container (zip, dmg, pkg, etc)
#ditto -c -k okta "okta-cli-macos-${VERSION}-x86_64.zip"
# TODO: potential ARCH issue here

# notarize
echo "This could take a few minutes"
xcrun altool --notarize-app --primary-bundle-id "com.okta.oktadeveloper.cli" -u "${CODE_SIGN_EMAIL}" --file "${DIST_ZIP_NAME}"

# This will output something like:
## No errors uploading 'okta-cli-macos-0.4.0-x86_64.zip'.
## RequestUUID = <SOME-ID>

CHECKSUM=$(shasum -a256 "${DIST_ZIP_NAME}")

popd

echo "You should get an email in a few minutes confirming code signing was successful"
echo "Manually check the status using: xcrun altool -u \"${EMAIL}\" --notarization-info \"<RequestUUID>\""

echo ""
echo "Once signing is complete upload release to GitHub with the following Markdown:"
echo ""
echo "Checksums (SHA 256):"
echo ""
echo "[okta-cli-mac-${VERSION}-x86_64.zip](https://github.com/oktadeveloper/okta-cli/releases/download/okta-cli-tools-${VERSION}/okta-cli-macos-${VERSION}-x86_64.zip) - ${CHECKSUM}"
echo ""
echo "Location of file: $(pwd)/target/checkout/cli/target/${DIST_ZIP_NAME}"
echo ""
echo ""
echo "Update Homebrew Tap:"
echo "To to: https://github.com/oktadeveloper/homebrew-tap/edit/master/Casks/okta.rb"
echo ""
echo "Update with the following:"
echo "  version '${VERSION}'"
echo "  sha256 '${CHECKSUM}'"
echo ""
echo "commit, and then test with: brew cask reinstall okta"
