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

import com.okta.cli.common.FactorVerificationException
import com.okta.cli.common.config.MutablePropertySource
import com.okta.cli.common.model.ErrorResponse
import com.okta.cli.common.model.OrganizationRequest
import com.okta.cli.common.model.OrganizationResponse
import com.okta.cli.common.model.RegistrationQuestions
import com.okta.sdk.client.Client
import com.okta.sdk.client.ClientBuilder
import com.okta.sdk.client.Clients
import com.okta.sdk.impl.config.ClientConfiguration
import com.okta.sdk.resource.ExtensibleResource
import com.okta.sdk.resource.application.OpenIdConnectApplicationType
import com.okta.sdk.resource.role.Scope
import com.okta.sdk.resource.role.ScopeType
import com.okta.sdk.resource.trusted.origin.TrustedOrigin
import com.okta.sdk.resource.trusted.origin.TrustedOriginList
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.testng.PowerMockObjectFactory
import org.testng.IObjectFactory
import org.testng.annotations.ObjectFactory
import org.testng.annotations.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is
import static org.mockito.Mockito.*

@PrepareForTest(Clients)
class DefaultSetupServiceTest {

    @ObjectFactory
    IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory()
    }

    @Test
    void createOktaOrg() {

        String newOrgUrl = "https://org.example.com"

        DefaultSetupService setupService = setupService()

        OrganizationRequest orgRequest = mock(OrganizationRequest)
        RegistrationQuestions registrationQuestions = RegistrationQuestions.answers(true, orgRequest, null)
        File oktaPropsFile = mock(File)
        OrganizationResponse orgResponse = mock(OrganizationResponse)
        when(setupService.organizationCreator.createNewOrg(orgRequest)).thenReturn(orgResponse)
        when(orgResponse.getOrgUrl()).thenReturn(newOrgUrl)

        setupService.createOktaOrg(registrationQuestions, oktaPropsFile, false, false)

        verify(setupService.organizationCreator).createNewOrg(orgRequest)
    }

    @Test
    void verifyOktaOrg() {
        String newOrgUrl = "https://org.example.com"

        DefaultSetupService setupService = setupService()
        RegistrationQuestions registrationQuestions = RegistrationQuestions.answers(true, null, "123456")

        File oktaPropsFile = mock(File)
        OrganizationResponse orgResponse = mock(OrganizationResponse)
        when(setupService.organizationCreator.verifyNewOrg("test-id", "123456")).thenReturn(orgResponse)
        when(orgResponse.getOrgUrl()).thenReturn(newOrgUrl)
        when(orgResponse.getUpdatePasswordUrl()).thenReturn("https://reset.password")

        setupService.verifyOktaOrg("test-id",  registrationQuestions, oktaPropsFile)

        verify(setupService.organizationCreator).verifyNewOrg("test-id", "123456")
    }

    @Test
    void verifyOktaOrg_invalidCode() {
        String newOrgUrl = "https://org.example.com"

        DefaultSetupService setupService = setupService()

        File oktaPropsFile = mock(File)
        OrganizationResponse orgResponse = mock(OrganizationResponse)
        RegistrationQuestions registrationQuestions = mock(RegistrationQuestions)
        when(registrationQuestions.getVerificationCode()).thenReturn("123456").thenReturn("654321")
        when(setupService.organizationCreator.verifyNewOrg("test-id", "123456")).thenThrow(new FactorVerificationException(new ErrorResponse()
                .setStatus(401)
                .setError("test-error")
                .setMessage("test-message")
                .setCauses(["one", "two"])
        , new Throwable("root-test-cause")))
        when(setupService.organizationCreator.verifyNewOrg("test-id", "654321")).thenReturn(orgResponse)
        when(orgResponse.getOrgUrl()).thenReturn(newOrgUrl)
        when(orgResponse.getUpdatePasswordUrl()).thenReturn("https://reset.password")

        setupService.verifyOktaOrg("test-id", registrationQuestions, oktaPropsFile)

        verify(setupService.organizationCreator).verifyNewOrg("test-id", "123456")
    }

    @Test
    void createOidcApplicationExistingClient() {

        MutablePropertySource propertySource = mock(MutablePropertySource)
        String oidcAppName = "test-app-name"
        String orgUrl = "https://org.example.com"
        String groupClaimName = null
        String authorizationServerId = "test-auth-id"
        boolean interactive = false

        // existing client id found
        when(propertySource.getProperty("okta.oauth2.client-id")).thenReturn("existing-client-id")

        DefaultSetupService setupService = setupService()

        setupService.createOidcApplication(propertySource, oidcAppName, orgUrl, groupClaimName, null, authorizationServerId, interactive, OpenIdConnectApplicationType.WEB)

        // verify nothing happened
        PowerMockito.verifyNoMoreInteractions(setupService.organizationCreator,
                setupService.sdkConfigurationService,
                setupService.oidcAppCreator,
                setupService.authorizationServerService)
    }

    @Test
    void createOidcApplicationNoGroups() {

        MutablePropertySource propertySource = mock(MutablePropertySource)
        String oidcAppName = "test-app-name"
        String orgUrl = "https://org.example.com"
        String groupClaimName = null
        String authorizationServerId = "test-auth-id"
        boolean interactive = false

        PowerMockito.mockStatic(Clients)
        ClientBuilder clientBuilder = mock(ClientBuilder)
        Client client = mock(Client)
        when(clientBuilder.build()).thenReturn(client)
        when(Clients.builder()).thenReturn(clientBuilder)

        DefaultSetupService setupService = setupService()
        ExtensibleResource resource = mock(ExtensibleResource)
        when(resource.getString("client_id")).thenReturn("test-client-id")
        when(resource.getString("client_secret")).thenReturn("test-client-secret")
        when(setupService.oidcAppCreator.createOidcApp(client, oidcAppName, [])).thenReturn(resource)

        setupService.createOidcApplication(propertySource, oidcAppName, orgUrl, groupClaimName, null, authorizationServerId, interactive, OpenIdConnectApplicationType.WEB)

        ArgumentCaptor<Map> mapCapture = ArgumentCaptor.forClass(Map)
        verify(propertySource).addProperties(mapCapture.capture())
        assertThat mapCapture.getValue(), is([
                "okta.oauth2.issuer": "${orgUrl}/oauth2/${authorizationServerId}".toString(),
                "okta.oauth2.client-id": "test-client-id",
                "okta.oauth2.client-secret": "test-client-secret"
        ])

        // no group claim created
        PowerMockito.verifyNoMoreInteractions(setupService.authorizationServerService)
    }

    @Test
    void createOidcApplicationWithGroupClaim() {

        MutablePropertySource propertySource = mock(MutablePropertySource)
        String oidcAppName = "test-app-name"
        String orgUrl = "https://org.example.com"
        String groupClaimName = "test-group-claim"
        String authorizationServerId = "test-auth-id"
        boolean interactive = false

        PowerMockito.mockStatic(Clients)
        ClientBuilder clientBuilder = mock(ClientBuilder)
        Client client = mock(Client)
        when(clientBuilder.build()).thenReturn(client)
        when(Clients.builder()).thenReturn(clientBuilder)

        DefaultSetupService setupService = setupService()
        ExtensibleResource resource = mock(ExtensibleResource)
        when(resource.getString("client_id")).thenReturn("test-client-id")
        when(resource.getString("client_secret")).thenReturn("test-client-secret")
        when(setupService.oidcAppCreator.createOidcApp(client, oidcAppName, [])).thenReturn(resource)

        setupService.createOidcApplication(propertySource, oidcAppName, orgUrl, groupClaimName, null, authorizationServerId, interactive, OpenIdConnectApplicationType.WEB)

        ArgumentCaptor<Map> mapCapture = ArgumentCaptor.forClass(Map)
        verify(propertySource).addProperties(mapCapture.capture())
        assertThat mapCapture.getValue(), is([
                "okta.oauth2.issuer": "${orgUrl}/oauth2/${authorizationServerId}".toString(),
                "okta.oauth2.client-id": "test-client-id",
                "okta.oauth2.client-secret": "test-client-secret"
        ])

        verify(setupService.authorizationServerService).createGroupClaim(client, groupClaimName, authorizationServerId)
    }

    @Test
    void propertyNameTest() {
        def setupService1 = setupService()
        assertThat setupService1.getIssuerUriPropertyName(), is("okta.oauth2.issuer")
        assertThat setupService1.getClientIdPropertyName(), is("okta.oauth2.client-id")
        assertThat setupService1.getClientSecretPropertyName(), is("okta.oauth2.client-secret")

        def setupService2 = setupService("okta")
        assertThat setupService2.getIssuerUriPropertyName(), is("spring.security.oauth2.client.provider.okta.issuer-uri")
        assertThat setupService2.getClientIdPropertyName(), is("spring.security.oauth2.client.registration.okta.client-id")
        assertThat setupService2.getClientSecretPropertyName(), is("spring.security.oauth2.client.registration.okta.client-secret")

        def setupService3 = setupService("oidc")
        assertThat setupService3.getIssuerUriPropertyName(), is("spring.security.oauth2.client.provider.oidc.issuer-uri")
        assertThat setupService3.getClientIdPropertyName(), is("spring.security.oauth2.client.registration.oidc.client-id")
        assertThat setupService3.getClientSecretPropertyName(), is("spring.security.oauth2.client.registration.oidc.client-secret")
    }

    @Test
    void configureTrustedOriginTest_null() {

        Client client = mock(Client)
        new DefaultSetupService(null).configureTrustedOrigins(client, null)

        // trustedOrigins was null, nothing should have happened
        verifyNoInteractions(client)
    }

    @Test
    void configureTrustedOriginTest_empty() {

        Client client = mock(Client)
        new DefaultSetupService(null).configureTrustedOrigins(client, [])

        // trustedOrigins was null, nothing should have happened
        verifyNoInteractions(client)
    }

    @Test
    void configureTrustedOriginTest_existingMatchingOrigins() {

        String url = "http://foo.example.com"
        Client client = mock(Client)
        Scope cors = mock(Scope)
        Scope redirect = mock(Scope)
        TrustedOriginList origins = mock(TrustedOriginList)
        TrustedOrigin origin1 = mock(TrustedOrigin)

        when(client.instantiate(Scope)).thenReturn(cors).thenReturn(redirect)
        when(cors.setType(ScopeType.CORS)).thenReturn(cors)
        when(redirect.setType(ScopeType.REDIRECT)).thenReturn(redirect)
        when(client.listOrigins()).thenReturn(origins)
        when(origins.stream()).thenReturn([origin1].stream())
        when(origin1.getOrigin()).thenReturn(url)

        new DefaultSetupService(null).configureTrustedOrigins(client, ["http://foo.example.com"])

        // test specifics
        verify(client).listOrigins()
        verify(client, times(2)).instantiate(Scope)

        // scopes should be updated
        verify(origin1).setScopes([cors, redirect])
        verify(origin1).update()

        verifyNoMoreInteractions(client)
    }

    @Test
    void configureTrustedOriginTest_nonMatchingOrigins() {

        String url = "http://bar.example.com"
        Client client = mock(Client)
        Scope cors = mock(Scope)
        Scope redirect = mock(Scope)
        TrustedOriginList origins = mock(TrustedOriginList)
        TrustedOrigin origin1 = mock(TrustedOrigin)
        TrustedOrigin newOrigin = mock(TrustedOrigin, Mockito.RETURNS_DEEP_STUBS)

        when(client.instantiate(Scope)).thenReturn(cors).thenReturn(redirect)
        when(cors.setType(ScopeType.CORS)).thenReturn(cors)
        when(redirect.setType(ScopeType.REDIRECT)).thenReturn(redirect)
        when(client.listOrigins()).thenReturn(origins)
        when(origins.stream()).thenReturn([origin1].stream())
        when(origin1.getOrigin()).thenReturn(url)
        when(client.instantiate(TrustedOrigin)).thenReturn(newOrigin)
        when(newOrigin.setOrigin("http://foo.example.com")
                      .setName("http://foo.example.com")
                      .setScopes([cors, redirect])).thenReturn(newOrigin)

        new DefaultSetupService(null).configureTrustedOrigins(client, ["http://foo.example.com"])

        // test specifics
        verify(client).listOrigins()
        verify(client, times(2)).instantiate(Scope)

        // scopes should be updated
        verify(client).instantiate(TrustedOrigin)
        verify(client).createOrigin(newOrigin)

        verifyNoMoreInteractions(client)
    }

    @Test
    void configureTrustedOriginTest_noExistingOrigins() {

        Client client = mock(Client)
        Scope cors = mock(Scope)
        Scope redirect = mock(Scope)
        TrustedOriginList origins = mock(TrustedOriginList)
        TrustedOrigin newOrigin = mock(TrustedOrigin, Mockito.RETURNS_DEEP_STUBS)

        when(client.instantiate(Scope)).thenReturn(cors).thenReturn(redirect)
        when(cors.setType(ScopeType.CORS)).thenReturn(cors)
        when(redirect.setType(ScopeType.REDIRECT)).thenReturn(redirect)
        when(client.listOrigins()).thenReturn(origins)
        when(origins.stream()).thenReturn([].stream())
        when(client.instantiate(TrustedOrigin)).thenReturn(newOrigin)
        when(newOrigin.setOrigin("http://foo.example.com")
                .setName("http://foo.example.com")
                .setScopes([cors, redirect])).thenReturn(newOrigin)

        new DefaultSetupService(null).configureTrustedOrigins(client, ["http://foo.example.com"])

        // test specifics
        verify(client).listOrigins()
        verify(client, times(2)).instantiate(Scope)

        // scopes should be updated
        verify(client).instantiate(TrustedOrigin)
        verify(client).createOrigin(newOrigin)

        verifyNoMoreInteractions(client)
    }

    private static DefaultSetupService setupService(String springPropertyKey = null) {
        OktaOrganizationCreator organizationCreator = mock(OktaOrganizationCreator)
        SdkConfigurationService sdkConfigurationService = mock(SdkConfigurationService)
        OidcAppCreator oidcAppCreator = mock(OidcAppCreator)
        AuthorizationServerService authServerService = mock(AuthorizationServerService)
        when(sdkConfigurationService.loadUnvalidatedConfiguration()).thenReturn(new ClientConfiguration())

        DefaultSetupService setupService = new DefaultSetupService(sdkConfigurationService, organizationCreator, oidcAppCreator, authServerService, springPropertyKey)

        return setupService
    }
}
