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
package com.okta.maven.orgcreation.spring

import org.hamcrest.MatcherAssert
import org.testng.annotations.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.both
import static org.hamcrest.Matchers.endsWith
import static org.hamcrest.Matchers.hasEntry
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.nullValue
import static org.hamcrest.Matchers.startsWith

class EnvFilePropertiesSourceTest {

    @Test
    void readAndWriteConfig() {

        File configFile = writeFile([
                "SPRING_FOO": "bar",
                "SPRING_NUMBERS_ONE": "1",
                "SPRING_NUMBERS_TWO": "two",
                "SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_OIDC_CLIENT_ID":"a-client-id"],
            "readAndWriteConfig")

        new EnvFilePropertiesSource(configFile).addProperties(["okta.oauth2.key-1": "one",
                                                               "okta.other.key": "otherValue",
                                                               "top-level": "not-nested",
                                                               "spring.security.oauth2.client.registration.oidc.client-secret": "a client secret"])
        assertThat(readFromFile(configFile), is([
                "SPRING_FOO": "bar",
                "SPRING_NUMBERS_ONE": "1",
                "SPRING_NUMBERS_TWO": "two",
                "TOP_LEVEL": "not-nested",
                "OKTA_OTHER_KEY": "otherValue",
                "OKTA_OAUTH2_KEY_1": "one",
                "SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_OIDC_CLIENT_ID": "a-client-id",
                "SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_OIDC_CLIENT_SECRET": "a client secret"]))
    }

    @Test
    void missingFileTest() {

        File outputDir = File.createTempDir("missingFileTest-", "-test")
        File configFile = new File(outputDir, "missingFileTest.properties")

        EnvFilePropertiesSource source = new EnvFilePropertiesSource(configFile)

        // lookup property expectException null value
        assertThat source.getProperty("ANY_VALUE_IS_NULL"), nullValue()

        // add props
        source.addProperties(["okta.oauth2.key-1": "one",
                              "okta.other.key": "otherValue",
                              "top-level": "not-nested"])

        assertThat(readFromFile(configFile), is([
                "TOP_LEVEL": "not-nested",
                "OKTA_OTHER_KEY": "otherValue",
                "OKTA_OAUTH2_KEY_1": "one"]))
    }

    @Test
    void findCamelWithKebabTest() {

        File configFile = writeFile([
                "SPRING_FOO": "bar",
                "SPRING_FOO_BAR": "expected-value",
                "SPRING_NUMBERS_ONE": "1",
                "SPRING_NUMBERS_TWO": "two"],
            "readAndWriteConfig")

        def result = new EnvFilePropertiesSource(configFile).getProperties()
        MatcherAssert.assertThat result, hasEntry("SPRING_FOO_BAR", "expected-value")
    }

    static File writeFile(Map data, String testName) {
        File tempFile = File.createTempFile(testName, "test.env")
        tempFile.withWriter {

            data.entrySet().each {entry ->
                it.writeLine("export " + entry.key + "=\"" + entry.value +"\"")
            }
        }
        return tempFile
    }

    static Map readFromFile(File configFile) {
        Map<String, String> result = new LinkedHashMap<>()
        configFile.withReader {
            it.lines().each { line ->
                assertThat(line, startsWith("export "))
                def keyValue = line.substring(7).split("=") // strip export and then split
                assertThat(keyValue[1], both(startsWith('"')).and(endsWith('"')))
                result.put(keyValue[0], keyValue[1].substring(1, keyValue[1].length() - 1))
            }
        }
        return result
    }
}
