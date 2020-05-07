package com.okta.cli.common.service

import com.okta.cli.common.config.MutablePropertySource
import com.okta.cli.common.model.OrganizationRequest
import com.okta.cli.common.model.OrganizationResponse
import com.okta.sdk.client.Client
import com.okta.sdk.client.ClientBuilder
import com.okta.sdk.client.Clients
import com.okta.sdk.impl.config.ClientConfiguration
import com.okta.sdk.resource.ExtensibleResource
import org.mockito.ArgumentCaptor
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.testng.PowerMockObjectFactory
import org.testng.IObjectFactory
import org.testng.annotations.ObjectFactory
import org.testng.annotations.Test

import java.util.function.Supplier

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
    void configEnvWithExistingOrg() {

        Supplier<OrganizationRequest> organizationRequestSupplier = mock(Supplier)
        OrganizationRequest request = mock(OrganizationRequest)
        File oktaPropsFile = mock(File)
        MutablePropertySource propertySource = mock(MutablePropertySource)
        String oidcAppName = "test-app-name"
        String groupClaimName = "group-claim"
        String authorizationServerId = "test-auth-id"
        boolean demo = false
        boolean interactive = false
        String orgUrl = "http://okta.example.com"

        def originalSetupService = setupService()
        def setupService = spy(originalSetupService)


        ClientConfiguration clientConfiguration = mock(ClientConfiguration)

        when(originalSetupService.sdkConfigurationService.loadUnvalidatedConfiguration()).thenReturn(clientConfiguration)
        when(clientConfiguration.getBaseUrl()).thenReturn(orgUrl)
        when(organizationRequestSupplier.get()).thenReturn(request)

        // these methods are tested elsewhere in this class
        doNothing().when(setupService).createOidcApplication(propertySource, oidcAppName, orgUrl, groupClaimName, authorizationServerId, interactive)

        setupService.configureEnvironment(organizationRequestSupplier,
                                          oktaPropsFile,
                                          propertySource,
                                          oidcAppName,
                                          groupClaimName,
                                          authorizationServerId,
                                          demo,
                                          interactive)

        verify(setupService).createOidcApplication(propertySource, oidcAppName, orgUrl, groupClaimName, authorizationServerId, interactive)
    }

    @Test
    void configEnvNewOrg() {

        Supplier<OrganizationRequest> organizationRequestSupplier = mock(Supplier)
        OrganizationRequest request = mock(OrganizationRequest)
        File oktaPropsFile = mock(File)
        MutablePropertySource propertySource = mock(MutablePropertySource)
        String oidcAppName = "test-app-name"
        String groupClaimName = "group-claim"
        String authorizationServerId = "test-auth-id"
        boolean demo = false
        boolean interactive = false
        String orgUrl = "http://okta.example.com"

        def originalSetupService = setupService()
        def setupService = spy(originalSetupService)


        ClientConfiguration clientConfiguration = mock(ClientConfiguration)

        when(originalSetupService.sdkConfigurationService.loadUnvalidatedConfiguration()).thenReturn(clientConfiguration)
        when(clientConfiguration.getBaseUrl()).thenReturn(null)
        when(organizationRequestSupplier.get()).thenReturn(request)

        // these methods are tested elsewhere in this class
        doNothing().when(setupService).createOidcApplication(propertySource, oidcAppName, orgUrl, groupClaimName, authorizationServerId, interactive)
        doReturn(orgUrl).when(setupService).createOktaOrg(organizationRequestSupplier, oktaPropsFile, demo, interactive)

        setupService.configureEnvironment(organizationRequestSupplier,
                oktaPropsFile,
                propertySource,
                oidcAppName,
                groupClaimName,
                authorizationServerId,
                demo,
                interactive)

        verify(setupService).createOidcApplication(propertySource, oidcAppName, orgUrl, groupClaimName, authorizationServerId, interactive)
    }

    @Test
    void createOktaOrg() {

        String newOrgUrl = "https://org.example.com"
        String newOrgToken = "test-token"

        DefaultSetupService setupService = setupService()

        Supplier<OrganizationRequest> organizationRequestSupplier = mock(Supplier)
        OrganizationRequest orgRequest = mock(OrganizationRequest)
        File oktaPropsFile = mock(File)
        OrganizationResponse orgResponse = mock(OrganizationResponse)
        when(organizationRequestSupplier.get()).thenReturn(orgRequest)
        when(setupService.organizationCreator.createNewOrg("https://start.okta.dev/", orgRequest)).thenReturn(orgResponse)
        when(orgResponse.getOrgUrl()).thenReturn(newOrgUrl)
        when(orgResponse.getApiToken()).thenReturn(newOrgToken)

        setupService.createOktaOrg(organizationRequestSupplier, oktaPropsFile, false, false)

        verify(setupService.sdkConfigurationService).writeOktaYaml(newOrgUrl, newOrgToken, oktaPropsFile)
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

        setupService.createOidcApplication(propertySource, oidcAppName, orgUrl, groupClaimName, authorizationServerId, interactive)

        // verify nothing happened
        PowerMockito.verifyNoMoreInteractions(setupService.organizationCreator,
                setupService.sdkConfigurationService,
                setupService.oidcAppCreator,
                setupService.authorizationServerConfigureService)
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
        when(setupService.oidcAppCreator.createOidcApp(client, oidcAppName)).thenReturn(resource)

        setupService.createOidcApplication(propertySource, oidcAppName, orgUrl, groupClaimName, authorizationServerId, interactive)

        ArgumentCaptor<Map> mapCapture = ArgumentCaptor.forClass(Map)
        verify(propertySource).addProperties(mapCapture.capture())
        assertThat mapCapture.getValue(), is([
                "okta.oauth2.issuer": "${orgUrl}/oauth2/${authorizationServerId}".toString(),
                "okta.oauth2.client-id": "test-client-id",
                "okta.oauth2.client-secret": "test-client-secret"
        ])

        // no group claim created
        PowerMockito.verifyNoMoreInteractions(setupService.authorizationServerConfigureService)
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
        when(setupService.oidcAppCreator.createOidcApp(client, oidcAppName)).thenReturn(resource)

        setupService.createOidcApplication(propertySource, oidcAppName, orgUrl, groupClaimName, authorizationServerId, interactive)

        ArgumentCaptor<Map> mapCapture = ArgumentCaptor.forClass(Map)
        verify(propertySource).addProperties(mapCapture.capture())
        assertThat mapCapture.getValue(), is([
                "okta.oauth2.issuer": "${orgUrl}/oauth2/${authorizationServerId}".toString(),
                "okta.oauth2.client-id": "test-client-id",
                "okta.oauth2.client-secret": "test-client-secret"
        ])

        verify(setupService.authorizationServerConfigureService).createGroupClaim(client, groupClaimName, authorizationServerId)
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


    private static DefaultSetupService setupService(String springPropertyKey = null) {
        OktaOrganizationCreator organizationCreator = mock(OktaOrganizationCreator)
        SdkConfigurationService sdkConfigurationService = mock(SdkConfigurationService)
        OidcAppCreator oidcAppCreator = mock(OidcAppCreator)
        AuthorizationServerConfigureService authServerService = mock(AuthorizationServerConfigureService)
        when(sdkConfigurationService.loadUnvalidatedConfiguration()).thenReturn(new ClientConfiguration())

        DefaultSetupService setupService = new DefaultSetupService(sdkConfigurationService, organizationCreator, oidcAppCreator, authServerService, springPropertyKey)

        return setupService
    }
}
