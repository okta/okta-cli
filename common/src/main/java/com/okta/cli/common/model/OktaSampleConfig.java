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

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
public class OktaSampleConfig {

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private OAuthClient oauthClient;
    private String appConfig;
    private String command;
    private String directions;

    public OAuthClient getOAuthClient() {
        return oauthClient;
    }

    public void setOAuthClient(OAuthClient oauthClient) {
        this.oauthClient = oauthClient;
    }

    // ugly, but we use "oauthClient" and not "oAuthClient", and this makes snake yaml happy
    public void setOauthClient(OAuthClient oauthClient) {
        setOAuthClient(oauthClient);
    }

    @Data
    public static class OAuthClient {
        private List<String> redirectUris;
        private String applicationType;
    }
}
