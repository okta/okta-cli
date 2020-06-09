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
package com.okta.cli.common

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig

trait WireMockSupport {

    private WireMockServer wireMockServer

    @BeforeClass
    void startMockServer() throws IOException {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort())
        configureWireMock()
        wireMockServer.start()
    }

    String mockUrl() {
        return wireMockServer.url("/")
    }

    WireMockServer getWireMockServer() {
        return wireMockServer
    }

    void configureWireMock() throws IOException {
        wireMockStubMapping().forEach {
            wireMockServer.stubFor(it)
        }
    }

    abstract Collection<StubMapping> wireMockStubMapping()

    @AfterClass
    void stopMockServer() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop()
        }
    }
}
