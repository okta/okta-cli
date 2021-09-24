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
package com.okta.cli.common.model;

import com.okta.sdk.impl.ds.InternalDataStore;
import com.okta.sdk.impl.resource.AbstractCollectionResource;
import com.okta.sdk.impl.resource.ArrayProperty;
import com.okta.sdk.impl.resource.Property;
import com.okta.sdk.resource.user.User;

import java.util.Collections;
import java.util.Map;

public class AuthorizationServerList extends AbstractCollectionResource<AuthorizationServer> {

    private static final ArrayProperty<User> ITEMS = new ArrayProperty<>("items", User.class);
    private static final Map<String, Property> PROPERTY_DESCRIPTORS = Collections.unmodifiableMap(createPropertyDescriptorMap(ITEMS));

    public AuthorizationServerList(InternalDataStore dataStore) {
        super(dataStore);
    }

    public AuthorizationServerList(InternalDataStore dataStore, Map<String, Object> properties) {
        super(dataStore, properties);
    }

    public AuthorizationServerList(InternalDataStore dataStore, Map<String, Object> properties, Map<String, Object> queryParams) {
        super(dataStore, properties, queryParams);
    }

    @Override
    protected Class<AuthorizationServer> getItemType() {
        return AuthorizationServer.class;
    }

    @Override
    public Map<String, Property> getPropertyDescriptors() {
        return PROPERTY_DESCRIPTORS;
    }
}