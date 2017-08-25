/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.NonDefaultProjectConfigurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.ui.HaxeSettingsForm;
import com.intellij.plugins.haxe.util.HaxeUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeSettingsConfigurable implements SearchableConfigurable, NonDefaultProjectConfigurable {
  private HaxeSettingsForm mySettingsPane;
  private final HaxeSettingsHandler mySettingsHandler;

  public HaxeSettingsConfigurable(HaxeSettingsHandler handler) {
    mySettingsHandler = handler;
  }

  public String getDisplayName() {
    return HaxeBundle.message("haxe.settings.name");
  }

  @NotNull
  public String getId() {
    return "haxe.settings";
  }

  public String getHelpTopic() {
    return null;
  }

  public JComponent createComponent() {
    if (mySettingsPane == null) {
      mySettingsPane = new HaxeSettingsForm(mySettingsHandler);
    }
    reset();
    return mySettingsPane.getPanel();
  }

  public boolean isModified() {
    return mySettingsPane != null && mySettingsPane.isModified();
  }

  public void apply() throws ConfigurationException {
    if (mySettingsPane != null) {
      final boolean modified = isModified();
      mySettingsPane.applyEditorToSettings();
      if (modified) {
        HaxeUtil.reparseProjectFiles(mySettingsHandler.getProject());
      }
    }
  }

  public void reset() {
    if (mySettingsPane != null) {
      mySettingsPane.resetEditorFromSettings();
    }
  }

  public void disposeUIResources() {
    mySettingsPane = null;
  }

  public Runnable enableSearch(String option) {
    return null;
  }

}
