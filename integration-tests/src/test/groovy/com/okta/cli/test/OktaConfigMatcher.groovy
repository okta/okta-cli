/*
 * Copyright 2020-Present Okta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.okta.cli.test

import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.yaml.snakeyaml.Yaml

class OktaConfigMatcher extends TypeSafeMatcher<File> {

    private final Map expected

    OktaConfigMatcher(String orgUrl, String token) {
        this([
            okta: [
                "client": [
                    orgUrl: orgUrl,
                    token: token ]]])
    }

    OktaConfigMatcher(Map expected) {
        this.expected = expected
    }

    @Override
    protected boolean matchesSafely(File item) {

        def matches = item != null && item.exists()
        return matches && expected == parseYaml(item)
    }

    @Override
    void describeTo(Description description) {
        description.appendText("To contain YAML equal to: " + expected)
    }

    @Override
    protected void describeMismatchSafely(File item, Description mismatchDescription) {
        mismatchDescription.appendText("File contents: " + item?.text)
    }

    private static Map parseYaml(File oktaConfigFile) {
        Yaml yaml = new Yaml()
        return yaml.load(oktaConfigFile.text)
    }
}
