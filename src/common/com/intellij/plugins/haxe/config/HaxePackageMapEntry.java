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

import com.intellij.openapi.util.text.CharFilter;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.util.HaxePackageUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Created by ebishton on 8/18/17.
 */
public class HaxePackageMapEntry {
  public static char SEPARATOR = ':';
  public static String SEPARATOR_STRING = Character.toString(SEPARATOR);

  public String oldPackage;
  public String newPackage;

  public HaxePackageMapEntry(@NotNull String oldName, @NotNull String newName) throws FormatException {
    oldPackage = StringUtil.strip(oldName, CharFilter.NOT_WHITESPACE_FILTER);
    newPackage = StringUtil.strip(newName, CharFilter.NOT_WHITESPACE_FILTER);
    if (!HaxePackageUtil.packageNameIsValid(oldPackage) || !HaxePackageUtil.packageNameIsValid(newPackage)) {
      throw new FormatException("Mapping from '" + oldPackage + "' to '" + newPackage + "' cannot be created.");
    }
  }

  @NotNull
  public static HaxePackageMapEntry parse(@NotNull String mapping) throws FormatException {
    String[] pieces = mapping.split(SEPARATOR_STRING);
    if (pieces.length != 2) {
      throw new FormatException("Mapping string '" + mapping + "' contains too many separators (" + SEPARATOR + ")");
    }
    return new HaxePackageMapEntry(pieces[0], pieces[1]);
  }

  @NotNull
  public String normalize() {
    return oldPackage + SEPARATOR + newPackage;
  }

  public String toString() {
    return normalize();
  }

  @NotNull
  public String getName() {
    return oldPackage;
  }

  @NotNull
  public String getReplacement() {
    return newPackage;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HaxePackageMapEntry entry = (HaxePackageMapEntry)o;

    if (!oldPackage.equals(entry.oldPackage)) return false;
    return newPackage.equals(entry.newPackage);
  }

  @Override
  public int hashCode() {
    int result = oldPackage.hashCode();
    result = 31 * result + newPackage.hashCode();
    return result;
  }

  public static class FormatException extends Exception {
    FormatException(String s) {
      super(s);
    }
  }

}
