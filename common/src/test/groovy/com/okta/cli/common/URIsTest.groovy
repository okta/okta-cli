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
package com.okta.cli.common


import org.testng.annotations.Test

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.MatcherAssert.assertThat

class URIsTest {

    @Test
    void reverseDomain() {
        assertThat URIs.reverseDomain("http://example.com"), equalTo("com.example")
        assertThat URIs.reverseDomain("https://foo.example.com"), equalTo("com.example.foo")
        assertThat URIs.reverseDomain("foo.example.com"), equalTo("com.example.foo")
        assertThat URIs.reverseDomain("ionic://foo.example.com"), equalTo("com.example.foo")
    }

    @Test
    void baseUrl() {
        assertThat URIs.baseUrlOf("http://example.com"), equalTo("http://example.com/")
        assertThat URIs.baseUrlOf("http://example.com/foo/bar"), equalTo("http://example.com/")
        assertThat URIs.baseUrlOf("ionic://foo.example.com"), equalTo("ionic://foo.example.com/")
        assertThat URIs.baseUrlOf("com.example.foo:/callback"), equalTo("com.example.foo:/")
    }
}
