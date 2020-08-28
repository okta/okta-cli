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

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.jdk.Package_jdk_internal_reflect;

import java.lang.reflect.Constructor;

/**
 * Work around for a build time issue, see: <p>
 * https://github.com/quarkusio/quarkus/issues/7422 <p>
 * https://github.com/oracle/graal/pull/2194
 *
 * TODO: Next time we update GraalVM attempt to remove this class
 */
@TargetClass(classNameProvider = Package_jdk_internal_reflect.class, className = "ReflectionFactory")
public final class Target_jdk_internal_reflect_ReflectionFactory {

    @Substitute
    private Constructor<?> generateConstructor(Class<?> cl, Constructor<?> constructorToCall) {
        return null;
    }

}