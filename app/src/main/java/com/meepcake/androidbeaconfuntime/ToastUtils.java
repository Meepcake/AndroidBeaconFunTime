package com.meepcake.androidbeaconfuntime;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 * Created by JTsao on 7/8/2016.
 */
public class ToastUtils {

    public static void showToastInUiThread(final Context ctx,
                                           final int stringRes) {

        Handler mainThread = new Handler(Looper.getMainLooper());
        mainThread.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ctx, ctx.getString(stringRes), Toast.LENGTH_SHORT).show();
            }
        });
    }
}