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
package com.okta.maven.orgcreation.support;

import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class PromptUtil {

    private PromptUtil() {}

    public static String promptIfNull(Prompter prompter, boolean interactive, String currentValue, String keyName, String promptText) {

        String value = currentValue;

        if (StringUtils.isEmpty(value)) {
            if (interactive) {
                try {
                    value = prompter.prompt(promptText);
                    value = promptIfNull(prompter, interactive, value, keyName, promptText);
                }
                catch (PrompterException e) {
                    throw new RuntimeException( e.getMessage(), e );
                }
            } else {
                throw new IllegalArgumentException( "You must specify the '" + keyName + "' property either on the command line " +
                                                    "-D" + keyName + "=... or run in interactive mode" );
            }
        }
        return value;
    }

    public static boolean promptYesNo(Prompter prompter, boolean interactive, Boolean currentValue, String keyName, String promptText) {

        Boolean value = currentValue;

        if (value == null) {
            if (interactive) {
                try {
                    List<String> options = new ArrayList<>();
                    options.add("Yes");
                    options.add("No");
                    value = options.get(0).equals(prompter.prompt(promptText, options, options.get(0)));
                }
                catch (PrompterException e) {
                    throw new RuntimeException( e.getMessage(), e );
                }
            } else {
                throw new IllegalArgumentException( "You must specify the '" + keyName + "' property either on the command line " +
                                                    "-D" + keyName + "=... or run in interactive mode" );
            }
        }
        return value;
    }
}
