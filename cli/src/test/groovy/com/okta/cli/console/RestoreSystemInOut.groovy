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
package com.okta.cli.console

import org.testng.IInvokedMethod
import org.testng.IInvokedMethodListener
import org.testng.ITestResult

class RestoreSystemInOut implements IInvokedMethodListener {

    private final InputStream originalIn = System.in
    private final OutputStream originalOut = System.out
    private final OutputStream originalErr = System.err

    @Override
    void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        System.in = originalIn
        System.out = originalOut
        System.err = originalErr
    }
}
