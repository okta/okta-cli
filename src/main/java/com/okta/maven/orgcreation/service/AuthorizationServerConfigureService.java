package com.okta.maven.orgcreation.service;

import com.okta.sdk.client.Client;

public interface AuthorizationServerConfigureService {

    boolean createGroupClaim(Client client, String groupClaimName, String authorizationServerId);
}
