package com.okta.cli.common.service;

import com.okta.cli.common.config.MutablePropertySource;
import com.okta.cli.common.model.OrganizationRequest;
import com.okta.cli.common.progressbar.ProgressBar;

import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

public interface SetupService {

    void configureEnvironment(
            Supplier<OrganizationRequest> organizationRequestSupplier, // A supplier, because a Mojo may prompt the user
            File oktaPropsFile,
            MutablePropertySource propertySource,
            String oidcAppName,
            String groupClaimName,
            String authorizationServerId,
            boolean demo,
            boolean interactive,
            String... redirectUris) throws IOException, ClientConfigurationException;

    String createOktaOrg(Supplier<OrganizationRequest> organizationRequestSupplier,
                         File oktaPropsFile,
                         boolean demo,
                         boolean interactive) throws IOException, ClientConfigurationException;

    void createOidcApplication(MutablePropertySource propertySource,
                               String oidcAppName,
                               String orgUrl,
                               String groupClaimName,
                               String authorizationServerId,
                               boolean interactive,
                               String... redirectUris) throws IOException;
}
