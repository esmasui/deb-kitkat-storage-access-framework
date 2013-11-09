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

package com.uphyca.kitkat.storage.internal;

/**
 * mime-typeを扱うためのインターフェイス。
 * 
 * @author masui@uphyca.com
 */
public interface MimeTypeResolver {

    /**
     * ファイル名からmime-typeを得る。
     * 
     * @param name ファイル名
     * @return mime-type
     */
    String resolveMimeTypeFromName(String name);

    /**
     * ファイル名の拡張子をmime-type的に適切なものにする。
     * 
     * @param mimeType mime-type
     * @param name ファイル名
     * @return 適切な拡張子につけかえたファイル名
     */
    String suggestExtensionIfNecessary(String mimeType, String name);
}
