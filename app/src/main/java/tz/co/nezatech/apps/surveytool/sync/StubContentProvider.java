package tz.co.nezatech.apps.surveytool.sync;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by nkayamba on 2/7/18.
 */


public class StubContentProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull final Uri uri, final String[] projection, final String selection,
                        final String[] selectionArgs,
                        final String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull final Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull final Uri uri, final ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull final Uri uri, final String selection,
                      final String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull final Uri uri, final ContentValues values, final String selection,
                      final String[] selectionArgs) {
        return 0;
    }
}
