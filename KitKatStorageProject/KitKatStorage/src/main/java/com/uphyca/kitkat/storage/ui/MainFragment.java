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

package com.uphyca.kitkat.storage.ui;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.SecureRandom;

import javax.inject.Inject;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import butterknife.OnClick;

import com.uphyca.kitkat.storage.R;
import com.uphyca.kitkat.storage.internal.SkyDriveClient;

/**
 * サンプルUIのfragment。
 * SkyDriveをバックエンドにした DocumentsProviderに対して以下の機能が使える。
 * SkyDriveへのログインにLive IDが必要。
 * <ul>
 * <li>ディレクトリの作成</li>
 * <li>テキストファイルの作成</li>
 * <li>テキストファイルのダンプ</li>
 * <li>テキストファイルの編集（固定的な文字列です）</li>
 * <li>テキストファイルの削除（エラーが出て失敗する）</li>
 * </ul>
 * 
 * @author masui@uphyca.com
 */
public class MainFragment extends Fragment {

    private static final int REQUEST_OPEN = 1;
    private static final int REQUEST_CREATE = 2;
    private static final int REQUEST_EDIT = 3;
    private static final int REQUEST_MKDIR = 4;
    private static final int REQUEST_DELETE = 5;

    @Inject
    SkyDriveClient mSkyDriveClient;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Activity.RESULT_OK && data.getData() != null) {
            int takeFlags = data.getFlags();
            takeFlags &= (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            // Check for the freshest data.
            getActivity().getContentResolver()
                         .takePersistableUriPermission(data.getData(), takeFlags);
        }
        switch (requestCode) {
            case REQUEST_OPEN:
                onOpenRequestResult(resultCode, data);
                return;
            case REQUEST_EDIT:
            case REQUEST_CREATE:
                onEditRequestResult(resultCode, data);
                return;
            case REQUEST_DELETE:
                onDeleteRequestResult(resultCode, data);
                return;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Live APIの認証を行う。
     */
    @OnClick(R.id.button_auth)
    void onAuthButtonClick() {
        mSkyDriveClient.login(getActivity(), new SkyDriveClient.SkyDriveAuthListener() {
            @Override
            public void onAuthComplete() {
                Toast.makeText(getActivity(), "Logged in", Toast.LENGTH_SHORT)
                     .show();
            }

            @Override
            public void onAuthError(Exception exception) {
                Toast.makeText(getActivity(), exception.getMessage(), Toast.LENGTH_SHORT)
                     .show();
            }
        });
    }

    /**
     * ファイルをダンプする。
     */
    @OnClick(R.id.button_open)
    void onOpenButtonClick() {
        Intent intent = new Intent().setAction(Intent.ACTION_OPEN_DOCUMENT)
                                    .addCategory(Intent.CATEGORY_OPENABLE)
                                    .setType("text/*");
        startActivityForResult(intent, REQUEST_OPEN);
    }

    /**
     * ディレクトリを作成する。
     */
    @OnClick(R.id.button_mkdir)
    void onMkdirButtonClick() {
        String fileName = String.format("folder-%d", new SecureRandom().nextInt());
        Intent intent = new Intent().setAction(Intent.ACTION_CREATE_DOCUMENT)
                                    .addCategory(Intent.CATEGORY_OPENABLE)
                                    .setType(DocumentsContract.Document.MIME_TYPE_DIR)
                                    .putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, REQUEST_MKDIR);
    }

    /**
     * ファイルを作成する。
     */
    @OnClick(R.id.button_create)
    void onCreateButtonClick() {
        String fileName = String.format("document-%d.txt", new SecureRandom().nextInt());
        Intent intent = new Intent().setAction(Intent.ACTION_CREATE_DOCUMENT)
                                    .addCategory(Intent.CATEGORY_OPENABLE)
                                    .setType("text/plain")
                                    .putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, REQUEST_CREATE);
    }

    /**
     * ファイルを編集する。
     */
    @OnClick(R.id.button_edit)
    void onEditButtonClick() {
        Intent intent = new Intent().setAction(Intent.ACTION_OPEN_DOCUMENT)
                                    .addCategory(Intent.CATEGORY_OPENABLE)
                                    .setType("text/plain");
        startActivityForResult(intent, REQUEST_EDIT);
    }

    /**
     * ファイルを削除する。
     */
    @OnClick(R.id.button_delete)
    void onDeleteButtonClick() {
        Intent intent = new Intent().setAction(Intent.ACTION_OPEN_DOCUMENT)
                                    .addCategory(Intent.CATEGORY_OPENABLE)
                                    .setType("text/plain");
        startActivityForResult(intent, REQUEST_DELETE);
    }

    /**
     * テキストファイルのダンプを実行する。
     * 
     * @param resultCode
     * @param data
     */
    void onOpenRequestResult(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return;
        }

        new AsyncTask<Uri, Void, String>() {
            Context appContext = getActivity().getApplicationContext();

            @Override
            protected String doInBackground(Uri... params) {
                String text;
                try {
                    text = readTextFromUri(params[0]);
                } catch (IOException e) {
                    text = e.getMessage();
                }
                return text;
            }

            @Override
            protected void onPostExecute(String s) {
                Toast.makeText(appContext, s, Toast.LENGTH_SHORT)
                     .show();
            }
        }.execute(data.getData());
    }

    /**
     * テキストファイルの編集を実行する。
     * 
     * @param resultCode
     * @param data
     */
    void onEditRequestResult(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return;
        }

        new AsyncTask<Uri, Void, String>() {
            Context appContext = getActivity().getApplicationContext();

            @Override
            protected String doInBackground(Uri... params) {
                String text;
                try {
                    alterDocument(params[0]);
                    text = "Overwritten";
                } catch (IOException e) {
                    text = e.getMessage();
                }
                return text;
            }

            @Override
            protected void onPostExecute(String s) {
                Toast.makeText(appContext, s, Toast.LENGTH_SHORT)
                     .show();
            }
        }.execute(data.getData());
    }

    /**
     * ファイルの削除を実行する。
     * 
     * @param resultCode
     * @param data
     */
    void onDeleteRequestResult(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return;
        }

        new AsyncTask<Uri, Void, String>() {
            Context appContext = getActivity().getApplicationContext();

            @Override
            protected String doInBackground(Uri... params) {
                //FIXME エラー吐いて削除できない
                DocumentsContract.deleteDocument(getActivity().getContentResolver(), params[0]);
                return "Delete...できない！";
            }

            @Override
            protected void onPostExecute(String s) {
                Toast.makeText(appContext, s, Toast.LENGTH_SHORT)
                     .show();
            }
        }.execute(data.getData());
    }

    /**
     * uriに対応するテキストファイルの内容をダンプする。
     * 
     * @param uri
     * @return
     * @throws IOException
     */
    private String readTextFromUri(Uri uri) throws IOException {
        InputStream inputStream = getActivity().getContentResolver()
                                               .openInputStream(uri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        reader.close();
        inputStream.close();
        return stringBuilder.toString();
    }

    /**
     * uriに対応するテキストファイルの内容を書き換える。
     * 
     * @param uri
     * @throws IOException
     */
    private void alterDocument(Uri uri) throws IOException {
        ParcelFileDescriptor pfd = getActivity().getContentResolver()
                                                .openFileDescriptor(uri, "w");
        FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
        fileOutputStream.write(("Overwritten by MyCloud at " + System.currentTimeMillis() + "\n").getBytes());
        // Let the document provider know you're done by closing the stream.
        fileOutputStream.close();
        pfd.close();
    }
}
