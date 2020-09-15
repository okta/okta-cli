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
package com.okta.cli.common.service;

import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;

import java.util.Map;

public class DefaultInterpolator implements Interpolator {

    @Override
    public String interpolate(String text, Map<String, String> context) {

        StringSearchInterpolator interpolator = new StringSearchInterpolator();
        interpolator.addValueSource(new MapBasedValueSource(context));
        try {
            return interpolator.interpolate(text);
        } catch (InterpolationException e) {
            throw new RuntimeException("Failed to filter content: " , e);
        }
    }
}
