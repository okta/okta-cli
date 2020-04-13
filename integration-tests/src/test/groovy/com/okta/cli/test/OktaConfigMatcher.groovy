package com.okta.cli.test

import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.yaml.snakeyaml.Yaml

class OktaConfigMatcher extends TypeSafeMatcher<File> {

    private final Map expected

    OktaConfigMatcher(String orgUrl, String token) {
        this([
            okta: [
                "client": [
                    orgUrl: orgUrl,
                    token: token ]]])
    }

    OktaConfigMatcher(Map expected) {
        this.expected = expected
    }

    @Override
    protected boolean matchesSafely(File item) {

        def matches = item != null && item.exists()
        return matches && expected == parseYaml(item)
    }

    @Override
    void describeTo(Description description) {
        description.appendText("To contain YAML equal to: " + expected)
    }

    @Override
    protected void describeMismatchSafely(File item, Description mismatchDescription) {
        mismatchDescription.appendText("File contents: " + item?.text)
    }

    private static Map parseYaml(File oktaConfigFile) {
        Yaml yaml = new Yaml()
        return yaml.load(oktaConfigFile.text)
    }
}
