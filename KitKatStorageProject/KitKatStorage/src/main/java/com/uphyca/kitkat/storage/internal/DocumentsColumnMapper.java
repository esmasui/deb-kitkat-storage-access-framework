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

import android.provider.DocumentsContract;

/**
 * DocumentのカラムとStorageのドメインクラスをマッピングするためのインターフェイス。
 * 
 * @author masui@uphyca.com
 */
public interface DocumentsColumnMapper<T> {

    /**
     * @see DocumentsContract.Document#COLUMN_DOCUMENT_ID
     *      <p>
     *      Type: STRING
     * @param source
     */
    String mapDocumentId(T source);

    /**
     * @see DocumentsContract.Document#COLUMN_MIME_TYPE
     *      <p>
     *      Type: STRING
     * @param source
     */
    String mapMimeType(T source);

    /**
     * @see DocumentsContract.Document#COLUMN_DISPLAY_NAME
     *      <p>
     *      Type: STRING
     * @param source a source
     */
    String mapDisplayName(T source);

    /**
     * @see DocumentsContract.Document#COLUMN_SUMMARY
     *      <p>
     *      Type: STRING
     * @param source
     */
    String mapSummary(T source);

    /**
     * @see DocumentsContract.Document#COLUMN_LAST_MODIFIED
     *      <p>
     *      Type: INTEGER (long)
     * @param source
     */
    Long mapLastModified(T source);

    /**
     * @see DocumentsContract.Document#COLUMN_ICON
     *      <p>
     *      Type: INTEGER (int)
     * @param source
     */
    Integer mapIcon(T source);

    /**
     * @see DocumentsContract.Document#COLUMN_FLAGS
     *      <p>
     *      Type: INTEGER (int)
     * @see DocumentsContract.Document#FLAG_SUPPORTS_WRITE
     * @see DocumentsContract.Document#FLAG_SUPPORTS_DELETE
     * @see DocumentsContract.Document#FLAG_SUPPORTS_THUMBNAIL
     * @see DocumentsContract.Document#FLAG_DIR_PREFERS_GRID
     * @see DocumentsContract.Document#FLAG_DIR_PREFERS_LAST_MODIFIED
     * @param source
     */
    Integer mapFlags(T source);

    /**
     * @see DocumentsContract.Document#COLUMN_SIZE
     *      <p>
     *      Type: INTEGER (long)
     * @param source
     */
    Long mapSize(T source);
}
