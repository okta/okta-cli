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
package com.okta.cli.common;

import java.net.URI;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class URIs {

    private URIs() {}

    public static String reverseDomain(String uri) {
        URI parsedUri = URI.create(uri);

        // if the parsed URI does NOT have a protocol, assume it's just a host name
        if (parsedUri.getScheme() == null) {
            return dnsReverse(uri);
        }

        return dnsReverse(parsedUri.getHost());
    }

    public static String baseUrlOf(String url) {
        return URI.create(url).resolve("/").toString();
    }

    private static String dnsReverse(String host) {
        String[] parts = host.split("\\.");
        String reverseDomain = IntStream.rangeClosed(1, parts.length)
                .mapToObj(i -> parts[parts.length - i])
                .collect(Collectors.joining("."));

        return reverseDomain;
    }
}
