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
package com.okta.cli.common.model;

import lombok.Data;

import java.util.Comparator;

/**
 * A basic Semver class that represents a typical Maven style version {@code MAJOR.MINOR.PATCH[-SNAPSHOT][-QUALIFIER]} that is
 * mapped to a Semver, where the optional {@code [-SNAPSHOT][-QUALIFIER]} is considered buildMetadata.
 *
 * This distinction is subtle, but _true_ semver is {@code MAJOR.MINOR.PATCH[-pre-release][+buildMetadata]}, the differences is the '{@code +}'.
 */
@Data
public class Semver implements Comparable<Semver> {

    private final String version;

    private final Integer major;
    private final Integer minor;
    private final Integer patch;
    private final String buildMetadata;
    private final boolean releaseBuild;

   private Semver(String version, Integer major, Integer minor, Integer patch, String buildMetadata, boolean releaseBuild) {
        this.version = version;
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.buildMetadata = buildMetadata;
        this.releaseBuild = releaseBuild;
    }

    @Override
    public int compareTo(Semver other) {
        return Comparator.comparing(Semver::getMajor, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Semver::getMinor, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Semver::getPatch, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Semver::isReleaseBuild, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Semver::getVersion, Comparator.nullsLast(Comparator.naturalOrder()))
                .compare(this, other);
    }

    public boolean isGreaterThan(String otherVersion) {
        return this.isGreaterThan(Semver.parse(otherVersion));
    }

    public boolean isGreaterThan(Semver other) {
        return this.compareTo(other) > 0;
    }

    public static Semver parse(String version) {

        // overly simple semver parser, split on '.', then grab the optional '-qualifier' from the last segment
        // Using the "official" semver regex works, but it has a risk of a REDOS attack, i.e. it's really slow
        String[] parts = version.split("\\.", 3);
        String major = null;
        String minor = null;
        String patch = null;
        boolean release = true;
        String meta = null;

        if (parts.length > 0) {
            // split on the qualifier i.e. 1.2.[3-gitSha], or 1.2.3-SNAPSHOT-gitSha
            String[] lastAndQual = parts[parts.length - 1].split("-", 2);

            // qualifier
            if (lastAndQual.length == 2) {
                meta = lastAndQual[1];
            }

            // update the last part to just the simple value without the qualifier
            parts[parts.length - 1] = lastAndQual[0];

            major = parts[0];
            if (parts.length > 1) {
                minor = parts[1];
            }

            if (parts.length > 2) {
                patch = parts[2];
            }

            if (version.contains("-SNAPSHOT")) {
                release = false;
            }
        }
        return new Semver(version, toInt(major), toInt(minor), toInt(patch), meta, release);
    }

    private static Integer toInt(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
