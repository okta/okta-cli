#!/usr/bin/env bats

run_installer() {
  bash ./src/main/scripts/install.sh
}

@test "basic install test" {
  TEMP_HOME=$(mktemp -d "${TMPDIR:-/tmp}/cli-test.XXXXXXXXX")
  HOME="${TEMP_HOME}"
  touch "${TEMP_HOME}/.bashrc"

  run run_installer

  # assert the installer exited cleanly
  if [ "$status" -ne 0 ]; then
    echo "$output"
    echo "ERROR: status = $status"
    return 1
  fi

  # assert ${TEMP_HOME}/bin/okta --version outputs x.x.x-hash
  run ${TEMP_HOME}/bin/okta --version
  if [ "$status" -ne 0 ]; then
    echo "${output}"
    echo "ERROR: status = $status"
    return 1
  fi

  if ! [[ "${output}" =~ ^[0-9]+\.[0-9]+\.[0-9]+-.+$ ]];
  then
    echo "Expected version to match x.x.x-hash"
    echo "Was: ${output}"
    return 1
  fi

  # assert .bashrc updated
  run grep 'export PATH=$HOME/bin:$PATH' "${TEMP_HOME}/.bashrc"
  if [ "$status" -ne 0 ]; then
    echo ".bashrc not updated, contents as followed:"
    cat "${TEMP_HOME}/.bashrc"
    return 1
  fi
}