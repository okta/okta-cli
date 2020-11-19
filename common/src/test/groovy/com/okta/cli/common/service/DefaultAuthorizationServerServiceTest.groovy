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


import com.okta.sdk.client.Client
import com.okta.sdk.ds.RequestBuilder
import com.okta.sdk.resource.ExtensibleResource
import org.testng.annotations.Test

import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*

class DefaultAuthorizationServerServiceTest {

    @Test
    void claimExistsTest() {
        DefaultAuthorizationServerService configureService = new DefaultAuthorizationServerService()

        ExtensibleResource existingClaimsResource = mock(ExtensibleResource)
        ExtensibleResource createdClaimsResource = mock(ExtensibleResource)
        ExtensibleResource conditions = mock(ExtensibleResource)
        RequestBuilder requestBuilder = mock(RequestBuilder)
        when(existingClaimsResource.get("items")).thenReturn([[name: "test-claim"]])
        Client client = mockClient(existingClaimsResource, createdClaimsResource, createdClaimsResource, conditions, requestBuilder)

        configureService.createGroupClaim(client, "test-claim", "test-auth-id")
        verifyNoInteractions(createdClaimsResource)
    }

    @Test
    void createClaimTest() {
        DefaultAuthorizationServerService configureService = new DefaultAuthorizationServerService()
        ExtensibleResource existingClaimsResource = mockExistingClaims()
        ExtensibleResource createdClaimsResourceAccess = mock(ExtensibleResource)
        ExtensibleResource createdClaimsResourceId = mock(ExtensibleResource)
        ExtensibleResource conditions = mock(ExtensibleResource)
        Client client = mockClient(existingClaimsResource, createdClaimsResourceAccess, createdClaimsResourceId, conditions)

        configureService.createGroupClaim(client, "test-claim", "test-auth-id")
        verifyResource("test-claim", createdClaimsResourceAccess, conditions, "RESOURCE")
        verifyResource("test-claim", createdClaimsResourceId, conditions, "IDENTITY")
    }

    static void verifyResource(String groupClaimName, ExtensibleResource claimResource, ExtensibleResource conditions, String claimType) {
        verify(conditions, times(2)).put("scopes", []) // reused for access and Id tokens in this test
        verify(claimResource).put("conditions", conditions)
        verify(claimResource).put("name", groupClaimName)
        verify(claimResource).put("status", "ACTIVE")
        verify(claimResource).put("claimType", claimType)
        verify(claimResource).put("valueType", "GROUPS")
        verify(claimResource).put("value", ".*")
        verify(claimResource).put("alwaysIncludeInToken", true)
        verify(claimResource).put("group_filter_type", "REGEX")
    }

    static Client mockClient(ExtensibleResource existingClaimResource,
                             ExtensibleResource createdClaimsResourceAccess,
                             ExtensibleResource createdClaimsResourceId,
                             ExtensibleResource conditions,
                             RequestBuilder getClaimsRequestBuilder = mock(RequestBuilder),
                             RequestBuilder createClaimsRequestBuilder = mock(RequestBuilder)) {
        Client client = mock(Client)
        when(client.instantiate(ExtensibleResource)).thenReturn(createdClaimsResourceAccess, conditions, createdClaimsResourceId, conditions)
        when(client.http()).thenReturn(getClaimsRequestBuilder, createClaimsRequestBuilder)

        when(getClaimsRequestBuilder.get(anyString(), eq(ExtensibleResource))).thenReturn(existingClaimResource)

        when(createClaimsRequestBuilder.setBody(createdClaimsResourceAccess)).thenReturn(createClaimsRequestBuilder)
        when(createClaimsRequestBuilder.setBody(createdClaimsResourceId)).thenReturn(createClaimsRequestBuilder)

        return client
    }

    static ExtensibleResource mockExistingClaims() {
        ExtensibleResource existingClaimResource = mock(ExtensibleResource)
        when(existingClaimResource.get("items")).thenReturn([] as Map)
        return existingClaimResource
    }
}
