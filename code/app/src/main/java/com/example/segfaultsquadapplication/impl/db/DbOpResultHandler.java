package com.example.segfaultsquadapplication.impl.db;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

/**
 * Organizes the logic used to handle task operation results.
 */
public class DbOpResultHandler<TResult> {
    private final OnSuccessListener<TResult> successListener;
    private final OnFailureListener failListener;

    /**
     * Creates an instance of result handler. </br>
     * To find out more detail about how success / failure would be triggered, see documentation for usages.
     * I.e. DbUtils.
     *
     * @param successListener The success callback, put null if want to be omitted
     * @param failListener    The failure callback, put null if want to be omitted
     */
    public DbOpResultHandler(@Nullable OnSuccessListener<TResult> successListener,
                             @Nullable OnFailureListener failListener) {
        this.successListener = successListener;
        this.failListener = failListener;
    }

    void onSuccess(TResult result) {
        if (successListener != null) {
            successListener.onSuccess(result);
        }
    }

    void onFailure(Exception exception) {
        if (failListener != null) {
            failListener.onFailure(exception);
        }
    }
}
