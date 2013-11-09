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

import android.app.Application;
import android.content.Context;

import com.uphyca.kitkat.storage.internal.InternalModule;
import com.uphyca.kitkat.storage.provider.ProviderModule;
import com.uphyca.kitkat.storage.ui.UIModule;

import dagger.Module;
import dagger.Provides;

/**
 * Storage Access Frameworkのデモ用モジュール。
 * 
 * @author masui@uphyca.com
 */
@Module(includes = {
        UIModule.class, //
        ProviderModule.class, //
        InternalModule.class, //
})
public class HelloKitKatModule {

    private final Context mContext;

    public HelloKitKatModule(Application application) {
        mContext = application;
    }

    @Provides
    Context provideAppContext() {
        return mContext;
    }
}
