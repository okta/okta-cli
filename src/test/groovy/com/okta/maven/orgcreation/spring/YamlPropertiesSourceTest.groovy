/*
 * Copyright 2019-Present Okta, Inc.
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
package com.okta.maven.orgcreation.spring

import com.okta.maven.orgcreation.test.TestUtil
import org.testng.annotations.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.nullValue

class YamlPropertiesSourceTest {

    @Test
    void readAndWriteConfig() {

        File configFile = TestUtil.writeYamlToTempFile([
                spring: [
                    foo: "bar",
                    numbers: [
                        one: 1,
                        two: "two"]]
        ], "readAndWriteConfig")

        new YamlPropertiesSource(configFile).addProperties(["okta.oauth2.key-1": "one",
                                                            "okta.otherKey": "otherValue",
                                                            "top-level": "not-nested"])
        assertThat(TestUtil.readYamlFromFile(configFile), is([
                spring: [
                    foo: "bar",
                    numbers: [
                        one: 1,
                        two: "two"
                    ]
                ],
                "top-level": "not-nested",
                okta: [
                    otherKey: "otherValue",
                    oauth2: [
                        "key-1": "one"]]]))
    }

    @Test
    void missingFileTest() {

        File outputDir = File.createTempDir("missingFileTest-", "-test")
        File configFile = new File(outputDir, "missingFileTest.yaml")

        YamlPropertiesSource source = new YamlPropertiesSource(configFile)

        // lookup property expectException null value
        assertThat source.getProperty("any.value.is.null"), nullValue()

        // add props
        source.addProperties(["okta.oauth2.key-1": "one",
                              "okta.otherKey": "otherValue",
                              "top-level": "not-nested"])

        assertThat(TestUtil.readYamlFromFile(configFile), is([
                "top-level": "not-nested",
                okta: [
                    otherKey: "otherValue",
                    oauth2: [
                        "key-1": "one"]]]))
    }

    @Test
    void findCamelWithKebabTest() {

        File configFile = TestUtil.writeYamlToTempFile([
                spring: [
                    foo: "bar",
                    fooBar: "expected-value",
                    numbers: [
                        one: 1,
                        two: "two"]]
        ], "readAndWriteConfig")

        String result = new YamlPropertiesSource(configFile).getProperty("spring.foo-bar")
        assertThat result, is("expected-value")

    }
}
