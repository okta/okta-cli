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
package com.okta.cli.common

import org.testng.Assert
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor

class TestUtil {

    static File writeYamlToTempFile(Map data, String testName) {
        File tempFile = File.createTempFile(testName, "test.yaml")
        return writeYamlToTempFile(data, tempFile)
    }

    static File writeYamlToTempFile(Map data, File destFile) {
        destFile.withWriter {
            new Yaml().dump(data, it)
        }
        return destFile
    }

    static Map readYamlFromFile(File configFile) {
        configFile.withReader {
            return new Yaml(new Constructor(Map.class, new LoaderOptions())).loadAs(it, Map)
        }
    }

    static Throwable expectException(Class<? extends Throwable> catchMe, Closure callMe) {
        try {
            callMe.call()
            Assert.fail("Expected ${catchMe.getName()} to be thrown.")
        } catch(e) {
            if (!e.class.isAssignableFrom(catchMe)) {
                throw e
            }
            return e
        }
    }

}
