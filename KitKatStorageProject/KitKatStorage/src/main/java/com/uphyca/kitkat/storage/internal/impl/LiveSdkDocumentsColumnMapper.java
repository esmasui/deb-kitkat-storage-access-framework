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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import android.provider.DocumentsContract;

import com.uphyca.kitkat.storage.internal.DocumentsColumnMapper;
import com.uphyca.kitkat.storage.internal.MimeTypeResolver;
import com.uphyca.kitkat.storage.skydrive.SkyDriveAlbum;
import com.uphyca.kitkat.storage.skydrive.SkyDriveAudio;
import com.uphyca.kitkat.storage.skydrive.SkyDriveFile;
import com.uphyca.kitkat.storage.skydrive.SkyDriveFolder;
import com.uphyca.kitkat.storage.skydrive.SkyDriveObject;
import com.uphyca.kitkat.storage.skydrive.SkyDrivePhoto;
import com.uphyca.kitkat.storage.skydrive.SkyDriveVideo;

/**
 * LiveSDK for Androidを使った実装。
 *
 * @see <a href="https://github.com/liveservices/LiveSDK-for-Android/">LiveSDK for Android</a>
 * @author masui@uphyca.com
 */
public class LiveSdkDocumentsColumnMapper implements DocumentsColumnMapper<SkyDriveObject> {

    private final MimeTypeResolver mMimeTypeResolver;
    private final DateFormat mDateFormat;
    {
        mDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZ");
        mDateFormat.setTimeZone(TimeZone.getDefault());
    }

    public LiveSdkDocumentsColumnMapper(MimeTypeResolver mimeTypeResolver) {
        mMimeTypeResolver = mimeTypeResolver;
    }

    @Override
    public String mapDocumentId(SkyDriveObject source) {
        return source.getId();
    }

    @Override
    public String mapMimeType(SkyDriveObject source) {
        return thatObjectIsDirectory(source) ? DocumentsContract.Document.MIME_TYPE_DIR : mMimeTypeResolver.resolveMimeTypeFromName(source.getName());
    }

    @Override
    public String mapDisplayName(SkyDriveObject source) {
        return source.getName();
    }

    @Override
    public String mapSummary(SkyDriveObject source) {
        return source.getDescription();
    }

    @Override
    public Long mapLastModified(SkyDriveObject source) {
        try {
            return mDateFormat.parse(source.getUpdatedTime())
                              .getTime();
        } catch (ParseException ignore) {
        }
        return null;
    }

    @Override
    public Integer mapIcon(SkyDriveObject source) {
        return null;
    }

    @Override
    public Integer mapFlags(SkyDriveObject source) {
        int flags = 0;

        flags |= DocumentsContract.Document.FLAG_SUPPORTS_WRITE;

        if (thatObjectIsDirectory(source)) {
            flags |= DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE;
        } else {
            flags |= DocumentsContract.Document.FLAG_SUPPORTS_WRITE;
        }

        flags |= DocumentsContract.Document.FLAG_SUPPORTS_DELETE;
        String mimeType = mapMimeType(source);

        if (mimeType.startsWith("image/")) {
            flags |= DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL;
        }

        return flags;
    }

    @Override
    public Long mapSize(SkyDriveObject source) {
        final long[] size = new long[1];
        source.accept(new SkyDriveObject.Visitor() {
            @Override
            public void visit(SkyDriveAlbum album) {
            }

            @Override
            public void visit(SkyDriveAudio audio) {
                size[0] = audio.getSize();
            }

            @Override
            public void visit(SkyDrivePhoto photo) {
                size[0] = photo.getSize();
            }

            @Override
            public void visit(SkyDriveFolder folder) {
            }

            @Override
            public void visit(SkyDriveFile file) {
                size[0] = file.getSize();
            }

            @Override
            public void visit(SkyDriveVideo video) {
                size[0] = video.getSize();
            }
        });
        return size[0];
    }

    private static boolean thatObjectIsDirectory(SkyDriveObject source) {
        final boolean[] dir = new boolean[1];
        source.accept(new SkyDriveObject.Visitor() {
            @Override
            public void visit(SkyDriveAlbum album) {
                dir[0] = true;
            }

            @Override
            public void visit(SkyDriveAudio audio) {
            }

            @Override
            public void visit(SkyDrivePhoto photo) {
            }

            @Override
            public void visit(SkyDriveFolder folder) {
                dir[0] = true;
            }

            @Override
            public void visit(SkyDriveFile file) {
            }

            @Override
            public void visit(SkyDriveVideo video) {
            }
        });
        return dir[0];
    }
}
