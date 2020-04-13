package com.okta.cli.common.service

import com.okta.cli.common.TestUtil
import com.okta.cli.common.config.EnvFilePropertiesSource
import com.okta.cli.common.config.PropertiesFilePropertiesSource
import com.okta.cli.common.config.YamlPropertiesSource
import org.testng.annotations.Test

import java.nio.file.Files

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.instanceOf

class ConfigFileLocatorServiceTest {

    @Test
    void springYamlFileExists() {
        File parentDir = tempDir
        File target = new File(parentDir, "src/main/resources/application.yml")
        target.getParentFile().mkdirs()
        target.createNewFile()

        new ConfigFileLocatorService().findApplicationConfig(parentDir, null)
        assertThat new ConfigFileLocatorService().findApplicationConfig(parentDir, null), instanceOf(YamlPropertiesSource)
    }

    @Test
    void springPropertiesFileExists() {
        File parentDir = tempDir
        File target = new File(parentDir, "src/main/resources/application.properties")
        target.getParentFile().mkdirs()
        target.createNewFile()

        new ConfigFileLocatorService().findApplicationConfig(parentDir, null)
        assertThat new ConfigFileLocatorService().findApplicationConfig(parentDir, null), instanceOf(PropertiesFilePropertiesSource)
    }

    @Test
    void bothSpringConfigFiles() {
        File parentFile = tempDir
        File target1 = new File(parentFile, "src/main/resources/application.properties")
        File target2 = new File(parentFile, "src/main/resources/application.yml")
        target1.getParentFile().mkdirs()
        target1.createNewFile()
        target2.createNewFile()

        new ConfigFileLocatorService().findApplicationConfig(parentFile, null)
        assertThat new ConfigFileLocatorService().findApplicationConfig(parentFile, null), instanceOf(PropertiesFilePropertiesSource)
    }

    @Test
    void ymlFile() {
        assertThat new ConfigFileLocatorService().findApplicationConfig(null, new File(tempDir,"foo.yml")), instanceOf(YamlPropertiesSource)
    }

    @Test
    void propertiesFile() {
        assertThat new ConfigFileLocatorService().findApplicationConfig(null, new File(tempDir,"foo.properties")), instanceOf(PropertiesFilePropertiesSource)
    }

    @Test
    void envFile() {
        assertThat new ConfigFileLocatorService().findApplicationConfig(null, new File(tempDir,"foo.env")), instanceOf(EnvFilePropertiesSource)
    }

    @Test
    void otherFileType() {
        TestUtil.expectException IllegalArgumentException, { new ConfigFileLocatorService().findApplicationConfig(null, new File(tempDir,"foo.txt")) }
    }

    File getTempDir() {
        return Files.createTempDirectory(getClass().simpleName + "-").toFile()
    }
}
