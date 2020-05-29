/*
 * Copyright 2019-Present Okta, Inc.
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
package com.okta.maven.orgcreation

import org.apache.maven.plugin.MojoFailureException
import org.testng.annotations.Test

import static com.okta.maven.orgcreation.TestUtil.expectException
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.containsString

class SetupMojoTest {

    @Test
    void throwsException() {
        def exception = expectException(MojoFailureException) { new SetupMojo().execute() }
        assertThat(exception.getMessage(), allOf(
                                                containsString("This mojo has been removed"),
                                                containsString("okta:register"),
                                                containsString("okta:spring-boot"),
                                                containsString("okta:web-app"),
                                                containsString("okta:jhipster")))
    }
}
