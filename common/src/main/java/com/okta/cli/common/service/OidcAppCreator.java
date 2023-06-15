/*
 * Copyright 2018-Present Okta, Inc.
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
package com.okta.cli.common.service;

import com.okta.sdk.client.Client;
import com.okta.sdk.resource.ExtensibleResource;

import java.util.List;

public interface OidcAppCreator {

    ExtensibleResource createOidcApp(Client client, String oidcAppName, List<String> redirectUris, List<String> postLogoutRedirectUris);

    ExtensibleResource createOidcNativeApp(Client client, String oidcAppName, List<String> redirectUris, List<String> postLogoutRedirectUris);

    ExtensibleResource createOidcSpaApp(Client client, String oidcAppName, List<String> redirectUris, List<String> postLogoutRedirectUris);

    ExtensibleResource createOidcServiceApp(Client client, String oidcAppName, List<String> redirectUris);

    ExtensibleResource createDeviceCodeApp(Client client, String oidcAppName);
}
