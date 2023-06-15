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
package com.okta.cli.common.service

import com.okta.sdk.client.Client
import com.okta.sdk.ds.RequestBuilder
import com.okta.sdk.resource.ExtensibleResource
import com.okta.sdk.resource.application.*
import com.okta.sdk.resource.group.Group
import com.okta.sdk.resource.group.GroupList
import org.testng.annotations.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is
import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.*

class DefaultOidcAppCreatorTest {

    @Test
    void appAlreadyExistsTest() {

        String appName = "appLabel-appAlreadyExistsTest"
        String appId = "appId-appAlreadyExistsTest"

        Client client = mock(Client)
        ApplicationList appList = mock(ApplicationList)
        Application existingApp = mock(Application)
        List<Application> apps = [existingApp]
        RequestBuilder http = mock(RequestBuilder)
        ExtensibleResource response = mock(ExtensibleResource)

        DefaultOidcAppCreator appCreator = new DefaultOidcAppCreator()

        when(client.listApplications(appName, null, null, null)).thenReturn(appList)
        when(appList.stream()).thenReturn(apps.stream())
        when(existingApp.getLabel()).thenReturn(appName)
        when(existingApp.getId()).thenReturn(appId)
        when(client.http()).thenReturn(http)
        when(http.get("/api/v1/internal/apps/${appId}/settings/clientcreds", ExtensibleResource)).thenReturn(response)

        ExtensibleResource result = appCreator.createOidcApp(client, appName, [], [])

        assertThat result, is(response)
    }

    @Test
    void createNewApp() {

        String appName = "appLabel-createNewApp"
        String appId = "appId-createNewApp"
        String groupId = "everyone-id"

        Client client = mock(Client)
        ApplicationList appList = mock(ApplicationList)
        List<Application> apps = []
        RequestBuilder http = mock(RequestBuilder)
        ExtensibleResource response = mock(ExtensibleResource)

        OpenIdConnectApplication newApp = mock(OpenIdConnectApplication)
        OpenIdConnectApplicationSettings appSettings = mock(OpenIdConnectApplicationSettings)
        OpenIdConnectApplicationSettingsClient settingsClient = mock(OpenIdConnectApplicationSettingsClient)
        ApplicationGroupAssignment groupAssignment = mock(ApplicationGroupAssignment)

        GroupList groupList = mock(GroupList)
        Group group = mock(Group)

        DefaultOidcAppCreator appCreator = new DefaultOidcAppCreator()

        when(client.listApplications(appName, null, null, null)).thenReturn(appList)

        when(newApp.setLabel(appName)).thenReturn(newApp)
        when(newApp.setSettings(appSettings)).thenReturn(newApp)
        when(appSettings.setOAuthClient(settingsClient)).thenReturn(appSettings)

        when(client.instantiate(OpenIdConnectApplication)).thenReturn(newApp)
        when(client.instantiate(OpenIdConnectApplicationSettings)).thenReturn(appSettings)
        when(client.instantiate(OpenIdConnectApplicationSettingsClient)).thenReturn(settingsClient)
        when(client.instantiate(ApplicationGroupAssignment)).thenReturn(groupAssignment)

        when(settingsClient.setRedirectUris(any(List))).thenReturn(settingsClient)
        when(settingsClient.setResponseTypes(any(List))).thenReturn(settingsClient)
        when(settingsClient.setGrantTypes(any(List))).thenReturn(settingsClient)
        when(settingsClient.setApplicationType(OpenIdConnectApplicationType.WEB)).thenReturn(settingsClient)

        when(client.createApplication(newApp)).thenReturn(newApp)
        when(newApp.getId()).thenReturn(appId)
        when(appList.stream()).thenReturn(apps.stream())

        when(groupList.single()).thenReturn(group)
        when(group.getId()).thenReturn(groupId)
        when(client.listGroups(null, "profile.name eq \"everyone\"", null)).thenReturn(groupList)

        when(client.http()).thenReturn(http)
        when(http.get("/api/v1/internal/apps/${appId}/settings/clientcreds", ExtensibleResource)).thenReturn(response)

        ExtensibleResource result = appCreator.createOidcApp(client, appName, ["http://localhost:8080/callback", "http://localhost:8080/login/oauth2/code/okta"], [])

        assertThat result, is(response)

        verify(settingsClient).setRedirectUris(["http://localhost:8080/callback",
                                                "http://localhost:8080/login/oauth2/code/okta"])
        verify(settingsClient).setResponseTypes([OAuthResponseType.CODE])
        verify(settingsClient).setGrantTypes([OAuthGrantType.AUTHORIZATION_CODE])
        verify(settingsClient).setApplicationType(OpenIdConnectApplicationType.WEB)
        verify(settingsClient).setPostLogoutRedirectUris(["http://localhost:8080/"])
    }

    @Test
    void createNewAppWithPostLogoutUris() {

        String appName = "appLabel-createNewApp"
        String appId = "appId-createNewApp"
        String groupId = "everyone-id"

        Client client = mock(Client)
        ApplicationList appList = mock(ApplicationList)
        List<Application> apps = []
        RequestBuilder http = mock(RequestBuilder)
        ExtensibleResource response = mock(ExtensibleResource)

        OpenIdConnectApplication newApp = mock(OpenIdConnectApplication)
        OpenIdConnectApplicationSettings appSettings = mock(OpenIdConnectApplicationSettings)
        OpenIdConnectApplicationSettingsClient settingsClient = mock(OpenIdConnectApplicationSettingsClient)
        ApplicationGroupAssignment groupAssignment = mock(ApplicationGroupAssignment)

        GroupList groupList = mock(GroupList)
        Group group = mock(Group)

        DefaultOidcAppCreator appCreator = new DefaultOidcAppCreator()

        when(client.listApplications(appName, null, null, null)).thenReturn(appList)

        when(newApp.setLabel(appName)).thenReturn(newApp)
        when(newApp.setSettings(appSettings)).thenReturn(newApp)
        when(appSettings.setOAuthClient(settingsClient)).thenReturn(appSettings)

        when(client.instantiate(OpenIdConnectApplication)).thenReturn(newApp)
        when(client.instantiate(OpenIdConnectApplicationSettings)).thenReturn(appSettings)
        when(client.instantiate(OpenIdConnectApplicationSettingsClient)).thenReturn(settingsClient)
        when(client.instantiate(ApplicationGroupAssignment)).thenReturn(groupAssignment)

        when(settingsClient.setRedirectUris(any(List))).thenReturn(settingsClient)
        when(settingsClient.setResponseTypes(any(List))).thenReturn(settingsClient)
        when(settingsClient.setGrantTypes(any(List))).thenReturn(settingsClient)
        when(settingsClient.setApplicationType(OpenIdConnectApplicationType.WEB)).thenReturn(settingsClient)

        when(client.createApplication(newApp)).thenReturn(newApp)
        when(newApp.getId()).thenReturn(appId)
        when(appList.stream()).thenReturn(apps.stream())

        when(groupList.single()).thenReturn(group)
        when(group.getId()).thenReturn(groupId)
        when(client.listGroups(null, "profile.name eq \"everyone\"", null)).thenReturn(groupList)

        when(client.http()).thenReturn(http)
        when(http.get("/api/v1/internal/apps/${appId}/settings/clientcreds", ExtensibleResource)).thenReturn(response)

        ExtensibleResource result = appCreator.createOidcApp(client, appName, ["http://localhost:8080/callback", "http://localhost:8080/login/oauth2/code/okta"], ["http://localhost:8080/logout", "http://localhost:8080/logout2"])

        assertThat result, is(response)

        verify(settingsClient).setRedirectUris(["http://localhost:8080/callback",
                                                "http://localhost:8080/login/oauth2/code/okta"])
        verify(settingsClient).setResponseTypes([OAuthResponseType.CODE])
        verify(settingsClient).setGrantTypes([OAuthGrantType.AUTHORIZATION_CODE])
        verify(settingsClient).setApplicationType(OpenIdConnectApplicationType.WEB)
        verify(settingsClient).setPostLogoutRedirectUris(["http://localhost:8080/logout", "http://localhost:8080/logout2"])
    }

    @Test
    void createNativeAppWithoutLogoutUri() {

        String appName = "appLabel-createNativeApp"
        String appId = "appId-createNativeApp"
        String groupId = "everyone-id"

        Client client = mock(Client)
        ApplicationList appList = mock(ApplicationList)
        List<Application> apps = []
        RequestBuilder http = mock(RequestBuilder)
        ExtensibleResource response = mock(ExtensibleResource)

        OpenIdConnectApplication newApp = mock(OpenIdConnectApplication)
        OpenIdConnectApplicationSettings appSettings = mock(OpenIdConnectApplicationSettings)
        OpenIdConnectApplicationSettingsClient settingsClient = mock(OpenIdConnectApplicationSettingsClient)
        ApplicationGroupAssignment groupAssignment = mock(ApplicationGroupAssignment)
        ApplicationCredentialsOAuthClient credentialsOAuthClient = mock(ApplicationCredentialsOAuthClient)
        OAuthApplicationCredentials appCreds = mock(OAuthApplicationCredentials)

        GroupList groupList = mock(GroupList)
        Group group = mock(Group)

        DefaultOidcAppCreator appCreator = new DefaultOidcAppCreator()

        when(client.listApplications(appName, null, null, null)).thenReturn(appList)
        when(newApp.setLabel(appName)).thenReturn(newApp)
        when(newApp.setSettings(appSettings)).thenReturn(newApp)
        when(newApp.setCredentials((ApplicationCredentials) appCreds)).thenReturn(newApp)
        when(appSettings.setOAuthClient(settingsClient)).thenReturn(appSettings)
        when(appCreds.setOAuthClient(credentialsOAuthClient)).thenReturn(appCreds)
        when(credentialsOAuthClient.setTokenEndpointAuthMethod(OAuthEndpointAuthenticationMethod.NONE)).thenReturn(credentialsOAuthClient)

        when(client.instantiate(OpenIdConnectApplication)).thenReturn(newApp)
        when(client.instantiate(OpenIdConnectApplicationSettings)).thenReturn(appSettings)
        when(client.instantiate(OpenIdConnectApplicationSettingsClient)).thenReturn(settingsClient)
        when(client.instantiate(ApplicationGroupAssignment)).thenReturn(groupAssignment)
        when(client.instantiate(OAuthApplicationCredentials)).thenReturn(appCreds)
        when(client.instantiate(ApplicationCredentialsOAuthClient)).thenReturn(credentialsOAuthClient)

        when(settingsClient.setRedirectUris(any(List))).thenReturn(settingsClient)
        when(settingsClient.setResponseTypes(any(List))).thenReturn(settingsClient)
        when(settingsClient.setGrantTypes(any(List))).thenReturn(settingsClient)
        when(settingsClient.setApplicationType(OpenIdConnectApplicationType.NATIVE)).thenReturn(settingsClient)
        when(groupAssignment.setPriority(any(Integer))).thenReturn(groupAssignment)

        when(client.createApplication(newApp)).thenReturn(newApp)
        when(newApp.getId()).thenReturn(appId)
        when(appList.stream()).thenReturn(apps.stream())

        when(groupList.single()).thenReturn(group)
        when(group.getId()).thenReturn(groupId)
        when(client.listGroups(null, "profile.name eq \"everyone\"", null)).thenReturn(groupList)

        when(client.http()).thenReturn(http)
        when(http.get("/api/v1/internal/apps/${appId}/settings/clientcreds", ExtensibleResource)).thenReturn(response)

        ExtensibleResource result = appCreator.createOidcNativeApp(client, appName, ["http://localhost:8080/callback", "http://localhost:8080/login/oauth2/code/okta"], null)

        assertThat result, is(response)

        verify(settingsClient).setRedirectUris(["http://localhost:8080/callback",
                                                "http://localhost:8080/login/oauth2/code/okta"])
        verify(settingsClient).setResponseTypes([OAuthResponseType.CODE])
        verify(settingsClient).setGrantTypes([OAuthGrantType.AUTHORIZATION_CODE])
        verify(settingsClient).setApplicationType(OpenIdConnectApplicationType.NATIVE)
        verify(settingsClient, never()).setPostLogoutRedirectUris(any(List))
    }

    @Test
    void createDeviceApp() {

        String appName = "appLabel-createDeviceApp"
        String appId = "appId-createDeviceApp"
        String groupId = "everyone-id"

        Client client = mock(Client)
        ApplicationList appList = mock(ApplicationList)
        List<Application> apps = []
        RequestBuilder http = mock(RequestBuilder)
        ExtensibleResource response = mock(ExtensibleResource)

        OpenIdConnectApplication newApp = mock(OpenIdConnectApplication)
        OpenIdConnectApplicationSettings appSettings = mock(OpenIdConnectApplicationSettings)
        OpenIdConnectApplicationSettingsClient settingsClient = mock(OpenIdConnectApplicationSettingsClient)
        ApplicationGroupAssignment groupAssignment = mock(ApplicationGroupAssignment)
        ApplicationCredentialsOAuthClient credentialsOAuthClient = mock(ApplicationCredentialsOAuthClient)
        OAuthApplicationCredentials appCreds = mock(OAuthApplicationCredentials)

        GroupList groupList = mock(GroupList)
        Group group = mock(Group)

        DefaultOidcAppCreator appCreator = new DefaultOidcAppCreator()

        when(client.listApplications(appName, null, null, null)).thenReturn(appList)
        when(newApp.setLabel(appName)).thenReturn(newApp)
        when(newApp.setSettings(appSettings)).thenReturn(newApp)
        when(newApp.setCredentials((OAuthApplicationCredentials) appCreds)).thenReturn(newApp)
        when(appSettings.setOAuthClient(settingsClient)).thenReturn(appSettings)
        when(appCreds.setOAuthClient(credentialsOAuthClient)).thenReturn(appCreds)
        when(credentialsOAuthClient.setTokenEndpointAuthMethod(OAuthEndpointAuthenticationMethod.NONE)).thenReturn(credentialsOAuthClient)

        when(client.instantiate(OpenIdConnectApplication)).thenReturn(newApp)
        when(client.instantiate(OpenIdConnectApplicationSettings)).thenReturn(appSettings)
        when(client.instantiate(OpenIdConnectApplicationSettingsClient)).thenReturn(settingsClient)
        when(client.instantiate(ApplicationGroupAssignment)).thenReturn(groupAssignment)
        when(client.instantiate(OAuthApplicationCredentials)).thenReturn(appCreds)
        when(client.instantiate(ApplicationCredentialsOAuthClient)).thenReturn(credentialsOAuthClient)

        when(settingsClient.setRedirectUris(any(List))).thenReturn(settingsClient)
        when(settingsClient.setResponseTypes(any(List))).thenReturn(settingsClient)
        when(settingsClient.setGrantTypes(any(List))).thenReturn(settingsClient)
        when(settingsClient.setApplicationType(OpenIdConnectApplicationType.NATIVE)).thenReturn(settingsClient)
        when(groupAssignment.setPriority(any(Integer))).thenReturn(groupAssignment)

        when(client.createApplication(newApp)).thenReturn(newApp)
        when(newApp.getId()).thenReturn(appId)
        when(appList.stream()).thenReturn(apps.stream())

        when(groupList.single()).thenReturn(group)
        when(group.getId()).thenReturn(groupId)
        when(client.listGroups(null, "profile.name eq \"everyone\"", null)).thenReturn(groupList)

        when(client.http()).thenReturn(http)
        when(http.get("/api/v1/internal/apps/${appId}/settings/clientcreds", ExtensibleResource)).thenReturn(response)

        ExtensibleResource result = appCreator.createDeviceCodeApp(client, appName)

        assertThat result, is(response)

        verify(settingsClient).put("grant_types", [SetupService.APP_TYPE_DEVICE])
        verify(settingsClient).setApplicationType(OpenIdConnectApplicationType.NATIVE)
        verify(settingsClient, never()).setPostLogoutRedirectUris(any(List))
        verify(settingsClient, never()).setRedirectUris(any(List))
    }
}
