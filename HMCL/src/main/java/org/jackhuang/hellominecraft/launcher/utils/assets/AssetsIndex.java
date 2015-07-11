/*
 * Copyright 2013 huangyuhui <huanghongxun2008@126.com>
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.
 */
package org.jackhuang.hellominecraft.launcher.utils.assets;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author huangyuhui
 */
public class AssetsIndex {

    public static final String DEFAULT_ASSET_NAME = "legacy";
    private Map<String, AssetsObject> objects;
    private boolean virtual;

    public AssetsIndex() {
        this.objects = new LinkedHashMap();
    }

    public Map<String, AssetsObject> getFileMap() {
        return this.objects;
    }

    public Set<AssetsObject> getUniqueObjects() {
        return new HashSet(this.objects.values());
    }

    public boolean isVirtual() {
        return this.virtual;
    }
}
