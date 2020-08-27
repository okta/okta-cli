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