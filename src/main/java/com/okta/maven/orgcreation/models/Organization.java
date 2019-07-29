package com.okta.maven.orgcreation.models;

public class Organization {

    private String adminEmail;
    private String adminPassword;
    private String apiToken;
    private String base;
    private String domain;
    private String id;
    private String name;
    private String subdomain;

    public String getAdminEmail() {
        return adminEmail;
    }

    public Organization setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
        return this;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public Organization setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
        return this;
    }

    public String getApiToken() {
        return apiToken;
    }

    public Organization setApiToken(String apiToken) {
        this.apiToken = apiToken;
        return this;
    }

    public String getBase() {
        return base;
    }

    public Organization setBase(String base) {
        this.base = base;
        return this;
    }

    public String getDomain() {
        return domain;
    }

    public Organization setDomain(String domain) {
        this.domain = domain;
        return this;
    }

    public String getId() {
        return id;
    }

    public Organization setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public Organization setName(String name) {
        this.name = name;
        return this;
    }

    public String getSubdomain() {
        return subdomain;
    }

    public Organization setSubdomain(String subdomain) {
        this.subdomain = subdomain;
        return this;
    }
}
