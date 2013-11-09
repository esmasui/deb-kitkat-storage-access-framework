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

package com.uphyca.kitkat.storage.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.inject.Inject;

import android.annotation.TargetApi;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;

import com.uphyca.kitkat.storage.R;
import com.uphyca.kitkat.storage.internal.DocumentsColumnMapper;
import com.uphyca.kitkat.storage.internal.MimeTypeResolver;
import com.uphyca.kitkat.storage.internal.SkyDriveClient;
import com.uphyca.kitkat.storage.skydrive.SkyDriveObject;

/**
 * SkyDriveをバックエンドにした DocumentsProvider の実装。
 * FIXME アクセスするたびにネットワークアクセスしているので遅い。キャッシュしたり先読みしたりする必要がありそう。
 * 
 * @author masui@uphyca.com
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class SkyDriveProvider extends DocumentsProvider {

    private static final String[] DEFAULT_ROOT_PROJECTION = new String[] {
            DocumentsContract.Root.COLUMN_ROOT_ID, // 
            DocumentsContract.Root.COLUMN_MIME_TYPES, // 
            DocumentsContract.Root.COLUMN_FLAGS, //
            DocumentsContract.Root.COLUMN_ICON, //
            DocumentsContract.Root.COLUMN_TITLE, //
            DocumentsContract.Root.COLUMN_SUMMARY, //
            DocumentsContract.Root.COLUMN_DOCUMENT_ID, // 
            DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, //
    };
    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[] {
            DocumentsContract.Document.COLUMN_DOCUMENT_ID, //
            DocumentsContract.Document.COLUMN_MIME_TYPE, //
            DocumentsContract.Document.COLUMN_DISPLAY_NAME, //
            DocumentsContract.Document.COLUMN_LAST_MODIFIED, //
            DocumentsContract.Document.COLUMN_FLAGS, //
            DocumentsContract.Document.COLUMN_SIZE, //
    };

    @Inject
    DocumentsColumnMapper mDocumentsColumnMapper;

    @Inject
    MimeTypeResolver mMimeTypeResolver;

    @Inject
    SkyDriveClient mSkyDriveClient;

    /**
     * SkyDriveのルートディレクトリ。
     * FIXME プロバイダではなくSkyDriveClientが扱うべき情報
     */
    private static final String HOME_FOLDER = "me/skydrive";

    /**
     * ルートの情報を返す。最初に必ず呼ばれる。
     * 
     * @param projection
     * @return
     * @throws FileNotFoundException
     */
    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveRootProjection(projection));
        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(DocumentsContract.Root.COLUMN_ROOT_ID, HOME_FOLDER);
        int flags = 0;
        flags |= DocumentsContract.Root.FLAG_SUPPORTS_CREATE;
        //flags |= DocumentsContract.Root.FLAG_SUPPORTS_RECENTS;
        //flags |= DocumentsContract.Root.FLAG_SUPPORTS_SEARCH;
        row.add(DocumentsContract.Root.COLUMN_FLAGS, flags);
        row.add(DocumentsContract.Root.COLUMN_TITLE, getContext().getString(R.string.title));
        row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, HOME_FOLDER);
        row.add(DocumentsContract.Root.COLUMN_ICON, R.drawable.ic_skydrive);
        return result;
    }

    /**
     * ドキュメントのメタ情報を取得する為に呼ばれる。
     * 
     * @param documentId
     * @param projection
     * @return
     * @throws FileNotFoundException
     */
    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        if (HOME_FOLDER.equals(documentId)) {
            includeDefaultDocument(result);
            return result;
        }

        for (SkyDriveObject each : mSkyDriveClient.get(documentId)) {
            includeFile(result, each);
        }

        return result;
    }

    /**
     * ディレクトリ配下のファイルをリストする為に呼ばれる。
     * FIXME sortOrderを無視している
     * FIXME projectionを無視している
     * 
     * @param parentDocumentId
     * @param projection
     * @param sortOrder
     * @return
     * @throws FileNotFoundException
     */
    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));

        //SkyDriveはドキュメントのID/filesでファイルをリストする
        //FIXME このプロバイダがSkyDriveのAPIの詳細を知っているのはよくないので list() メソッドをSkyDriveClientに設けるなどしたほうが良さそう
        for (SkyDriveObject each : mSkyDriveClient.get(parentDocumentId + "/files")) {
            includeFile(result, each);
        }
        return result;
    }

    /**
     * ファイルの内容を取得する為に呼ばれる。
     * 
     * @param documentId
     * @param mode
     * @param signal
     * @return
     * @throws FileNotFoundException
     */
    @Override
    public ParcelFileDescriptor openDocument(final String documentId, String mode, final CancellationSignal signal) throws FileNotFoundException {

        try {
            final File file = mSkyDriveClient.download(documentId);
            final int accessMode = ParcelFileDescriptor.parseMode(mode);
            final boolean isWrite = (mode.indexOf('w') != -1);

            if (!isWrite) {
                return ParcelFileDescriptor.open(file, accessMode);
            }

            // 書き込みモードで開かれた時は、コールバックを設定する。
            // コールバックはクライアントがファイルを編集してクローズした時に呼ばれるので、それをクラウドに同期するトリガーにする。
            Handler handler = new Handler(getContext().getMainLooper());
            return ParcelFileDescriptor.open(file, accessMode, handler, new ParcelFileDescriptor.OnCloseListener() {
                @Override
                public void onClose(IOException e) {
                    // FIXME リトライ処理が要る。
                    new UploadTask(mSkyDriveClient, documentId, file).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            });
        } catch (IOException e) {
            throw new FileNotFoundException("Failed to open document with id " + documentId + " and mode " + mode);
        }
    }

    /**
     * ファイルを作成する為に呼ばれる。
     * 
     * @param parentDocumentId
     * @param mimeType
     * @param displayName
     * @return
     * @throws FileNotFoundException
     */
    @Override
    public String createDocument(String parentDocumentId, String mimeType, String displayName) throws FileNotFoundException {
        if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
            try {
                return mSkyDriveClient.mkdir(parentDocumentId, displayName);
            } catch (IOException e) {
                FileNotFoundException fileNotFound = new FileNotFoundException(e.getMessage());
                fileNotFound.initCause(e);
                throw fileNotFound;
            }
        }
        try {
            return mSkyDriveClient.touch(parentDocumentId, mMimeTypeResolver.suggestExtensionIfNecessary(mimeType, displayName));
        } catch (IOException e) {
            FileNotFoundException fileNotFound = new FileNotFoundException(e.getMessage());
            fileNotFound.initCause(e);
            throw fileNotFound;
        }
    }

    /**
     * ファイルを削除する為に呼ばれる。
     * 
     * @param documentId
     * @throws FileNotFoundException
     */
    @Override
    public void deleteDocument(String documentId) throws FileNotFoundException {
        // FIXME クライアントからdelete呼ぶとクラッシュする。why?
        // java.lang.SecurityException: android:deleteDocument: Neither user 10079 nor current process has write permission on content://com.uphyca.kitkat.storage.documents/document/file.1528683a3710d39f.1528683A3710D39F!1109.
        //   at android.app.ContextImpl.enforceForUri(ContextImpl.java:1811)
        //   at android.app.ContextImpl.enforceCallingOrSelfUriPermission(ContextImpl.java:1840)
        //   at android.content.ContextWrapper.enforceCallingOrSelfUriPermission(ContextWrapper.java:622)
        //   at android.provider.DocumentsProvider.call(DocumentsProvider.java:499)
        //   at android.content.ContentProvider$Transport.call(ContentProvider.java:325)
        //   at android.content.ContentProviderClient.call(ContentProviderClient.java:392)
        //   at android.provider.DocumentsContract.deleteDocument(DocumentsContract.java:812)
        //   at android.provider.DocumentsContract.deleteDocument(DocumentsContract.java:796)
        //   at com.uphyca.kitkat.storage.ui.MainFragment$4.doInBackground(MainFragment.java:221)
        //   at com.uphyca.kitkat.storage.ui.MainFragment$4.doInBackground(MainFragment.java:1)
        //   at android.os.AsyncTask$2.call(AsyncTask.java:288)
        //   at java.util.concurrent.FutureTask.run(FutureTask.java:237)
        //   at android.os.AsyncTask$SerialExecutor$1.run(AsyncTask.java:231)
        //   at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1112)
        //   at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:587)
        //   at java.lang.Thread.run(Thread.java:841)

        try {
            mSkyDriveClient.delete(documentId);
        } catch (IOException e) {
            FileNotFoundException fileNotFound = new FileNotFoundException(e.getMessage());
            fileNotFound.initCause(e);
            throw fileNotFound;
        }
    }

    /**
     * ドキュメントの履歴を取得する為に呼ばれる。
     * ルートがクエリされた時に、DocumentsContract.Root.FLAG_SUPPORTS_RECENTS を設定していなければ呼ばれない。
     * 
     * @param rootId
     * @param projection
     * @return
     * @throws FileNotFoundException
     */
    @Override
    public Cursor queryRecentDocuments(String rootId, String[] projection) throws FileNotFoundException {
        //デフォルト実装は例外を投げる
        return super.queryRecentDocuments(rootId, projection);
    }

    /**
     * ドキュメントを検索する為に呼ばれる。
     * ルートがクエリされた時に、DocumentsContract.Root.FLAG_SUPPORTS_SEARCH を設定していなければ呼ばれない。
     * 
     * @param rootId
     * @param query
     * @param projection
     * @return
     * @throws FileNotFoundException
     */
    @Override
    public Cursor querySearchDocuments(String rootId, String query, String[] projection) throws FileNotFoundException {
        //デフォルト実装は例外を投げる
        return super.querySearchDocuments(rootId, query, projection);
    }

    /**
     * 調べてないので分からない。
     * 
     * @param documentId
     * @return
     * @throws FileNotFoundException
     */
    @Override
    public String getDocumentType(String documentId) throws FileNotFoundException {
        return super.getDocumentType(documentId);
    }

    /**
     * ドキュメントのサムネイルを取得する為に呼ばれる。
     * サムネイルを返さないか、例外を投げるとデフォルトのサムネイルが使われる。
     * ドキュメントがクエリされた時に、DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL を設定していないドキュメントに対しては呼ばれない。
     * 
     * @param documentId
     * @param sizeHint
     * @param signal
     * @return
     * @throws FileNotFoundException
     */
    @Override
    public AssetFileDescriptor openDocumentThumbnail(String documentId, Point sizeHint, CancellationSignal signal) throws FileNotFoundException {
        return super.openDocumentThumbnail(documentId, sizeHint, signal);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    private void includeDefaultDocument(MatrixCursor result) {
        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, HOME_FOLDER);
        row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.MIME_TYPE_DIR);
        int flags = 0;
        flags |= DocumentsContract.Document.FLAG_DIR_PREFERS_LAST_MODIFIED;
        flags |= DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE;
        row.add(DocumentsContract.Document.COLUMN_FLAGS, flags);
    }

    private void includeFile(MatrixCursor result, SkyDriveObject skyDriveObj) {
        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, mDocumentsColumnMapper.mapDocumentId(skyDriveObj));
        row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, mDocumentsColumnMapper.mapMimeType(skyDriveObj));
        row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, mDocumentsColumnMapper.mapDisplayName(skyDriveObj));
        row.add(DocumentsContract.Document.COLUMN_SUMMARY, mDocumentsColumnMapper.mapSummary(skyDriveObj));
        row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, mDocumentsColumnMapper.mapLastModified(skyDriveObj));
        row.add(DocumentsContract.Document.COLUMN_ICON, mDocumentsColumnMapper.mapIcon(skyDriveObj));
        row.add(DocumentsContract.Document.COLUMN_SIZE, mDocumentsColumnMapper.mapSize(skyDriveObj));
        row.add(DocumentsContract.Document.COLUMN_FLAGS, mDocumentsColumnMapper.mapFlags(skyDriveObj));
    }

    private String[] resolveRootProjection(String[] projection) {
        return projection == null ? DEFAULT_ROOT_PROJECTION : projection;
    }

    private String[] resolveDocumentProjection(String[] projection) {
        return projection == null ? DEFAULT_DOCUMENT_PROJECTION : projection;
    }

    private static class UploadTask extends AsyncTask<Void, Void, Void> {

        private final SkyDriveClient mSkyDriveClient;
        private final String mDocumentId;
        private final File mLocalFile;

        private UploadTask(SkyDriveClient skyDriveClient, String documentId, File localFile) {
            mSkyDriveClient = skyDriveClient;
            mDocumentId = documentId;
            mLocalFile = localFile;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (!mLocalFile.exists()) {
                return null;
            }

            final SkyDriveObject[] documents = mSkyDriveClient.get(mDocumentId);
            if (documents.length < 1) {
                return null;
            }

            SkyDriveObject document = documents[0];
            try {
                mSkyDriveClient.upload(document.getParentId(), document.getName(), mLocalFile);
            } catch (IOException ignore) {
            }
            return null;
        }
    }
}
