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
package com.okta.cli.graal;

import java.io.ObjectStreamClass;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(java.io.ObjectStreamClass.class)
@SuppressWarnings({ "unused" })
final class Target_java_io_ObjectStreamClass {

    @Substitute
    private static ObjectStreamClass lookup(Class<?> cl, boolean all) {
        throw new UnsupportedOperationException("Serialization of class definitions not supported");
    }

    private Target_java_io_ObjectStreamClass(final Class<?> cl) {
        throw new UnsupportedOperationException("Serialization of class definitions not supported");
    }

    private Target_java_io_ObjectStreamClass() {
        throw new UnsupportedOperationException("Not supported");
    }

}