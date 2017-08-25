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

import com.intellij.plugins.haxe.util.HaxeDebugLogger;
import com.intellij.util.containers.HashMap;
import org.apache.log4j.Level;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by ebishton on 8/18/17.
 */
public class HaxePackageMap {
  private static HaxeDebugLogger LOG = HaxeDebugLogger.getLogger();
  static { LOG.setLevel(Level.INFO); }

  private HashMap<String,HaxePackageMapEntry> map;

  public HaxePackageMap() {
    this(0);
  }

  public HaxePackageMap(int n) {
    map = new HashMap<String,HaxePackageMapEntry>(n < 13 ? 13 : n);
  }

  public HaxePackageMap(HaxePackageMap otherMap) {
    this.map = new HashMap<String,HaxePackageMapEntry>(otherMap.map);
  }

  public HaxePackageMap(String[] remappings) {
    this(remappings.length);
    for (String s : remappings) {
      add(s);
    }
    if (remappings.length != map.size()) {
      LOG.warn("Invalid remappings found in package map list.");
    }
  }

  public HaxePackageMap(Object[] remappings) {
    this(remappings.length);
    if (remappings.length > 0 && remappings[0] instanceof String) {
      for (Object o : remappings) {
        String s = (String)o;
        add(s);
      }
    } else {
      if (remappings.length == 0) {
        LOG.debug("Empty mapping list.");
      } else {
        LOG.warn("Internal error. Remapping objects were not strings.");
      }
    }
    if (remappings.length != map.size()) {
      LOG.warn("Invalid remappings found in package map list.");
    }
  }

  public boolean add(String s) {
    try {
      HaxePackageMapEntry entry = HaxePackageMapEntry.parse(s);
      return addEntry(entry);
    } catch (HaxePackageMapEntry.FormatException e) {
      LOG.info(e.getMessage());
      return false;
    }
  }

  public boolean add(String oldPackage, String newPackage) {
    try {
      HaxePackageMapEntry entry = new HaxePackageMapEntry(oldPackage, newPackage);
      return addEntry(entry);
    } catch (HaxePackageMapEntry.FormatException e) {
      LOG.info(e.getMessage());
      return false;
    }
  }

  private boolean addEntry(HaxePackageMapEntry entry) {
    if (map.containsKey(entry.getName())) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Package remapping already contains a key for " + entry.getName());
      }
      return false;
    }
    map.put(entry.getName(), entry);
    return true;
  }

  public String getMapFor(String pkg) {
    HaxePackageMapEntry entry = map.get(pkg);
    return null == entry ? null : entry.getReplacement();
  }

  public Collection<String> getAllRemappings() {
    Collection<String> remappings = new ArrayList<String>();
    for (String key : map.keySet()) {
      remappings.add(map.get(key).normalize());
    }
    return remappings;
  }

  public String toString() {
    if (map.isEmpty()) {
      return "";
    }

    StringBuilder s = new StringBuilder();
    int first = 0;

    for (String key : map.keySet()) {
      if (first++ > 0) s.append(",");

      HaxePackageMapEntry entry = map.get(key);
      s.append(entry.normalize());
    }
    return s.toString();
  }

}
