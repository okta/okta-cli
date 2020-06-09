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
package com.okta.maven.orgcreation.service;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.repository.legacy.metadata.ArtifactMetadataRetrievalException;
import org.codehaus.plexus.component.annotations.Component;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@Component(role = LatestVersionService.class)
public class DefaultLatestVersionService implements LatestVersionService {

    private final ArtifactMetadataSource artifactMetadataSource;

    @Inject
    public DefaultLatestVersionService(ArtifactMetadataSource artifactMetadataSource) {
        this.artifactMetadataSource = artifactMetadataSource;
    }

    public ArtifactVersion getLatestVersion(String groupId, String artifactId, String defaultVersion, ArtifactRepository localRepository, List<ArtifactRepository> remoteArtifactRepositories) throws ArtifactMetadataRetrievalException {
        Artifact artifact = new DefaultArtifact(groupId, artifactId, defaultVersion, "compile", "jar", "", null);

        // get all versions
        final List<ArtifactVersion> versions = artifactMetadataSource.retrieveAvailableVersions(artifact, localRepository, remoteArtifactRepositories);
        // filter out snapshots
        versions.removeIf(artifactVersion -> ArtifactUtils.isSnapshot(artifactVersion.toString()));

        // if empty just return the defaultVersion
        if (versions.isEmpty()) {
            return new DefaultArtifactVersion(defaultVersion);
        }

        // get the latest
        return Collections.max(versions);
    }
}