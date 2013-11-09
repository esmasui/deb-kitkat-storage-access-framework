/*
 * Copyright (C) 2013 uPhyca Inc. http://www.uphyca.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uphyca.kitkat.storage.internal.impl;

import java.io.File;

import android.webkit.MimeTypeMap;

import com.uphyca.kitkat.storage.internal.MimeTypeResolver;

/**
 * MimeTypeMapを使った実装。
 * 
 * @author masui@uphyca.com
 */
public class DelegatingMimeTypeResolver implements MimeTypeResolver {
    private final MimeTypeMap mMimeTypeMap;

    public DelegatingMimeTypeResolver(MimeTypeMap mimeTypeMap) {
        mMimeTypeMap = mimeTypeMap;
    }

    @Override
    public String resolveMimeTypeFromName(String name) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(new File(name).toURI()
                                                                             .toString());
        if (extension != null) {
            type = mMimeTypeMap.getMimeTypeFromExtension(extension);
        }

        if (type == null) {
            type = "application/octet-stream";
        }

        return type;
    }

    @Override
    public String suggestExtensionIfNecessary(String mimeType, String name) {
        String returnThis = removeExtension(mimeType, name);
        returnThis = addExtension(mimeType, returnThis);
        return returnThis;
    }

    private String removeExtension(String mimeType, String name) {
        final int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            final String extension = name.substring(lastDot + 1);
            final String nameMime = mMimeTypeMap.getMimeTypeFromExtension(extension);
            if (mimeType.equals(nameMime)) {
                return name.substring(0, lastDot);
            }
        }
        return name;
    }

    private String addExtension(String mimeType, String name) {
        final String extension = mMimeTypeMap.getExtensionFromMimeType(mimeType);
        if (extension != null) {
            return name + "." + extension;
        }
        return name;
    }
}
