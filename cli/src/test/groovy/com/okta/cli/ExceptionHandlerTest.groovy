/*
 * Copyright 2022-Present Okta, Inc.
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
package com.okta.cli

import com.okta.sdk.impl.error.DefaultError
import com.okta.sdk.resource.ResourceException
import org.testng.annotations.Test

import java.nio.charset.StandardCharsets

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.containsString

class ExceptionHandlerTest {

    @Test
    void testResourceException() {
        String errorOutput = handleException(new ResourceException(new DefaultError()
                .setStatus(401)
                .setCode("E0000015")))
        assertThat(errorOutput, allOf(
                containsString("Your Okta Org is missing a feature required to use the Okta CLI: API Access Management"),
                containsString("You can create a free Okta developer account that has this feature at: https://developer.okta.com/signup/"),
                containsString("An error occurred if you need more detail use the '--verbose' option")
        ))
    }

    @Test
    void testException() {
        String errorOutput = handleException(new Exception())
        assertThat(errorOutput, containsString("An error occurred if you need more detail use the '--verbose' option"))
    }

    private String handleException(Exception exception) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        OktaCli oktaCli = new OktaCli()
        oktaCli.standardOptions = new OktaCli.StandardOptions()

        try (PrintStream printStream = new PrintStream(baos, true, StandardCharsets.UTF_8)) {
            OktaCli.ExceptionHandler exceptionHandler = new OktaCli.ExceptionHandler(oktaCli, printStream)
            exceptionHandler.handleExecutionException(exception, null, null)
            return baos.toString()
        }
    }
}
