package com.okta.maven.orgcreation;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.okta.commons.lang.Strings;
import com.okta.maven.orgcreation.models.Organization;
import com.okta.maven.orgcreation.models.OrganizationRequest;
import com.okta.sdk.client.Client;
import com.okta.sdk.client.Clients;
import com.okta.sdk.impl.client.DefaultClientBuilder;
import com.okta.sdk.impl.config.ClientConfiguration;
import com.okta.sdk.resource.ExtensibleResource;
import com.okta.sdk.resource.application.Application;
import com.okta.sdk.resource.application.ApplicationCredentialsOAuthClient;
import com.okta.sdk.resource.application.ApplicationGroupAssignment;
import com.okta.sdk.resource.application.OAuthApplicationCredentials;
import com.okta.sdk.resource.application.OAuthEndpointAuthenticationMethod;
import com.okta.sdk.resource.application.OAuthGrantType;
import com.okta.sdk.resource.application.OAuthResponseType;
import com.okta.sdk.resource.application.OpenIdConnectApplication;
import com.okta.sdk.resource.application.OpenIdConnectApplicationSettings;
import com.okta.sdk.resource.application.OpenIdConnectApplicationSettingsClient;
import com.okta.sdk.resource.application.OpenIdConnectApplicationType;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mojo(name = "createOrg", defaultPhase = LifecyclePhase.NONE, threadSafe = false, aggregator = true, requiresProject=false)
public class OrgCreationMojo extends AbstractMojo {

    @Parameter(property = "email", required = true)
    private String email;

    @Parameter(property = "firstName", required = true)
    private String firstName;

    @Parameter(property = "lastName", required = true)
    private String lastName;

    @Parameter(property = "applicationYaml", defaultValue = "${project.basedir}/src/main/resources/application.yml")
    private File applicationYaml;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        // check if okta client config exists?
        ClientConfiguration clientConfiguration;

        try {
            Field field = DefaultClientBuilder.class.getDeclaredField("clientConfig");
            field.setAccessible(true);
            clientConfiguration = (ClientConfiguration) field.get(new DefaultClientBuilder());

            String orgUrl;

            if (StringUtils.isEmpty(clientConfiguration.getBaseUrl())) {
                getLog().info("Current OrgUrl is empty, creating new org...");

                Organization newOrg = createNewOrg();

                orgUrl = "https://" + newOrg.getSubdomain() + ".oktapreview.com";

                getLog().info("OrgUrl: "+ orgUrl);
                getLog().info("Token: " + newOrg.getApiToken());

                // write ~/.okta/okta.yaml
                writeOktaYaml(newOrg, orgUrl);

            } else {
                orgUrl = clientConfiguration.getBaseUrl();
                getLog().info("Current OrgUrl: " + clientConfiguration.getBaseUrl());
            }





            Map<String, Object> springProps = new LinkedHashMap<>();
            Yaml springAppYaml = new Yaml(yamlOptions());
            if (applicationYaml.exists()) {
                springProps = (Map<String, Object>) springAppYaml.loadAs(new FileReader(applicationYaml), Map.class);
            }

            Map<String, Object> oktaProps = (Map<String, Object>) springProps.getOrDefault("okta", new HashMap<>());
            Map<String, Object> oauth2Props = (Map<String, Object>) oktaProps.getOrDefault("oauth2", new HashMap<>());
            if (Strings.isEmpty(oauth2Props.get("issuer"))) {

                // create ODIC application
                Client client = Clients.builder().build();

                Application app = client.instantiate(OpenIdConnectApplication.class)
                        .setLabel("app+" + UUID.randomUUID().toString())
                        .setSettings(client.instantiate(OpenIdConnectApplicationSettings.class)
                            .setOAuthClient(client.instantiate(OpenIdConnectApplicationSettingsClient.class)
        //                        .setClientUri("https://example.com/client")
                                .setRedirectUris(Arrays.asList("http://localhost:8080/authorization-code/callback"))
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


                springAppYaml.dump(springProps, new FileWriter(applicationYaml));
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

    private Organization createNewOrg() throws IOException {
        // create org
        String createOrgUrl = "https://oktane19-devrel-booth.herokuapp.com/api/org";
        String createOrgAdminUrl = "https://oktane19-devrel-booth.herokuapp.com/api/passwd";


        HttpClient httpClient = HttpClients.createDefault();
        HttpPost post = new HttpPost(createOrgUrl);


        ObjectMapper objectMapper = new ObjectMapper();
        String postBody = objectMapper.writeValueAsString(new OrganizationRequest(email, firstName, lastName));
        post.setEntity(new StringEntity(postBody, StandardCharsets.UTF_8));
        post.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        HttpResponse response = httpClient.execute(post);


        Organization newOrg = objectMapper.reader().readValue(new JsonFactory().createParser(response.getEntity().getContent()), Organization.class);


        // create an admin user

        String password = "123456aA$";
        newOrg.setAdminEmail(email);
        newOrg.setAdminPassword(password);

        HttpPost postForAdmin = new HttpPost(createOrgAdminUrl);
        String postBodyForAdmin = objectMapper.writeValueAsString(newOrg);
        postForAdmin.setEntity(new StringEntity(postBodyForAdmin, StandardCharsets.UTF_8));
        postForAdmin.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        HttpResponse responseForAdmin = httpClient.execute(postForAdmin);

        Organization updatedOrg = objectMapper.reader().readValue(new JsonFactory().createParser(responseForAdmin.getEntity().getContent()), Organization.class);

        getLog().info("Created admin user: "+ updatedOrg.getAdminEmail() + " with password: "+ updatedOrg.getAdminPassword());


        return updatedOrg;
    }

    private void writeOktaYaml(Organization organization, String orgUrl) throws IOException {
        Map<String, Object> rootProps = new HashMap<>();
        Map<String, Object> rootOktaProps = new HashMap<>();
        Map<String, Object> clientOktaProps = new HashMap<>();

        rootProps.put("okta", rootOktaProps);
        rootOktaProps.put("client", clientOktaProps);
        clientOktaProps.put("orgUrl", orgUrl);
        clientOktaProps.put("token", organization.getApiToken());

        File oktaPropsFile = new File(System.getProperty("user.home"), ".okta/okta.yaml");
        oktaPropsFile.getParentFile().mkdirs();

        Yaml yaml = new Yaml();
        yaml.dump(rootProps, new FileWriter(oktaPropsFile));
    }
}
