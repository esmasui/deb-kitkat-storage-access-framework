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

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;

import com.microsoft.live.LiveAuthClient;
import com.microsoft.live.LiveAuthException;
import com.microsoft.live.LiveAuthListener;
import com.microsoft.live.LiveConnectClient;
import com.microsoft.live.LiveConnectSession;
import com.microsoft.live.LiveDownloadOperation;
import com.microsoft.live.LiveOperation;
import com.microsoft.live.LiveOperationException;
import com.microsoft.live.LiveStatus;
import com.microsoft.live.OverwriteOption;
import com.uphyca.kitkat.storage.internal.SkyDriveClient;
import com.uphyca.kitkat.storage.skydrive.JsonKeys;
import com.uphyca.kitkat.storage.skydrive.Scopes;
import com.uphyca.kitkat.storage.skydrive.SkyDriveObject;

/**
 * LiveSDK for Androidを使った実装。
 * 
 * @see <a href="https://github.com/liveservices/LiveSDK-for-Android/">LiveSDK for Android</a>
 * @author masui@uphyca.com
 */
public class LiveSdkSkyDriveClient implements SkyDriveClient {

    private static final List<String> SCOPES = Arrays.asList(new String[] {
            Scopes.SIGNIN, //
            Scopes.OFFLINE_ACCESS, //
            Scopes.SKYDRIVE_UPDATE
    });

    private final Context mContext;
    private final LiveAuthClient mLiveAuthClient;
    private LiveConnectClient mLiveConnectClient;

    public LiveSdkSkyDriveClient(Context context, LiveAuthClient liveAuthClient) {
        mContext = context;
        mLiveAuthClient = liveAuthClient;

    }

    @Override
    public void initializeIfNecessary() {
        if (mLiveConnectClient != null) {
            return;
        }
        final CountDownLatch lock = new CountDownLatch(1);
        mLiveAuthClient.initialize(SCOPES, new LiveAuthListener() {
            @Override
            public void onAuthComplete(LiveStatus status, LiveConnectSession session, Object userState) {
                if (status == LiveStatus.CONNECTED) {
                    mLiveConnectClient = new LiveConnectClient(session);
                }
                lock.countDown();
            }

            @Override
            public void onAuthError(LiveAuthException exception, Object userState) {
                lock.countDown();
            }
        });
        try {
            lock.await();
        } catch (InterruptedException ignore) {
        }
    }

    @Override
    public void login(final Activity activity, final SkyDriveAuthListener listener) {
        mLiveAuthClient.login(activity, SCOPES, new LiveAuthListener() {
            @Override
            public void onAuthComplete(LiveStatus status, LiveConnectSession session, Object userState) {
                if (status == LiveStatus.CONNECTED) {
                    mLiveConnectClient = new LiveConnectClient(session);
                    listener.onAuthComplete();
                    return;
                }
                Exception e = new RuntimeException("Login did not connect. Status is " + status + ".");
                e.fillInStackTrace();
                listener.onAuthError(e);
            }

            @Override
            public void onAuthError(LiveAuthException exception, Object userState) {
                Exception e = new RuntimeException(exception);
                listener.onAuthError(e);
            }
        });
    }

    @Override
    public SkyDriveObject[] get(String documentId) {
        initializeIfNecessary();
        if (mLiveConnectClient == null) {
            return empty();
        }

        final LiveOperation operation;
        try {
            operation = mLiveConnectClient.get(documentId);
        } catch (LiveOperationException ignore) {
            return empty();
        }

        JSONObject syncResult = operation.getResult();
        if (syncResult.has(JsonKeys.ERROR)) {
            return empty();
        }

        if (syncResult.has(JsonKeys.DATA)) {
            JSONArray array = syncResult.optJSONArray(JsonKeys.DATA);
            SkyDriveObject[] objects = new SkyDriveObject[array.length()];
            for (int i = 0, length = array.length(); i < length; ++i) {
                objects[i] = SkyDriveObject.create(array.optJSONObject(i));
            }
            return objects;
        }

        return new SkyDriveObject[] {
            SkyDriveObject.create(syncResult)
        };
    }

    @Override
    public File download(String documentId) throws IOException {
        initializeIfNecessary();
        if (mLiveConnectClient == null) {
            return null;
        }
        String path = documentId + "/content";

        final LiveDownloadOperation download;
        try {
            download = mLiveConnectClient.download(path);
        } catch (LiveOperationException ignore) {
            return null;
        }

        InputStream in = null;
        try {
            File temp = File.createTempFile("document", null, mContext.getCacheDir());
            in = download.getStream();
            drain(in, temp);
            return temp;
        } finally {
            closeQuietly(in);
        }
    }

    @Override
    public String upload(String path, String name, File file) throws IOException {
        initializeIfNecessary();
        if (mLiveConnectClient == null) {
            return null;
        }

        try {
            LiveOperation post = mLiveConnectClient.upload(path, name, file, OverwriteOption.Overwrite);
            JSONObject result = post.getResult();
            if (result.has(JsonKeys.ERROR)) {
                JSONObject error = result.optJSONObject(JsonKeys.ERROR);
                String message = error.optString(JsonKeys.MESSAGE);
                IOException ioException = new IOException(message);
                ioException.fillInStackTrace();
                throw ioException;
            }
            try {
                return result.getString(JsonKeys.ID);
            } catch (JSONException e) {
                IOException ioException = new IOException(e.getMessage());
                ioException.initCause(e);
                throw ioException;
            }
        } catch (LiveOperationException e) {
            IOException ioException = new IOException(e.getMessage());
            ioException.initCause(e);
            throw ioException;
        }
    }

    @Override
    public String mkdir(String path, String name) throws IOException {
        initializeIfNecessary();
        if (mLiveConnectClient == null) {
            return null;
        }

        Map<String, String> folder = new HashMap<>();
        folder.put(JsonKeys.NAME, name);
        folder.put(JsonKeys.DESCRIPTION, null);
        try {
            LiveOperation post = mLiveConnectClient.post(path, new JSONObject(folder));
            JSONObject result = post.getResult();
            if (result.has(JsonKeys.ERROR)) {
                JSONObject error = result.optJSONObject(JsonKeys.ERROR);
                String message = error.optString(JsonKeys.MESSAGE);
                IOException ioException = new IOException(message);
                ioException.fillInStackTrace();
                throw ioException;
            }
            try {
                return result.getString(JsonKeys.ID);
            } catch (JSONException e) {
                IOException ioException = new IOException(e.getMessage());
                ioException.initCause(e);
                throw ioException;
            }
        } catch (LiveOperationException e) {
            IOException ioException = new IOException(e.getMessage());
            ioException.initCause(e);
            throw ioException;
        }
    }

    @Override
    public String touch(String path, String name) throws IOException {
        initializeIfNecessary();
        if (mLiveConnectClient == null) {
            return null;
        }

        File temp = File.createTempFile("document", null, mContext.getCacheDir());
        if (!temp.exists()) {
            IOException ioException = new IOException("Failed to touch " + name);
            ioException.fillInStackTrace();
            throw ioException;
        }
        return upload(path, name, temp);
    }

    @Override
    public void delete(String path) throws IOException {
        initializeIfNecessary();
        if (mLiveConnectClient == null) {
            return;
        }
        try {
            mLiveConnectClient.delete(path);
        } catch (LiveOperationException e) {
            IOException ioException = new IOException("Failed to delete " + path);
            ioException.initCause(e);
            throw ioException;
        }
    }

    private static SkyDriveObject[] empty() {
        return new SkyDriveObject[0];
    }

    private static void drain(InputStream in, File dest) throws IOException {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
        try {
            byte[] buf = new byte[8192];
            for (int c; (c = in.read(buf)) > -1;) {
                out.write(buf, 0, c);
            }
            out.flush();
        } finally {
            closeQuietly(out);
        }
    }

    private static void closeQuietly(Closeable res) {
        if (res == null) {
            return;
        }
        try {
            res.close();
        } catch (IOException e) {
        }
    }
}
