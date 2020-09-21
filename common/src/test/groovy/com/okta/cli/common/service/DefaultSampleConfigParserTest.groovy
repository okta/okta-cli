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
package com.okta.cli.common.service

import com.okta.cli.common.model.OktaSampleConfig
import org.hamcrest.MatcherAssert
import org.testng.annotations.Test

import static org.hamcrest.Matchers.equalTo

class DefaultSampleConfigParserTest {

    @Test
    void basicFilterTest() {

        File file = File.createTempFile("basicFilterTest", "-sample.yaml")
        file << "oauthClient:\n"
        file << "  redirectUris:\n"
        file << '    - ${CLI_OKTA_REVERSE_DOMAIN}.myApp://endpoint\n'
        file << '    - http://example.com/foo\n'

        OktaSampleConfig config = new DefaultSampleConfigParser().parseConfig(file, [CLI_OKTA_REVERSE_DOMAIN: "com.example.id"])
        MatcherAssert.assertThat config.getOAuthClient().redirectUris, equalTo(["com.example.id.myApp://endpoint", "http://example.com/foo"])
    }
}
