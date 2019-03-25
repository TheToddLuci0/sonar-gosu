/*
 * Sonar Gosu Plugin
 * Copyright (C) 2016-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.gosu.jacoco;

import com.google.common.collect.Lists;
import com.google.common.io.Files;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.coverage.CoverageType;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.plugins.gosu.foundation.Gosu;
import org.sonar.test.TestUtils;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JaCoCoItSensorTest {

  private File jacocoExecutionData;
  private DefaultInputFile inputFile;
  private JaCoCoConfiguration configuration;
  private PathResolver pathResolver;
  private JaCoCoItSensor sensor;

  @Before
  public void setUp() throws Exception {
    File outputDir = FileUtils.toFile(TestUtils.class.getResource("/org/sonar/plugins/gosu/jacoco/JaCoCoItSensorTests/"));
    jacocoExecutionData = new File(outputDir, "jacoco-it.exec");

    Files.copy(FileUtils.toFile(TestUtils.class.getResource("/org/sonar/plugins/gosu/jacoco/Hello.class.toCopy")),
      new File(jacocoExecutionData.getParentFile(), "Hello.class"));
    Files.copy(FileUtils.toFile(TestUtils.class.getResource("/org/sonar/plugins/gosu/jacoco/Hello$InnerClass.class.toCopy")),
      new File(jacocoExecutionData.getParentFile(), "Hello$InnerClass.class"));

    Gosu gosu = mock(Gosu.class);
    when(gosu.getBinaryDirectories()).thenReturn(Lists.newArrayList("."));

    configuration = mock(JaCoCoConfiguration.class);
    when(configuration.shouldExecuteOnProject(true)).thenReturn(true);
    when(configuration.shouldExecuteOnProject(false)).thenReturn(false);
    when(configuration.getItReportPath()).thenReturn(jacocoExecutionData.getPath());

    DefaultFileSystem fileSystem = new DefaultFileSystem(jacocoExecutionData.getParentFile());
    inputFile = TestInputFileBuilder.create("", "example/Hello.groovy").setLanguage(Gosu.KEY)
      .setType(Type.MAIN).setLines(50).build();
    fileSystem.add(inputFile);

    pathResolver = mock(PathResolver.class);
    sensor = new JaCoCoItSensor(gosu, configuration, fileSystem, pathResolver);
  }

  @Test
  public void testSensorDefinition() {
    assertThat(sensor.toString()).isEqualTo("Gosu JaCoCoItSensor");
  }

  @Test
  public void test_description() {
    DefaultSensorDescriptor defaultSensorDescriptor = new DefaultSensorDescriptor();
    sensor.describe(defaultSensorDescriptor);
    assertThat(defaultSensorDescriptor.languages()).containsOnly(Gosu.KEY);
  }

  @Test
  public void should_Execute_On_Project_only_if_exec_exists() {
    when(configuration.getItReportPath()).thenReturn("it.exec");
    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(jacocoExecutionData);
    assertThat(sensor.shouldExecuteOnProject()).isTrue();

    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(jacocoExecutionData.getParentFile());
    assertThat(sensor.shouldExecuteOnProject()).isFalse();

    File outputDir = FileUtils.toFile(TestUtils.class.getResource("./"+JaCoCoSensorTest.class.getName()));
    File fakeExecFile = new File(outputDir, "it.not.found.exec");
    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(fakeExecFile);
    assertThat(sensor.shouldExecuteOnProject()).isFalse();

    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(fakeExecFile);
    when(configuration.shouldExecuteOnProject(false)).thenReturn(true);
    assertThat(sensor.shouldExecuteOnProject()).isTrue();
  }

  @Test
  public void test_read_execution_data() {
    when(pathResolver.relativeFile(any(File.class), Matchers.endsWith(".exec"))).thenReturn(jacocoExecutionData);

    SensorContextTester context = SensorContextTester.create(new File(""));
    sensor.execute(context);

    int[] oneHitlines = {9, 10, 25};
    int[] zeroHitlines = {14, 15, 17, 21, 29, 30, 32, 33, 38, 42, 47};
    int[] conditionLines = {14, 29, 30};

    for (int zeroHitline : zeroHitlines) {
      assertThat(context.lineHits(":example/Hello.groovy", zeroHitline)).isEqualTo(0);
    }
    for (int oneHitline : oneHitlines) {
      assertThat(context.lineHits(":example/Hello.groovy", oneHitline)).isEqualTo(1);
    }
    for (int conditionLine : conditionLines) {
      assertThat(context.conditions(":example/Hello.groovy", conditionLine)).isEqualTo(2);
      assertThat(context.coveredConditions(":example/Hello.groovy", conditionLine)).isEqualTo(0);
    }
  }

}
