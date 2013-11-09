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

package com.uphyca.kitkat.storage.util;

import java.util.Locale;

/**
 * ログ出力用のユーティリティクラス。
 * 
 * @author masui@uphyca.com
 */
public class Log {

    public static String TAG = "KitKatStorage";

    public static boolean DEBUG = android.util.Log.isLoggable(TAG, android.util.Log.VERBOSE);

    public static void setTag(String tag) {
        d("Changing log tag to %s", tag);
        TAG = tag;

        // Reinitialize the DEBUG "constant"
        DEBUG = android.util.Log.isLoggable(TAG, android.util.Log.VERBOSE);
    }

    public static void v(String format, Object... args) {
        if (DEBUG) {
            android.util.Log.v(TAG, buildMessage(format, args));
        }
    }

    public static void d(String format, Object... args) {
        android.util.Log.d(TAG, buildMessage(format, args));
    }

    public static void e(String format, Object... args) {
        android.util.Log.e(TAG, buildMessage(format, args));
    }

    public static void e(Throwable tr, String format, Object... args) {
        android.util.Log.e(TAG, buildMessage(format, args), tr);
    }

    public static void wtf(String format, Object... args) {
        android.util.Log.wtf(TAG, buildMessage(format, args));
    }

    public static void wtf(Throwable tr, String format, Object... args) {
        android.util.Log.wtf(TAG, buildMessage(format, args), tr);
    }

    private static String buildMessage(String format, Object... args) {
        String msg = (args == null) ? format : String.format(Locale.US, format, args);
        StackTraceElement[] trace = new Throwable().fillInStackTrace()
                                                   .getStackTrace();

        String caller = "<unknown>";
        for (int i = 2; i < trace.length; i++) {
            Class<?> clazz = trace[i].getClass();
            if (!clazz.equals(Log.class) && !trace[i].getMethodName()
                                                     .startsWith("ajc$")) {
                String callingClass = trace[i].getClassName();
                callingClass = callingClass.substring(callingClass.lastIndexOf('.') + 1);
                callingClass = callingClass.substring(callingClass.lastIndexOf('$') + 1);

                caller = callingClass + "." + trace[i].getMethodName();
                break;
            }
        }
        return String.format(Locale.US, "[%d] %s: %s", Thread.currentThread()
                                                             .getId(), caller, msg);
    }

}
