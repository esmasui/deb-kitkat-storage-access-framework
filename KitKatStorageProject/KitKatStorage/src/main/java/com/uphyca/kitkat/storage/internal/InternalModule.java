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

import javax.inject.Singleton;

import android.content.Context;
import android.webkit.MimeTypeMap;

import com.microsoft.live.LiveAuthClient;
import com.uphyca.kitkat.storage.internal.impl.DelegatingMimeTypeResolver;
import com.uphyca.kitkat.storage.internal.impl.LiveSdkDocumentsColumnMapper;
import com.uphyca.kitkat.storage.internal.impl.LiveSdkSkyDriveClient;
import com.uphyca.kitkat.storage.internal.impl.StrictSkyDriveClient;

import dagger.Module;
import dagger.Provides;

/**
 * internalパッケージのモジュール。
 * 
 * @author masui@uphyca.com
 */
@Module(library = true, complete = false)
public class InternalModule {

    /**
     * Live APIのクライアントID。悪用すんな。
     */
    private static final String LIVE_CLIENT_ID = "000000004C107D21";

    @Provides
    @Singleton
    LiveAuthClient provideLiveAuthClient(Context context) {
        return new LiveAuthClient(context, LIVE_CLIENT_ID);
    }

    @Provides
    @Singleton
    MimeTypeResolver provideMimeTypeResolver() {
        return new DelegatingMimeTypeResolver(MimeTypeMap.getSingleton());
    }

    @Provides
    @Singleton
    DocumentsColumnMapper provideDocumentsColumnMapper(MimeTypeResolver mimeTypeResolver) {
        return new LiveSdkDocumentsColumnMapper(mimeTypeResolver);
    }

    @Provides
    @Singleton
    SkyDriveClient provideSkyDriveClient(Context context, LiveAuthClient liveAuthClient) {
        LiveSdkSkyDriveClient delegate = new LiveSdkSkyDriveClient(context, liveAuthClient);
        return new StrictSkyDriveClient(delegate);
    }
}
