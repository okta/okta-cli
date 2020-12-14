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
import static org.hamcrest.Matchers.is

class OidcPropertiesTest {

    @Test
    void propertyNameTest() {
        def oidcProperties1 = OidcProperties.oktaEnv()
        assertThat oidcProperties1.issuerUriPropertyName, is("okta.oauth2.issuer")
        assertThat oidcProperties1.clientIdPropertyName, is("okta.oauth2.client-id")
        assertThat oidcProperties1.clientSecretPropertyName , is("okta.oauth2.client-secret")

        def oidcProperties2 = OidcProperties.spring("okta")
        assertThat oidcProperties2.issuerUriPropertyName, is("spring.security.oauth2.client.provider.okta.issuer-uri")
        assertThat oidcProperties2.clientIdPropertyName, is("spring.security.oauth2.client.registration.okta.client-id")
        assertThat oidcProperties2.clientSecretPropertyName, is("spring.security.oauth2.client.registration.okta.client-secret")

        def oidcProperties3 = OidcProperties.spring("oidc")
        assertThat oidcProperties3.issuerUriPropertyName, is("spring.security.oauth2.client.provider.oidc.issuer-uri")
        assertThat oidcProperties3.clientIdPropertyName, is("spring.security.oauth2.client.registration.oidc.client-id")
        assertThat oidcProperties3.clientSecretPropertyName, is("spring.security.oauth2.client.registration.oidc.client-secret")
    }
}
