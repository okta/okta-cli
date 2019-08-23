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
package com.okta.maven.orgcreation.service

import com.okta.maven.orgcreation.TestUtil
import com.okta.maven.orgcreation.model.OrganizationResponse
import com.okta.sdk.impl.client.DefaultClientBuilder
import com.okta.sdk.impl.config.ClientConfiguration
import org.testng.annotations.Test


import static org.hamcrest.Matchers.is
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.io.FileMatchers.anExistingFile
import static org.mockito.Mockito.mock


class DefaultSdkConfigurationServiceTest {

    @Test
    void loadConfig() {

        ClientConfiguration clientConfig = mock(ClientConfiguration)
        DefaultSdkConfigurationService configurationService = configurationService(clientConfig)

        assertThat configurationService.loadUnvalidatedConfiguration(), is(clientConfig)
    }

    @Test
    void writeSdkConfig() {

        ClientConfiguration clientConfig = mock(ClientConfiguration)
        DefaultSdkConfigurationService configurationService = configurationService(clientConfig)

        File configFile = new File(File.createTempDir("writeSdkConfig-", "-test"), "test.yaml")
        configurationService.writeOktaYaml("https://okta.example.com", "an-api-token", configFile)

        assertThat configFile, anExistingFile()
        assertThat TestUtil.readYamlFromFile(configFile), is([
                okta: [
                    client: [
                        orgUrl: "https://okta.example.com",
                        token: "an-api-token"]]])
    }


    private static DefaultSdkConfigurationService configurationService(ClientConfiguration clientConfig) {
        return new DefaultSdkConfigurationService() {
            @Override
            DefaultClientBuilder clientBuilder() {

                DefaultClientBuilder clientBuilder = new DefaultClientBuilder()
                clientBuilder.clientConfig = clientConfig
                return clientBuilder
            }
        }
    }

}
