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
package com.okta.cli.common.model


import org.testng.annotations.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.mockito.Mockito.spy
import static org.mockito.Mockito.verify

class FilterConfigBuilderTest {

    @Test
    void issuerTest() {
        // setting the issuer should cascade and call setOrgUrl, and setReverseDomain
        FilterConfigBuilder configBuilder = spy(new FilterConfigBuilder())
        configBuilder.setIssuer("https://foobar.example.com/my/issuer")
        def result = configBuilder.build()

        assertThat result, equalTo([CLI_OKTA_ORG_URL: "https://foobar.example.com/",
                                    CLI_OKTA_ISSUER: "https://foobar.example.com/my/issuer",
                                    CLI_OKTA_REVERSE_DOMAIN: "com.example.foobar"])

        verify(configBuilder).setOrgUrl("https://foobar.example.com/")
        verify(configBuilder).setReverseDomain("com.example.foobar")
    }
}