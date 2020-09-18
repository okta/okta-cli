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
package com.okta.cli.graalvm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.ClassPath;
import com.okta.commons.lang.Classes;
import com.okta.sdk.client.Client;
import com.okta.sdk.impl.ds.DiscriminatorConfig;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SdkResourceConfigGenerator {

    public static void main(String[] args) throws Exception {

        File destFile = new File("src/main/graalvm/okta-sdk-reflect-config.json");
        System.out.println("Writing reflect-config file: " + destFile.getAbsolutePath());
        List<Map<String, Object>> resourceConfig = new ArrayList<>();

        Method method = DiscriminatorConfig.class.getDeclaredMethod("loadConfig");
        method.setAccessible(true);
        DiscriminatorConfig config = (DiscriminatorConfig) method.invoke(null);

        config.getConfig().entrySet().forEach(entry -> {
            resourceConfig.add(sdkInterface(entry.getKey()));
            entry.getValue().getValues().values().forEach(it -> {
                resourceConfig.add(sdkInterface(it));
            });
        });

        ClassPath.from(Client.class.getClassLoader()).getTopLevelClassesRecursive("com.okta.sdk.impl.resource").stream()
                .map(ClassPath.ClassInfo::load)
                .filter(it -> !it.isInterface())
                .filter(it -> it.getSimpleName().startsWith("Default"))
                .forEach(it -> {
                    resourceConfig.add(sdkClass(it));
        });

        try(FileWriter fileWriter = new FileWriter(destFile)) {
            new ObjectMapper().writer().withDefaultPrettyPrinter().writeValue(fileWriter, resourceConfig);
        }
    }

    static private Map<String, Object> sdkInterface(String klassName) {
        return sdkInterface(Classes.forName(klassName));
    }

    static private Map<String, Object> sdkInterface(Class klass) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("name", klass.getName());
        return resource;
    }

    static private Map<String, Object> sdkClass(Class klass) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("name", klass.getName());
        List<Object> methods = new ArrayList<>();
        resource.put("methods", methods);

        Arrays.stream(klass.getConstructors())
                .forEach(constructor -> {
                    Map<String, Object> constructorInfo = new LinkedHashMap<>();
                    constructorInfo.put("name", "<init>");
                    constructorInfo.put("parameterTypes", Arrays.stream(constructor.getParameterTypes()).map(Class::getName).collect(Collectors.toList()));
                    methods.add(constructorInfo);
                });
        return resource;
    }
}
