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
package com.intellij.plugins.haxe.util;

import com.intellij.openapi.util.text.CharFilter;
import com.intellij.openapi.util.text.StringUtil;

/**
 * Created by ebishton on 8/19/17.
 */
public class HaxePackageUtil {
  public static CharFilter NOT_PACKAGE_NAME_FILTER = new CharFilter() {
    @Override
    public boolean accept(char ch) {
      return !isPackageNameChar(ch);
    }
  };

  public static boolean isPackageNameChar(char ch) {
    return ('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z') || ('0' <= ch && ch <= '9') || ch == '_';
  }

  public static boolean isValidFirstPackageNameChar(char ch) {
    return Character.isLowerCase(ch) || ch == '_';
  }

  public static boolean packageNameIsValid(String name) {
    // XXX: Should we validate package paths as well???
    boolean valid = null != name && !name.isEmpty();
    valid = valid && -1 == StringUtil.findFirst(name, NOT_PACKAGE_NAME_FILTER);
    valid = valid && isValidFirstPackageNameChar(name.charAt(0));
    return valid;
  }
}
