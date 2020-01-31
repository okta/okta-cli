/*
 * Copyright 2020-Present Okta, Inc, Inc.
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
package com.okta.maven.orgcreation.service

import org.apache.maven.project.MavenProject
import org.apache.maven.shared.release.transform.jdom.JDomModelETLFactory
import org.codehaus.plexus.util.FileUtils
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is

class DefaultDependencyAddServiceTest {

    @Test(dataProvider = "testPomNames")
    void updatePomTest(String name) {

        // get project test file and expected result
        File testPom = new File(getClass().getResource("/test-poms/${name}.xml").getFile())
        File expectedPom = new File(getClass().getResource("/test-poms/${name}-expected.xml").getFile())
        Path tempPom = Files.createTempFile(getClass().getSimpleName(), '-${name}.xml')
        Files.copy(testPom.toPath(), tempPom, StandardCopyOption.REPLACE_EXISTING)

        DependencyAddService dependencyAddService = new DefaultDependencyAddService(new JDomModelETLFactory())
        MavenProject project = new MavenProject()
        project.setFile(tempPom.toFile())

        dependencyAddService.addDependencyToPom("com.okta.spring", "okta-spring-boot-starter", "1.2.3", project)

        String actual = FileUtils.fileRead(tempPom.toFile())
        String expected = FileUtils.fileRead(expectedPom)
        assertThat actual, is(expected)
    }

    @DataProvider
    Object[][] testPomNames() {
        return [
                ["tabs"],
                ["spaces"],
                ["no-deps"], // missing dependency block
                ["tabs-no-deps"], // when there is no dependency block spaces will be used
                ["ugly"], // ugly spacing
        ]
    }
}
