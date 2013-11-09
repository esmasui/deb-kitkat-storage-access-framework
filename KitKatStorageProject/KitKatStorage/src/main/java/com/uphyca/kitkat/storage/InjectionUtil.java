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

package com.uphyca.kitkat.storage;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.ContentProvider;
import android.content.Context;
import android.support.v4.app.Fragment;
import dagger.ObjectGraph;

/**
 * DIのためのユーティリティクラス。
 * このクラスのメソッドはaspectで呼ばれる。
 * 
 * @see com.uphyca.kitkat.storage.aspects.InjectionAspect
 * @author masui@uphyca.com
 */
public abstract class InjectionUtil {

    private InjectionUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * activityの依存をインジェクトする。
     * 
     * @param activity activity
     */
    public static void inject(Activity activity) {
        inject(acquireInjector(activity.getApplication()), activity);
    }

    /**
     * serviceの依存をインジェクトする。
     * 
     * @param service service
     */
    public static void inject(Service service) {
        inject(acquireInjector(service.getApplication()), service);
    }

    /**
     * fragmentの依存をインジェクトする。
     * 
     * @param fragment
     */
    public static void inject(Fragment fragment) {
        inject(acquireInjector(fragment.getActivity()
                                       .getApplication()), fragment);
    }

    /**
     * providerの依存をインジェクトする。
     * 
     * @param provider
     */
    public static void inject(ContentProvider provider) {
        inject(acquireInjector(provider.getContext()), provider);
    }

    private static ObjectGraph acquireInjector(Context context) {
        return acquireInjector(Application.class.cast((context instanceof Application) ? context : context.getApplicationContext()));
    }

    private static ObjectGraph acquireInjector(Application application) {
        return HelloKitKatApplication.class.cast(application)
                                           .getInjector();
    }

    private static void inject(ObjectGraph injector, Object obj) {
        if (injector == null) {
            return;
        }
        injector.inject(obj);
    }
}
