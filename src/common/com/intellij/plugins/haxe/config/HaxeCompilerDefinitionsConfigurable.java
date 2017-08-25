/*
 * Copyright 2017 Eric Bishton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.config;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.ui.StringValueDialog;

import javax.swing.*;

/**
 * Created by ebishton on 8/18/17.
 */
public class HaxeCompilerDefinitionsConfigurable extends HaxeSettingsConfigurable {
  public HaxeCompilerDefinitionsConfigurable(final Project p) {
    super(new SettingsHandler(p));
  }

  private static class SettingsHandler extends HaxeSettingsHandler {
    final Project project;

    public SettingsHandler(Project project) {
      this.project = project;
    }

    @Override
    public Project getProject() {
      return project;
    }

    @Override
    public void setSettingValues(String[] values) {
      HaxeProjectSettings.getInstance(project).setUserCompilerDefinitions(values);
    }

    @Override
    public String[] getSettingValues() {
      return HaxeProjectSettings.getInstance(project).getUserCompilerDefinitions();
    }

    @Override
    public String getNewSetting(JPanel parent, DefaultListModel unused) {
      StringValueDialog dialog = new StringValueDialog(parent, false);
      dialog.show();
      if (!dialog.isOK()) {
        return null;
      }
      return dialog.getStringValue();
    }

    @Override
    public String getDialogContextTitle() {
      return HaxeBundle.message("haxe.conditional.compilation.defined.macros");
    }
  }
}
