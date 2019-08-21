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
package com.okta.maven.orgcreation;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.okta.commons.lang.Strings;
import com.okta.maven.orgcreation.models.OrganizationResponse;
import com.okta.maven.orgcreation.models.OrganizationRequest;
import com.okta.sdk.client.Client;
import com.okta.sdk.client.Clients;
import com.okta.sdk.impl.client.DefaultClientBuilder;
import com.okta.sdk.impl.config.ClientConfiguration;
import com.okta.sdk.resource.ExtensibleResource;
import com.okta.sdk.resource.application.Application;
import com.okta.sdk.resource.application.ApplicationGroupAssignment;
import com.okta.sdk.resource.application.OAuthGrantType;
import com.okta.sdk.resource.application.OAuthResponseType;
import com.okta.sdk.resource.application.OpenIdConnectApplication;
import com.okta.sdk.resource.application.OpenIdConnectApplicationSettings;
import com.okta.sdk.resource.application.OpenIdConnectApplicationSettingsClient;
import com.okta.sdk.resource.application.OpenIdConnectApplicationType;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.util.StringUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Mojo(name = "init", defaultPhase = LifecyclePhase.NONE, threadSafe = false, aggregator = true, requiresProject=false)
public class OrgCreationMojo extends AbstractMojo {

    @Parameter(property = "email")
    private String email;

    @Parameter(property = "firstName")
    private String firstName;

    @Parameter(property = "lastName")
    private String lastName;

    @Parameter(property = "company")
    private String company;

    @Parameter(property = "applicationYaml", defaultValue = "${project.basedir}/src/main/resources/application.yml")
    private File applicationYaml;

    @Parameter(property = "apiUrl", defaultValue = "https://obscure-atoll-66316.herokuapp.com")
    private String apiBaseUrl;

    @Parameter(defaultValue = "${settings}", readonly = true)
    protected Settings settings;

    @Component
    private Prompter prompter;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        // check if okta client config exists?
        ClientConfiguration clientConfiguration;

        try {
            Field field = DefaultClientBuilder.class.getDeclaredField("clientConfig");

            AccessController.doPrivileged((PrivilegedAction) () -> {
                field.setAccessible(true);
                return null;
            });
            clientConfiguration = (ClientConfiguration) field.get(new DefaultClientBuilder());

            String orgUrl;

            if (StringUtils.isEmpty(clientConfiguration.getBaseUrl())) {
                getLog().info("Current OrgUrl is empty, creating new org...");

                OrganizationResponse newOrg = createNewOrg();
                orgUrl = newOrg.getOrgUrl();

                getLog().info("OrgUrl: "+ orgUrl);
                getLog().info("Check your email address to verify your account.\n");

                getLog().info("Writing Okta config to ~/.okta/okta.yaml");
                // write ~/.okta/okta.yaml
                writeOktaYaml(newOrg, orgUrl);

            } else {
                orgUrl = clientConfiguration.getBaseUrl();
                getLog().info("Current OrgUrl: " + clientConfiguration.getBaseUrl());
            }

            Map<String, Object> springProps = new LinkedHashMap<>();
            Yaml springAppYaml = new Yaml(yamlOptions());
            if (applicationYaml.exists()) {
                springProps = (Map<String, Object>) springAppYaml.loadAs(fileReader(applicationYaml), Map.class);
            }

            Map<String, Object> oktaProps = (Map<String, Object>) springProps.getOrDefault("okta", new HashMap<>());
            Map<String, Object> oauth2Props = (Map<String, Object>) oktaProps.getOrDefault("oauth2", new HashMap<>());
            if (Strings.isEmpty(oauth2Props.get("issuer"))) {

                // create ODIC application
                Client client = Clients.builder().build();

                Application app = client.instantiate(OpenIdConnectApplication.class)
                        .setLabel("app+" + UUID.randomUUID().toString()) // TODO: set name to Maven Project name
                        .setSettings(client.instantiate(OpenIdConnectApplicationSettings.class)
                            .setOAuthClient(client.instantiate(OpenIdConnectApplicationSettingsClient.class)
        //                        .setClientUri("https://example.com/client")
                                .setRedirectUris(Arrays.asList("http://localhost:8080/authorization-code/callback",
                                                               "http://localhost:8080/login/oauth2/code/okta"))
                                .setResponseTypes(Arrays.asList(OAuthResponseType.TOKEN,
                                                   OAuthResponseType.ID_TOKEN,
                                                   OAuthResponseType.CODE))
                                .setGrantTypes(Arrays.asList(OAuthGrantType.IMPLICIT,
                                                OAuthGrantType.AUTHORIZATION_CODE))
                                .setApplicationType(OpenIdConnectApplicationType.WEB)
                                .setPolicyUri("https://example.com/client/policy")));
                client.createApplication(app);

                ExtensibleResource clientCredsResponse = client.http()
                    .get("/api/v1/internal/apps/" + app.getId() + "/settings/clientcreds", ExtensibleResource.class);

                // write application.yml
                springProps.put("okta", oktaProps);
                oktaProps.put("oauth2", oauth2Props);
                oauth2Props.put("issuer", orgUrl + "/oauth2/default");
                oauth2Props.put("client-id", clientCredsResponse.getString("client_id"));
                oauth2Props.put("client-secret", clientCredsResponse.getString("client_secret"));

                springAppYaml.dump(springProps, fileWriter(applicationYaml));
                getLog().info("Created OIDC application, client-id: " + clientCredsResponse.getString("client_id"));

                // assign Everyone group to new app
                // look up 'everyone' group id
                String everyoneGroupId = client.listGroups("everyone", null, null).single().getId();

                ApplicationGroupAssignment aga = client.instantiate(ApplicationGroupAssignment.class).setPriority(2);
                app.createApplicationGroupAssignment(everyoneGroupId, aga);
            }

        } catch (NoSuchFieldException | IllegalAccessException | IOException e) {
            throw new MojoExecutionException("hack hack hack: " + e.getMessage(), e);
        }
    }

    private DumperOptions yamlOptions() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        return options;
    }

    private OrganizationResponse createNewOrg() throws IOException, MojoExecutionException {

        HttpClient httpClient = HttpClients.createDefault();
        HttpPost post = new HttpPost(apiBaseUrl + "/org/create");


        ObjectMapper objectMapper = new ObjectMapper();
        String postBody = objectMapper.writeValueAsString(new OrganizationRequest()
            .setFirstName(promptIfNull(firstName, "firstName", "First name"))
            .setLastName(promptIfNull(lastName, "lastName", "Last name"))
            .setEmail(promptIfNull(email, "email", "Email Address"))
            .setOrganization(promptIfNull(company, "company", "Company")));
        post.setEntity(new StringEntity(postBody, StandardCharsets.UTF_8));
        post.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        HttpResponse response = httpClient.execute(post);
        return objectMapper.reader().readValue(new JsonFactory().createParser(response.getEntity().getContent()), OrganizationResponse.class);
    }

    private String promptIfNull(String currentValue, String keyName, String promptText) throws MojoExecutionException {

        String value = currentValue;

        if (StringUtils.isEmpty(value)) {
            if (settings.isInteractiveMode()) {
                try {
                    value = prompter.prompt(promptText);
                }
                catch (PrompterException e) {
                    throw new MojoExecutionException( e.getMessage(), e );
                }
            } else {
                throw new MojoExecutionException( "You must specify the '" + keyName + "' property either on the command line " +
                        "-D" + keyName + "=... or run in interactive mode" );
            }
        }
        return value;
    }

    private void writeOktaYaml(OrganizationResponse organizationResponse, String orgUrl) throws IOException {
        Map<String, Object> rootProps = new HashMap<>();
        Map<String, Object> rootOktaProps = new HashMap<>();
        Map<String, Object> clientOktaProps = new HashMap<>();

        rootProps.put("okta", rootOktaProps);
        rootOktaProps.put("client", clientOktaProps);
        clientOktaProps.put("orgUrl", orgUrl);
        clientOktaProps.put("token", organizationResponse.getApiToken());

        File oktaPropsFile = new File(System.getProperty("user.home"), ".okta/okta.yaml");
        File parentDir = oktaPropsFile.getParentFile();

        // create parent dir
        if (!(parentDir.exists() || parentDir.mkdirs())) {
            throw new IOException("Unable to create directory: "+ parentDir.getAbsolutePath());
        }

        Yaml yaml = new Yaml();
        try (Writer writer = fileWriter(oktaPropsFile)){
            yaml.dump(rootProps, writer);
        }
    }

    private Writer fileWriter(File file) throws FileNotFoundException {
        return new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
    }

    private Reader fileReader(File file) throws FileNotFoundException {
        return new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
    }
}
