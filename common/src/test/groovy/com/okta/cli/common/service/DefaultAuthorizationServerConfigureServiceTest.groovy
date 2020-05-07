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

import com.okta.cli.common.TestUtil
import com.okta.sdk.client.Client
import com.okta.sdk.ds.RequestBuilder
import com.okta.sdk.impl.error.DefaultError
import com.okta.sdk.resource.ExtensibleResource
import com.okta.sdk.resource.ResourceException
import org.testng.annotations.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.mockito.Mockito.*

class DefaultAuthorizationServerConfigureServiceTest {

    @Test
    void clientThrowsResourceExceptionIgnore() {
        DefaultAuthorizationServerConfigureService configureService = new DefaultAuthorizationServerConfigureService()

        ExtensibleResource claimResource = mock(ExtensibleResource)
        ExtensibleResource conditions = mock(ExtensibleResource)
        RequestBuilder requestBuilder = mock(RequestBuilder)
        Client client = mockClient(claimResource, conditions, requestBuilder)
        DefaultError error = new DefaultError()
            .setMessage("Api validation failed: name")
            .setStatus(400)

        when(requestBuilder.post("/api/v1/authorizationServers/test-auth-id/claims", ExtensibleResource)).thenThrow(new ResourceException(error))

        // return false
        assertThat("expected createGroupClaim to return false when ResourceException is thrown", !configureService.createGroupClaim(client, "test-claim", "test-auth-id"))

        verifyResource("test-claim", claimResource, conditions)
    }

    @Test
    void clientThrowsResourceExceptionFail() {
        DefaultAuthorizationServerConfigureService configureService = new DefaultAuthorizationServerConfigureService()

        ExtensibleResource claimResource = mock(ExtensibleResource)
        ExtensibleResource conditions = mock(ExtensibleResource)
        RequestBuilder requestBuilder = mock(RequestBuilder)
        Client client = mockClient(claimResource, conditions, requestBuilder)
        DefaultError error = new DefaultError()
                .setMessage("An error message in a different format will cause a failure")
                .setStatus(400)

        when(requestBuilder.post("/api/v1/authorizationServers/test-auth-id/claims", ExtensibleResource)).thenThrow(new ResourceException(error))

        TestUtil.expectException ResourceException, { configureService.createGroupClaim(client, "test-claim", "test-auth-id") }

        verifyResource("test-claim", claimResource, conditions)
    }

    @Test
    void clientSuccess() {
        DefaultAuthorizationServerConfigureService configureService = new DefaultAuthorizationServerConfigureService()
        ExtensibleResource claimResource = mock(ExtensibleResource)
        ExtensibleResource conditions = mock(ExtensibleResource)
        Client client = mockClient(claimResource, conditions)

        // return false
        assertThat("expected createGroupClaim to return true", configureService.createGroupClaim(client, "test-claim", "test-auth-id"))

        verifyResource("test-claim", claimResource, conditions)
    }

    void verifyResource(String groupClaimName, ExtensibleResource claimResource, ExtensibleResource conditions) {
        verify(conditions).put("scopes", [])
        verify(claimResource).put("conditions", conditions)
        verify(claimResource).put("name", groupClaimName)
        verify(claimResource).put("status", "ACTIVE")
        verify(claimResource).put("claimType", "RESOURCE")
        verify(claimResource).put("valueType", "GROUPS")
        verify(claimResource).put("value", ".*")
        verify(claimResource).put("alwaysIncludeInToken", true)
        verify(claimResource).put("group_filter_type", "REGEX")
    }

    Client mockClient(ExtensibleResource claimResource, ExtensibleResource conditions, RequestBuilder requestBuilder = mock(RequestBuilder)) {
        Client client = mock(Client)
        when(client.instantiate(ExtensibleResource)).thenReturn(claimResource, conditions)
        when(client.http()).thenReturn(requestBuilder)
        when(requestBuilder.setBody(claimResource)).thenReturn(requestBuilder)

        return client
    }

}
