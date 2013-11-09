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

import java.io.File;
import java.io.IOException;

import android.app.Activity;

import com.uphyca.kitkat.storage.internal.impl.LiveSdkSkyDriveClient;
import com.uphyca.kitkat.storage.skydrive.SkyDriveObject;

/**
 * SkyDrive&trade;にアクセスするためのインターフェイス。
 * 
 * @author masui@uphyca.com
 */
public interface SkyDriveClient {

    /**
     * Live APIの認証結果を処理するためのコールバックインターフェイス。
     */
    public interface SkyDriveAuthListener {

        /**
         * 認証が成功したときに呼ばれる。
         */
        void onAuthComplete();

        /**
         * 認証が失敗したときに呼ばれる。
         * 
         * @param exception 失敗原因を表す例外
         */
        void onAuthError(Exception exception);
    }

    /**
     * 初期化が必要なら初期化する。
     * 現在の実装ではクライアントが明示的に呼ぶ必要はない。
     */
    void initializeIfNecessary();

    /**
     * Live APIでログインする。
     * このアプリケーションをStorage Frameworkで使う前に一度ログインしておく必要がある。
     * 一旦ログインすれば、認証結果は保持される。
     * 
     * @param activity ログインに使うactivity
     * @param listener 認証結果を受け取るインターフェイス
     */
    void login(Activity activity, LiveSdkSkyDriveClient.SkyDriveAuthListener listener);

    /**
     * 指定のIDのドキュメントを同期的に取得する。
     * 一意なIDの場合ドキュメントは一件だけ、それ以外は複数件の結果になる。
     * 対応するファイルが無い場合はからの配列を返す。
     * 結果がnullになることはない。
     * 
     * @param documentId ID
     * @return IDに対応するドキュメント(s)
     */
    SkyDriveObject[] get(String documentId);

    /**
     * 指定のIDのファイルを同期的にダウンロードする。
     * 
     * @param documentId ID
     * @return IDに対応するドキュメントのファイル
     * @throws IOException ダウンロードに失敗した場合に発生する
     */
    File download(String documentId) throws IOException;

    /**
     * 指定のIDのディレクトリに、指定のIDのファイルをアップロードする。
     * すでに同じIDのファイルがある場合は上書きする。
     * 
     * @param path ファイルを作成するディレクトリのID
     * @param name アップロードするファイルのID
     * @param file アップロードするファイルの内容
     * @return アップロードされたファイルのID
     * @throws IOException アップロードに失敗した場合に発生する
     */
    String upload(String path, String name, File file) throws IOException;

    /**
     * 指定のIDのディレクトリに、指定の名前のディレクトリを作成する。
     * 
     * @param path ディレクトリを作成するディレクトリのID
     * @param name 作成するディレクトリの名前
     * @return 作成されたディレクトリの名前
     * @throws IOException ディレクトリの作成に失敗した場合に発生する
     */
    String mkdir(String path, String name) throws IOException;

    /**
     * 指定のIDのディレクトリに、指定の名前のファイルを作成する。
     * 作成されたファイルは空なので、 内容を書き換えるには #upload(java.lang.String,lava.lang.String,java.io.File) メソッドを使う。
     * 
     * @param path ファイルを作成するディレクトリのID
     * @param name 作成するファイルの名前
     * @return 作成されたファイルのID
     * @throws IOException 作成に失敗した場合に発生する
     */
    String touch(String path, String name) throws IOException;

    /**
     * 指定のIDのファイルを削除する。
     * 
     * @param path 削除するファイルのID
     * @throws IOException ファイルの削除に失敗した場合に発生する
     */
    void delete(String path) throws IOException;
}
