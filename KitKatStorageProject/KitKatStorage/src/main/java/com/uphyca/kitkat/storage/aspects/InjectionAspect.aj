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

 package com.uphyca.kitkat.storage.aspect;

import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.JoinPoint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.Views;
import com.uphyca.kitkat.storage.util.Log;
import com.uphyca.kitkat.storage.InjectionUtil;
import com.uphyca.kitkat.storage.ui.MainFragment;

/**
 * コードにあんまり入れたくない処理を書いたaspect.
 * @author masui@uphyca.com
 */
public aspect InjectionAspect {

    private pointcut myPackage(): within(com.uphyca.kitkat.storage..*);
    private pointcut activity(): target(android.app.Activity+);
    private pointcut fragment(): target(android.support.v4.app.Fragment+);
    private pointcut provider(): target(android.content.ContentProvider+);
    private pointcut skydrive(): target(com.uphyca.kitkat.storage.internal.impl.LiveSdkSkyDriveClient);
    private pointcut onCreate(): execution(* onCreate(..));
    private pointcut onCreateView(): execution(android.view.View onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle));
    private pointcut onDestroyView(): execution(void onDestroyView());
    private pointcut publicMethods(): execution(public * *(..));

    /** onCreate()が呼ばれたらactivityの依存をインジェクトする */
    before(android.app.Activity activity): myPackage() && activity() && onCreate() && this(activity) {
        InjectionUtil.inject(activity);
    }

    /** onCreate()が呼ばれたらfragmentの依存をインジェクトする */
    before(android.support.v4.app.Fragment fragment): myPackage() && fragment() && onCreate() && this(fragment) {
        InjectionUtil.inject(fragment);
    }

    /** onCreate()が呼ばれたらproviderの依存をインジェクトする */
    before(android.content.ContentProvider provider): myPackage() && provider() && onCreate() && this(provider) {
        InjectionUtil.inject(provider);
    }

    /** onCreate()が呼ばれたらfragmentが参照するviewをインジェクトする */
    after(android.support.v4.app.Fragment fragment) returning(android.view.View view): myPackage() && fragment() && onCreateView() && this(fragment) {
        Views.inject(fragment, view);
    }

    /** onDestroyView()が呼ばれたらfragmentが参照するviewへの参照をクリアする */
    before(android.support.v4.app.Fragment fragment): myPackage() && fragment() && onDestroyView() && this(fragment) {
        Views.reset(fragment);
    }

    /** インジェクトのためだけに意味なくオーバーライドしたくないのでインタータイプでオーバーライドする。普段は違うやりかたをするがソースが大仰になるのでこれで済ます */
    public void MainFragment.onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /** インジェクトのためだけに意味なくオーバーライドしたくないのでインタータイプでオーバーライドする。普段は違うやりかたをするがソースが大仰になるのでこれで済ます */
    public void MainFragment.onDestroyView() {
        super.onDestroyView();
    }

    /** 例外の発生をログ出力する */
    before (Exception e): handler(Exception+) && myPackage() && args(e) {
        Log.e(e, e.getMessage());
    }

    /** providerの呼び出し、SkyDriveへのアクセスなどをログ出力する */
    before(): myPackage() && (
        execution(* com.uphyca.kitkat.storage.InjectionUtil.inject(dagger.ObjectGraph, Object))
        || (provider() && publicMethods())
        || (skydrive() && publicMethods())
    ){
         Object[] paramValues = thisJoinPoint.getArgs();
         String[] paramNames = ((CodeSignature) thisJoinPointStaticPart.getSignature()).getParameterNames();
         StringBuilder logLine = new StringBuilder();
         for(int i = 0, length = paramNames.length; i < length; ++i) {
            if(i > 0) logLine.append(',');
            logLine.append(paramNames[i]);
            logLine.append(':');
            logLine.append("%s");
         }
         Log.d(logLine.toString(), (Object[])paramValues);
    }

    /** providerの呼び出し、SkyDriveへのアクセスなどの結果をログ出力する */
    after() returning(Object obj): myPackage() && (
        (provider() && publicMethods())
        || (skydrive() && publicMethods())
    ){
         Log.d("= %s", obj);
    }
}
