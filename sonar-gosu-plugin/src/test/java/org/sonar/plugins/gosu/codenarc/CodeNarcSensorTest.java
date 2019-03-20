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
package org.sonar.plugins.gosu.codenarc;

import com.google.common.collect.Lists;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.ConfigurationBridge;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.ActiveRule;
import org.sonar.plugins.gosu.GosuPlugin;
import org.sonar.plugins.gosu.foundation.Gosu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CodeNarcSensorTest {

  private ActiveRules profile;
  private CodeNarcSensor sensor;
  private Gosu gosu;
  private Settings settings;
  private DefaultFileSystem fileSystem;

  @org.junit.Rule
  public TemporaryFolder projectdir = new TemporaryFolder();

  @Before
  public void setUp() {
    File sonarhome = projectdir.newFolder("sonarhome");

    profile = mock(ActiveRules.class);
    settings = mock(Settings.class);
    when(settings.getStringArray(GosuPlugin.FILE_SUFFIXES_KEY)).thenReturn(new String[] {".groovy", "groovy"});
    fileSystem = new DefaultFileSystem(new File("."));
    fileSystem.setWorkDir(sonarhome.toPath());
    gosu = new Gosu(new ConfigurationBridge(settings));

    sensor = new CodeNarcSensor(gosu, fileSystem, profile);
  }

  @Test
  public void should_execute_on_project() {
    fileSystem.add(new TestInputFileBuilder("", "fake.groovy").setLanguage(Gosu.KEY).build());

    ActiveRules activeRules = mock(ActiveRules.class);
    when(activeRules.findByRepository(CodeNarcRulesDefinition.REPOSITORY_KEY))
      .thenReturn(Lists.newArrayList(mock(org.sonar.api.batch.rule.ActiveRule.class)));

    SensorContextTester context = SensorContextTester.create(new File(""));
    context.setFileSystem(fileSystem);
    context.setActiveRules(activeRules);

    assertThat(sensor.shouldExecuteOnProject(context)).isTrue();
  }

  @Test
  public void should_not_execute_when_no_active_rules() {
    fileSystem.add(new TestInputFileBuilder("", "fake.groovy").setLanguage(Gosu.KEY).build());

    ActiveRules activeRules = mock(ActiveRules.class);
    when(activeRules.findByRepository(CodeNarcRulesDefinition.REPOSITORY_KEY)).thenReturn(new ArrayList<>());

    SensorContextTester context = SensorContextTester.create(new File(""));
    context.setFileSystem(fileSystem);
    context.setActiveRules(activeRules);

    assertThat(sensor.shouldExecuteOnProject(context)).isFalse();
  }

  @Test
  public void should_not_execute_if_no_groovy_files() {
    SensorContextTester context = SensorContextTester.create(new File(""));
    assertThat(sensor.shouldExecuteOnProject(context)).isFalse();
  }

  @Test
  public void test_description() {
    DefaultSensorDescriptor defaultSensorDescriptor = new DefaultSensorDescriptor();
    sensor.describe(defaultSensorDescriptor);
    assertThat(defaultSensorDescriptor.languages()).containsOnly(Gosu.KEY);
  }

  @Test
  public void should_parse() {
    SensorContextTester context = SensorContextTester.create(new File(""));

    ActiveRulesBuilder activeRulesBuilder = new ActiveRulesBuilder();
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "BooleanInstantiation");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "DuplicateImport");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "EmptyCatchBlock");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "EmptyElseBlock");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "EmptyFinallyBlock");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "EmptyForStatement");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "EmptyIfStatement");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "EmptyTryBlock");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "EmptyWhileStatement");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "ImportFromSamePackage");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "ReturnFromFinallyBlock");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "StringInstantiation");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "ThrowExceptionFromFinallyBlock");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "UnnecessaryGroovyImport");
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "UnusedImport");
    context.setActiveRules(activeRulesBuilder.build());

    File report = FileUtils.toFile(getClass().getResource("parsing/sample.xml"));
    when(settings.getString(GosuPlugin.CODENARC_REPORT_PATH)).thenReturn(report.getAbsolutePath());

    DefaultFileSystem fileSystem = mockFileSystem();
    context.setFileSystem(fileSystem);

    sensor = new CodeNarcSensor(gosu, fileSystem, profile);
    sensor.execute(context);

    assertThat(context.allIssues()).hasSize(17);
  }

  private static ActiveRulesBuilder activateFakeRule(ActiveRulesBuilder activeRulesBuilder, String ruleKey) {
    return activateRule(activeRulesBuilder, ruleKey, ruleKey);
  }

  private static ActiveRulesBuilder activateRule(ActiveRulesBuilder activeRulesBuilder, String ruleKey, String internalKey) {
//    return activeRulesBuilder.create(RuleKey.of(CodeNarcRulesDefinition.REPOSITORY_KEY, ruleKey)).setInternalKey(internalKey).activate();
    return activeRulesBuilder.addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of(CodeNarcRulesDefinition.REPOSITORY_KEY, ruleKey)).setInternalKey(internalKey).build());
  }

  @Test
  public void should_parse_but_not_add_issue_if_rule_not_found() {
    SensorContextTester context = SensorContextTester.create(new File(""));

    ActiveRulesBuilder activeRulesBuilder = new ActiveRulesBuilder();
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "UnknownRule");
    context.setActiveRules(activeRulesBuilder.build());

    DefaultFileSystem fileSystem = mockFileSystem();
    context.setFileSystem(fileSystem);

    File report = FileUtils.toFile(getClass().getResource("parsing/sample.xml"));
    when(settings.getString(GosuPlugin.CODENARC_REPORT_PATH)).thenReturn(report.getAbsolutePath());

    sensor = new CodeNarcSensor(gosu, fileSystem, profile);
    sensor.execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  @Test
  public void should_parse_but_not_add_issue_if_inputFile_not_found() {
    SensorContextTester context = SensorContextTester.create(new File(""));

    ActiveRulesBuilder activeRulesBuilder = new ActiveRulesBuilder();
    activeRulesBuilder = activateFakeRule(activeRulesBuilder, "BooleanInstantiation");
    context.setActiveRules(activeRulesBuilder.build());

    File report = FileUtils.toFile(getClass().getResource("parsing/sample.xml"));
    when(settings.getString(GosuPlugin.CODENARC_REPORT_PATH)).thenReturn(report.getAbsolutePath());

    DefaultFileSystem fileSystem = new DefaultFileSystem(new File(""));
    fileSystem.add(new TestInputFileBuilder("", "unknownFile.groovy").setLanguage(Gosu.KEY).setType(Type.MAIN).build());
    context.setFileSystem(fileSystem);

    sensor = new CodeNarcSensor(gosu, fileSystem, profile);
    sensor.execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  @Test
  public void should_run_code_narc() throws IOException {
    File sonarhome = projectdir.newFolder("sonarhome");
    SensorContextTester context = SensorContextTester.create(sonarhome);

    File sample = createSampleFile(sonarhome);
    DefaultInputFile inputFile = new TestInputFileBuilder("", "sample.groovy")
      .setLanguage(Gosu.KEY)
      .setType(Type.MAIN)
      .initMetadata(new String(Files.readAllBytes(sample.toPath()), "UTF-8"))
            .build();

    ActiveRulesBuilder activeRulesBuilder = new ActiveRulesBuilder();
    activeRulesBuilder = activateRule(activeRulesBuilder, "org.codenarc.rule.basic.EmptyClassRule", "EmptyClass");
    context.setActiveRules(activeRulesBuilder.build());

    DefaultFileSystem fileSystem = new DefaultFileSystem(sonarhome);
    fileSystem.setWorkDir(sonarhome.toPath());
    fileSystem.add(inputFile);
    context.setFileSystem(fileSystem);

    ActiveRule activeRule = mock(ActiveRule.class);
    when(activeRule.getRuleKey()).thenReturn("org.codenarc.rule.basic.EmptyClassRule");
    when(profile.findByRepository(CodeNarcRulesDefinition.REPOSITORY_KEY)).thenReturn(Arrays.asList((org.sonar.api.batch.rule.ActiveRule) activeRule));
    when(settings.getString(GosuPlugin.CODENARC_REPORT_PATH)).thenReturn("");

    sensor = new CodeNarcSensor(gosu, fileSystem, profile);
    sensor.execute(context);

    assertThat(context.allIssues()).hasSize(1);
  }

  @Test
  public void should_do_nothing_when_can_not_find_report_path() {
    SensorContextTester context = SensorContextTester.create(new File(""));

    when(settings.getString(GosuPlugin.CODENARC_REPORT_PATH)).thenReturn("../missing_file.xml");

    DefaultInputFile inputFile = new TestInputFileBuilder("", "sample.gosu").setLanguage(Gosu.KEY).setType(Type.MAIN).build();

    ActiveRulesBuilder activeRulesBuilder = new ActiveRulesBuilder();
    activeRulesBuilder = activateRule(activeRulesBuilder, "org.codenarc.rule.basic.EmptyClassRule", "EmptyClass");
    context.setActiveRules(activeRulesBuilder.build());

    context.fileSystem().add(inputFile);

    sensor = new CodeNarcSensor(gosu, context.fileSystem(), profile);
    sensor.execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  @Test
  public void should_run_code_narc_with_multiple_files() throws IOException {
    File sonarhome = projectdir.newFolder("sonarhome");
    SensorContextTester context = SensorContextTester.create(sonarhome);

    File sample1 = createSampleFile(sonarhome);
    File foo = new File(sonarhome, "foo/bar/qix");
    foo.mkdirs();
    File sample2 = createSampleFile(foo);

    ActiveRulesBuilder activeRulesBuilder = new ActiveRulesBuilder();
    activeRulesBuilder = activateRule(activeRulesBuilder, "org.codenarc.rule.basic.EmptyClassRule", "EmptyClass");
    context.setActiveRules(activeRulesBuilder.build());

    DefaultFileSystem fileSystem = context.fileSystem();
    fileSystem.setWorkDir(sonarhome.toPath());
    fileSystem.add(new TestInputFileBuilder("", "sample.groovy")
      .setLanguage(Gosu.KEY)
      .setType(Type.MAIN)
      .initMetadata(new String(Files.readAllBytes(sample1.toPath()), "UTF-8"))
    .build());
    fileSystem.add(new TestInputFileBuilder("", "foo/bar/qix/sample.groovy")
      .setLanguage(Gosu.KEY)
      .setType(Type.MAIN)
      .initMetadata(new String(Files.readAllBytes(sample2.toPath()), "UTF-8"))
            .build());

    ActiveRule activeRule = mock(ActiveRule.class);
    when(activeRule.getRuleKey()).thenReturn("org.codenarc.rule.basic.EmptyClassRule");
    when(profile.findByRepository(CodeNarcRulesDefinition.REPOSITORY_KEY)).thenReturn(Arrays.asList((org.sonar.api.batch.rule.ActiveRule) activeRule));
    when(settings.getString(GosuPlugin.CODENARC_REPORT_PATH)).thenReturn("");

    sensor = new CodeNarcSensor(gosu, fileSystem, profile);
    sensor.execute(context);

    assertThat(context.allIssues()).hasSize(2);
  }

  @Test
  public void test_toString() {
    assertThat(sensor.toString()).isEqualTo("CodeNarc");
  }

  private static File createSampleFile(File sonarhome) throws FileNotFoundException {
    File sample = new File(sonarhome, "sample.groovy");
    PrintWriter pw = new PrintWriter(sample);
    pw.write("package source\nclass SourceFile1 {\n}");
    pw.close();
    return sample;
  }

  private DefaultFileSystem mockFileSystem() {
    FilePredicates fp = mock(FilePredicates.class);

    class CustomFilePredicate implements FilePredicate {

      final String fileName;

      CustomFilePredicate(String fileName) {
        this.fileName = fileName;
      }

      @Override
      public boolean apply(InputFile inputFile) {
        return true;
      }
    }

    when(fp.hasAbsolutePath(Matchers.anyString())).thenAnswer(new Answer<FilePredicate>() {
      @Override
      public FilePredicate answer(InvocationOnMock invocation) throws Throwable {
        return new CustomFilePredicate(invocation.getArgument(0, String.class));
      }
    });

    DefaultFileSystem mockfileSystem = mock(DefaultFileSystem.class);
    when(mockfileSystem.predicates()).thenReturn(fp);
    when(mockfileSystem.hasFiles(Matchers.any(FilePredicate.class))).thenReturn(true);

    Map<String, DefaultInputFile> gosuFilesByName = new HashMap<>();
    File sampleFile = FileUtils.toFile(getClass().getResource("parsing/Sample.groovy"));

    when(mockfileSystem.inputFile(any(FilePredicate.class))).thenAnswer(new Answer<InputFile>() {
      @Override
      public InputFile answer(InvocationOnMock invocation) throws Throwable {
        String fileName = invocation.getArgument(0, CustomFilePredicate.class).fileName;
        DefaultInputFile gosuFile;
        if (!gosuFilesByName.containsKey(fileName)) {
          // store gosu file as default input files
          gosuFile = new TestInputFileBuilder("", fileName)
            .setLanguage(Gosu.KEY)
            .setType(Type.MAIN)
            .initMetadata(new String(Files.readAllBytes(sampleFile.toPath()), "UTF-8"))
          .build();
          gosuFilesByName.put(fileName, gosuFile);
        }
        return gosuFilesByName.get(fileName);
      }
    });
    return mockfileSystem;
  }

}
