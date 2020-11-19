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
package com.okta.cli.common.config

import org.hamcrest.MatcherAssert
import org.testng.annotations.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.nullValue

class PropertiesFilePropertiesSourceTest {

    @Test
    void readAndWriteConfig() {

        File configFile = writeFile([
                "spring.foo": "bar",
                "spring.numbers.one": "1",
                "spring.numbers.two": "two"],
            "readAndWriteConfig")

        new PropertiesFilePropertiesSource(configFile).addProperties(["okta.oauth2.key-1": "one",
                                                                      "okta.otherKey"    : "otherValue",
                                                                      "top-level"        : "not-nested"])
        assertThat(readFromFile(configFile), is([
                "spring.foo": "bar",
                "spring.numbers.one": "1",
                "spring.numbers.two": "two",
                "top-level": "not-nested",
                "okta.otherKey": "otherValue",
                "okta.oauth2.key-1": "one"]))
    }

    @Test
    void missingFileTest() {

        File outputDir = File.createTempDir("missingFileTest-", "-test")
        File configFile = new File(outputDir, "missingFileTest.properties")

        PropertiesFilePropertiesSource source = new PropertiesFilePropertiesSource(configFile)

        // lookup property expectException null value
        assertThat source.getProperty("any.value.is.null"), nullValue()

        // add props
        source.addProperties(["okta.oauth2.key-1": "one",
                              "okta.otherKey": "otherValue",
                              "top-level": "not-nested"])

        assertThat(readFromFile(configFile), is([
                "top-level": "not-nested",
                "okta.otherKey": "otherValue",
                "okta.oauth2.key-1": "one"]))
    }

    @Test
    void findCamelWithKebabTest() {

        File configFile = writeFile([
                "spring.foo": "bar",
                "spring.fooBar": "expected-value",
                "spring.numbers.one": "1",
                "spring.numbers.two": "two"],
            "readAndWriteConfig")

        String result = new PropertiesFilePropertiesSource(configFile).getProperty("spring.foo-bar")
        MatcherAssert.assertThat result, is("expected-value")
    }

    @Test
    void addPropertiesWithNull() {
        File configFile = writeFile([
                "spring.foo": "bar",
                "spring.fooBar": "expected-value",
                "spring.numbers.one": "1",
                "spring.numbers.two": "two"],
                "addPropertiesWithNull")

        Map<String, String> newProps = [
                "spring.foo": "bar-new",
                "null-value": null,
                "a-new-key": "a-new-value"
        ]

        Map<String, String> expectedMergedResult = [
                "spring.foo": "bar-new",
                "spring.fooBar": "expected-value",
                "spring.numbers.one": "1",
                "spring.numbers.two": "two",
                "a-new-key": "a-new-value"
        ]

        new PropertiesFilePropertiesSource(configFile).addProperties(newProps)
        assertThat readFromFile(configFile), is(expectedMergedResult)
    }

    static File writeFile(Map data, String testName) {
        File tempFile = File.createTempFile(testName, "test.properties")
        tempFile.withWriter {
            Properties properties = new Properties()
            properties.putAll(data)
            properties.store(it, null)
        }
        return tempFile
    }

    static Map readFromFile(File configFile) {
        configFile.withReader {
            Properties result = new Properties()
            result.load(it)
            return result
        }
    }
}
