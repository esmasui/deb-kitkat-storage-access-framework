
package com.uphyca.kitkat.storage;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.test.AndroidTestCase;

/**
 * Created by masui on 11/8/13.
 */
public class ParcelFileDescriptorTest extends AndroidTestCase {

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public void testThatPipeShouldWorkProperly() throws Exception {
        final String givenData = "Hello, ParcelFileDescriptor.";
        final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        final ParcelFileDescriptor source = pipe[0];
        final ParcelFileDescriptor sink = pipe[1];
        final InputStream in = new FileInputStream(source.getFileDescriptor());
        final OutputStream out = new ByteArrayOutputStream();

        new AsyncTask<ParcelFileDescriptor, Void, Void>() {
            @Override
            protected Void doInBackground(ParcelFileDescriptor... params) {
                //Simulates long periodic network operation
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ignore) {
                }
                ParcelFileDescriptor pfd = params[0];
                FileDescriptor fd = pfd.getFileDescriptor();
                FileOutputStream out = new FileOutputStream(fd);
                byte[] data = givenData.getBytes();
                try {
                    out.write(data, 0, data.length);
                    out.close();
                    pfd.close();
                } catch (IOException ignore) {
                }
                return null;
            }
        }.execute(sink);

        final byte[] buffer = new byte[8192];
        for (int count; (count = in.read(buffer)) > -1;) {
            out.write(buffer, 0, count);
        }

        assertThat(out.toString()).isEqualTo(givenData);
    }
}
