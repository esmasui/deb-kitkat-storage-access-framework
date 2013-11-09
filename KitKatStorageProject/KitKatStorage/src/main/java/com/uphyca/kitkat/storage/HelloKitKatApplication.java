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
import dagger.ObjectGraph;

/**
 * Storage Access Frameworkのデモ用アプリケーション。
 * 
 * @author masui@uphyca.com
 */
public class HelloKitKatApplication extends Application {

    private ObjectGraph mInjector;

    public HelloKitKatApplication() {
        //FIXME DocumentsProviderのonCreate()がApplicationのonCreate()より先に呼ばれるのでコンストラクタで、仕方なくDIコンテナを初期化しているが、できればonCreate()でやりたい。
        mInjector = ObjectGraph.create(new HelloKitKatModule(this));
    }

    ObjectGraph getInjector() {
        return mInjector;
    }
}
