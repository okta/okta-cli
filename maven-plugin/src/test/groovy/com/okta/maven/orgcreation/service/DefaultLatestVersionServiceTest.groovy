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

import org.apache.maven.artifact.Artifact
import org.apache.maven.artifact.DefaultArtifact
import org.apache.maven.artifact.metadata.ArtifactMetadataSource
import org.apache.maven.artifact.repository.ArtifactRepository
import org.apache.maven.artifact.versioning.ArtifactVersion
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.hamcrest.Matchers
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

import java.util.stream.Collectors

import static org.hamcrest.MatcherAssert.assertThat
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class DefaultLatestVersionServiceTest {

    @Test(dataProvider = "inputVersions")
    void latestVersionTest(String expectedVersion, Collection<String> metadataVersions) {

        String groupId = "gid"
        String artifactId = "aid"
        String defaultVersion = "dv"
        Artifact artifact = new DefaultArtifact(groupId, artifactId, defaultVersion, "compile", "jar", "", null)

        ArtifactRepository localRepository = mock(ArtifactRepository)
        List<ArtifactRepository> remoteArtifactRepositories = Collections.emptyList()

        ArtifactMetadataSource artifactMetadataSource = mock(ArtifactMetadataSource)
        when(artifactMetadataSource.retrieveAvailableVersions(artifact, localRepository, remoteArtifactRepositories)).thenReturn(
                metadataVersions.stream().map { new DefaultArtifactVersion(it) }.collect(Collectors.toList())
        )

        DefaultLatestVersionService latestVersionService = new DefaultLatestVersionService(artifactMetadataSource)
        ArtifactVersion result = latestVersionService.getLatestVersion(groupId, artifactId, defaultVersion, localRepository, remoteArtifactRepositories)
        assertThat result.toString(), Matchers.is(expectedVersion)
    }

    @DataProvider
    Object[][] inputVersions() {
        return [
                // expected version, metadata versions
                ["2.0", ["1.2.3", "1.2.0-SNAPSHOT", "2.0", "2.0-SNAPSHOT", "1.0"]], // basic test versions are out of order
                ["dv", ["1.2.0-SNAPSHOT", "0-SNAPSHOT", "abc-SNAPSHOT"]], // expect the default version to be used because only snapshots are returned
                ["dv", []], // empty metadata
        ]
    }

}
