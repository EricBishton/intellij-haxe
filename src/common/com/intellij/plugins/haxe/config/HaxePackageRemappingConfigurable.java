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
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.ui.KeyValueDialog;
import com.intellij.plugins.haxe.util.HaxePackageUtil;

import javax.swing.*;

/**
 * Created by ebishton on 8/18/17.
 */
public class HaxePackageRemappingConfigurable extends HaxeSettingsConfigurable {

  public HaxePackageRemappingConfigurable(final Project p) {
    super(new SettingsHandler(p));
  }

  public static class SettingsHandler extends HaxeSettingsHandler {
    private final Project project;

    public SettingsHandler(Project project) {
      this.project = project;
    }

    @Override
    public Project getProject() {
      return project;
    }

    @Override
    public void setSettingValues(String[] values) {
      HaxeProjectSettings.getInstance(project).setPackageRemappings(values);
    }

    @Override
    public String[] getSettingValues() {
      return HaxeProjectSettings.getInstance(project).getPackageRemappings();
    }

    @Override
    public String getDialogContextTitle() {
      return HaxeBundle.message("haxe.package.remapping");
    }

    @Override
    public String getNewSetting(JPanel parent, DefaultListModel parentValues) {
      HaxePackageMap remappings = new HaxePackageMap(parentValues.toArray());

      KeyValueDialog dialog = createDialog(parent, remappings);
      dialog.show();
      if (!dialog.isOK()) {
        return null;
      }
      return interpretDialogResult(dialog);
    }

  private KeyValueDialog createDialog(JPanel panel, final HaxePackageMap remappings) {
    return new KeyValueDialog(panel, "Package Remapping", "Remap package imports:",
                              "Original (from):", null, "Target (to):", null,
                              new KeyValueDialog.Validator() {
                                @Override
                                public ValidationInfo validate(KeyValueDialog dialog) {
                                  return validateDialogResult(dialog, remappings);
                                }
                              });
  }

  private ValidationInfo validateDialogResult(KeyValueDialog dialog, HaxePackageMap remappings) {
    String key = dialog.getKey();
    if (null == key || key.isEmpty()) {
      return new ValidationInfo("Original package name must not be empty.");
    }
    if (!HaxePackageUtil.packageNameIsValid(key)) {
      return new ValidationInfo("Original package name is invalid.");
    }
    if (null != remappings.getMapFor(key)) {
      return new ValidationInfo("Original package name is already mapped.");
    }
    // TODO: Fail if the package we are remapping is used in the project?
    //       That could take a while and this is run between keystrokes.

    String value = dialog.getValue();
    if (null == value || value.isEmpty()) {
      return new ValidationInfo("Target package name must not be empty.");
    }
    if (!HaxePackageUtil.packageNameIsValid(value)) {
      return new ValidationInfo("Target package name is invalid.");
    }
    // TODO: Fail if the target package name is not included in the project.
    //       A quick library scan should do the trick.

    if (key.equals(value)) {
      return new ValidationInfo("Original and Target package names must not be identical.");
    }
    return null;
  }

  private String interpretDialogResult(KeyValueDialog dialog) {
    assert(null != dialog);
    String key = dialog.getKey();
    String value = dialog.getValue();
    try {
      HaxePackageMapEntry entry = new HaxePackageMapEntry(key, value);
      return entry.normalize();
    } catch (HaxePackageMapEntry.FormatException e) {
      ; // Swallow it.
    }
    return null;
  }

}
}
