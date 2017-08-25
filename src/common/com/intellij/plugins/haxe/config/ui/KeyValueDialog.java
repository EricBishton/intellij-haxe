/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2017 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
package com.intellij.plugins.haxe.config.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.plugins.haxe.HaxeBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Created by ebishton on 8/18/17.
 */
public class KeyValueDialog extends DialogWrapper {
  private JLabel reasonText;
  private JTextField keyField;
  private JTextField valueField;
  private JLabel keyLabel;
  private JLabel valueLabel;
  private JPanel mainPanel;

  private String originalKey;
  private String originalValue;

  private Validator validator;

  public KeyValueDialog(@NotNull JPanel parent,
                        @Nullable String title, @Nullable String reasonText,
                        @Nullable String keyLabel, @Nullable String key,
                        @Nullable String valueLabel, @Nullable String value,
                        @NotNull Validator validator) {
    super(parent, false);
    this.validator = validator;

    setTitle(title);

    this.reasonText.setText(reasonText != null ? reasonText : "");
    this.keyLabel.setText(keyLabel != null ? keyLabel : "Key");
    this.valueLabel.setText(valueLabel != null ? valueLabel : "Value");

    originalKey = key != null ? key : "";
    originalValue = value != null ? value : "";
    this.keyField.setText(originalKey);
    this.valueField.setText(originalValue);

    init();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return mainPanel;
  }

  public String getKey() {
    return keyField.getText();
  }

  public String getValue() {
    return valueField.getText();
  }

  public boolean isModified() {
    boolean modified = originalKey != getKey() || originalValue != getValue();
    return modified;
  }

  @Nullable
  @Override
  protected ValidationInfo doValidate() {
    return validator.validate(this);
  }

  public interface Validator {
    ValidationInfo validate(KeyValueDialog dialog);
  }
}
