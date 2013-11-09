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
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;

import com.uphyca.kitkat.storage.internal.SkyDriveClient;
import com.uphyca.kitkat.storage.skydrive.SkyDriveObject;

/**
 * android.provider.DocumentsProvider のメソッドの中で別スレッドで実行したネットワークアクセスの結果を同期するための実装。
 * クライアントはUIスレッドからプロバイダにアクセスした際に、同じスレッドでネットワークアクセスすると android.os.NetworkOnMainThreadException が発生する。
 * デベロッパードキュメントでは android.os.ParcelFileDescriptorのpipeを使うのがベター的なことが書いてあった。
 * pipeの動作を確認するテストは com.uphyca.kitkat.storage.ParcelFileDescriptorTest なのだが、こんな感じでやるとロックしてしまう。
 * 
 * @author masui@uphyca.com
 */
public class StrictSkyDriveClient implements SkyDriveClient {

    private final SkyDriveClient mDelegate;

    public StrictSkyDriveClient(SkyDriveClient delegate) {
        mDelegate = delegate;
    }

    @Override
    public void initializeIfNecessary() {
        mDelegate.initializeIfNecessary();
    }

    @Override
    public void login(Activity activity, SkyDriveAuthListener listener) {
        mDelegate.login(activity, listener);
    }

    @Override
    public SkyDriveObject[] get(final String documentId) {
        try {
            return sync(new NetworkOperation<SkyDriveObject[]>() {
                @Override
                public SkyDriveObject[] execute() throws IOException {
                    return mDelegate.get(documentId);
                }
            });
        } catch (IOException e) {
            return new SkyDriveObject[0];
        }
    }

    @Override
    public File download(final String documentId) throws IOException {
        return sync(new NetworkOperation<File>() {
            @Override
            public File execute() throws IOException {
                return mDelegate.download(documentId);
            }
        });
    }

    @Override
    public String upload(final String path, final String name, final File file) throws IOException {
        return sync(new NetworkOperation<String>() {
            @Override
            public String execute() throws IOException {
                return mDelegate.upload(path, name, file);
            }
        });
    }

    @Override
    public String mkdir(final String path, final String name) throws IOException {
        return sync(new NetworkOperation<String>() {
            @Override
            public String execute() throws IOException {
                return mDelegate.mkdir(path, name);
            }
        });
    }

    @Override
    public String touch(final String path, final String name) throws IOException {
        return sync(new NetworkOperation<String>() {
            @Override
            public String execute() throws IOException {
                return mDelegate.touch(path, name);
            }
        });
    }

    @Override
    public void delete(final String path) throws IOException {
        sync(new NetworkOperation<Void>() {
            @Override
            public Void execute() throws IOException {
                mDelegate.delete(path);
                return null;
            }
        });
    }

    private static <T> T sync(final NetworkOperation<T> operation) throws IOException {
        return new NetworkOperationTask<T>().sync(operation);
    }

    private interface NetworkOperation<T> {
        T execute() throws IOException;
    }

    private static class NetworkOperationTask<T> extends AsyncTask<NetworkOperation<T>, Void, T> {

        private final CountDownLatch mLock = new CountDownLatch(1);
        private T mResult;
        private IOException mError;

        @Override
        protected void onPostExecute(T t) {
            mResult = t;
            mLock.countDown();
        }

        @Override
        protected T doInBackground(NetworkOperation<T>... params) {
            try {
                return params[0].execute();
            } catch (IOException e) {
                mError = e;
                return null;
            }
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        T sync(NetworkOperation<T> operation) throws IOException {
            executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, operation);
            try {
                mLock.await();
            } catch (InterruptedException ignore) {
            }
            if (mError != null) {
                throw mError;
            }
            return mResult;
        }
    }
}
