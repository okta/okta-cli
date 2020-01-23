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

import com.okta.maven.orgcreation.support.SuppressFBWarnings;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.config.ModelloReleaseDescriptor;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.transform.ModelETL;
import org.apache.maven.shared.release.transform.ModelETLFactory;
import org.apache.maven.shared.release.transform.ModelETLRequest;
import org.apache.maven.shared.release.transform.jdom.JDomModelETLFactory;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.component.annotations.Component;
import org.jdom.Content;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Text;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;

@Component(role = DependencyAddService.class)
public class DefaultDependencyAddService implements DependencyAddService {

    private static final Namespace NAMESPACE = Namespace.getNamespace("http://maven.apache.org/POM/4.0.0");

    private final ModelETLFactory modelETLFactory;

    @Inject
    public DefaultDependencyAddService(@Named(JDomModelETLFactory.ROLE_HINT) ModelETLFactory modelETLFactory) {
        this.modelETLFactory = modelETLFactory;
    }

    @Override
    public void addDependencyToPom(String groupId, String artifactId, String version, MavenProject project) throws PomUpdateException {
        File pomFile = ReleaseUtil.getStandardPom(project);

        // these classes are from the maven-release-plugin, it does 80% of what we need
        ModelETLRequest request = new ModelETLRequest();
        request.setLineSeparator(ReleaseUtil.LS);
        request.setProject(project);
        request.setReleaseDescriptor(new BuilderReleaseDescriptor());

        try {
            ModelETL etl = modelETLFactory.newInstance(request);
            etl.extract(pomFile);
            updatePom(groupId, artifactId, version, etl.getModel());
            etl.load(pomFile);
        } catch (ReleaseExecutionException e) {
            throw new PomUpdateException("Failed to update pom.xml", e);
        }
    }

    private void updatePom(String groupId, String artifactId, String version, Model modelTarget) throws PomUpdateException {
        // ugly, but the release plugin jdom wrapper doesn't allow adding new dependency blocks
        Element modelBaseElement = reflect(reflect(modelTarget, "modelBase"), "modelBase");
        Element dependencies = modelBaseElement.getChild("dependencies", Namespace.getNamespace("http://maven.apache.org/POM/4.0.0"));

        // if there is no dependencies block, add one
        if (dependencies == null) {
            dependencies = element("dependencies");
            dependencies.addContent("\n    ");
            modelBaseElement.addContent("    ");
            modelBaseElement.addContent(dependencies);
            modelBaseElement.addContent("\n");
        }

        // attempt to use the same white space as previous dependency blocks
        String baseSpacingText = findSpacingBeforeElement(dependencies, "    ");

        // JDOM allows for preserving whitespace
        Element newDepElement = dependency(groupId, artifactId, version, baseSpacingText);
        dependencies.addContent(baseSpacingText);
        dependencies.addContent(newDepElement); // add the dep block
        dependencies.addContent("\n" + baseSpacingText);
    }

    private Element dependency(String groupId, String artifactId, String version, String ws) {
        Element newDepElement = element("dependency");
        Element groupIdElement = element("groupId", groupId);
        Element artifactIdElement = element("artifactId", artifactId);
        Element versionElement = element("version", version);

        newDepElement.addContent("\n" + ws + ws + ws);
        newDepElement.addContent(groupIdElement);
        newDepElement.addContent("\n" + ws + ws + ws);
        newDepElement.addContent(artifactIdElement);
        newDepElement.addContent("\n" + ws + ws + ws);
        newDepElement.addContent(versionElement);
        newDepElement.addContent("\n" + ws + ws);

        return newDepElement;
    }

    private Element element(String name, String value) {
        Element element = element(name);
        element.setText(value);
        return element;
    }

    private Element element(String name) {
        return new Element(name , NAMESPACE);
    }

    private String stripNewLines(String input) {
        return input.replace("\r", "")
                .replace("\n", "");
    }

    private String previousTextElement(OptionalInt optionalPos, Element element, String defaultValue) {
        if (optionalPos.isPresent()) {
            int pos = optionalPos.getAsInt();
            if (pos > 0) {
                Content previousContent = element.getContent(pos - 1);
                if (previousContent instanceof Text) {
                    Text spacing = (Text) previousContent;
                    return spacing.getText();
                }
            }
        }
        return defaultValue;
    }

    private String findSpacingBeforeElement(Element element, String defaultValue) {
        if (element == null) {
            return defaultValue;
        }

        Element parent = element.getParentElement();
        List parentContent = parent.getContent();
        OptionalInt firstElementPos = IntStream.range(0, parentContent.size())
                .filter(index -> element.equals(parentContent.get(index)))
                .findFirst();

        return stripNewLines(previousTextElement(firstElementPos, parent, defaultValue));
    }

    private <T> T reflect(Object obj, String fieldName) throws PomUpdateException {
        try {
            final Field field = obj.getClass().getDeclaredField(fieldName);
            AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                field.setAccessible(true);
                return null;
            });

            return (T) field.get(obj);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new PomUpdateException("Failed locate field '" + fieldName + "' on class '" + obj.getClass() + "' an incompatible version of 'org.apache.maven.release:maven-release-manager' might be on the classpath.", e);
        }
    }

    @SuppressFBWarnings("SE_NO_SERIALVERSIONID")
    public static final class BuilderReleaseDescriptor extends ModelloReleaseDescriptor implements ReleaseDescriptor {
        private BuilderReleaseDescriptor() {}
    }
}