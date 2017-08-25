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
import com.intellij.plugins.haxe.config.ui.HaxeSettingsForm;

import javax.swing.*;

/**
 * Created by ebishton on 8/17/17.
 */
public abstract class HaxeSettingsHandler {
  public HaxeSettingsHandler() {
  }

  /** Return the project that this settings handler is currently associated with. */
  public abstract Project getProject();

  /** Set the list of settings values into the settings object */
  public abstract void setSettingValues(String[] values);

  /** Retrieve the settings values from the settings object. */
  public abstract String[] getSettingValues();

  /** Obtain a new setting from the user (usually shows a dialog).
   *  Note that the currentValues is the list as the parent dialog currently sees it,
   *  not the (saved) list that getSettingsValues returns.
   */
  public abstract String getNewSetting(JPanel parent, DefaultListModel currentValues);

  /** Returns the context string (sub-title) for our parent dialog. */
  public abstract String getDialogContextTitle();
}
