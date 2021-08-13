package com.kabouzeid.gramophone.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.MaterialDialog;
import com.kabouzeid.gramophone.R;

import kotlin.Unit;

public class RingtoneManager {


    public static boolean requiresDialog(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return !Settings.System.canWrite(context);
        }
        return false;
    }

    public static MaterialDialog showDialog(Context context) {
        MaterialDialog dialog = new MaterialDialog(context, MaterialDialog.getDEFAULT_BEHAVIOR());
        dialog.title(R.string.dialog_ringtone_title, null);
        dialog.message(R.string.dialog_ringtone_message, null, null);
        dialog.negativeButton(android.R.string.cancel, null, null);
        dialog.positiveButton(android.R.string.ok, null,
                (dialog1) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + context.getPackageName()));
                    context.startActivity(intent);
                    return null;
                }
        );
        dialog.show();
        return dialog;
//        return new MaterialDialog.Builder(context)
//                .title(R.string.dialog_ringtone_title)
//                .content)
//                .positiveText(android.R.string.ok)
//                .negativeText(android.R.string.cancel)
//                .onPositive((dialog, which) -> {
//                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
//                    intent.setData(Uri.parse("package:" + context.getPackageName()));
//                    context.startActivity(intent);
//                })
//                .show();
    }

    public   void setRingtone(@NonNull final Context context, final long id) {
        final ContentResolver resolver = context.getContentResolver();
        final Uri uri = MusicUtil.getSongFileUri(id);
        try {
            final ContentValues values = new ContentValues(2);
            values.put(MediaStore.Audio.AudioColumns.IS_RINGTONE, "1");
            values.put(MediaStore.Audio.AudioColumns.IS_ALARM, "1");
            resolver.update(uri, values, null, null);
        } catch (@NonNull final UnsupportedOperationException ignored) {
            return;
        }

        try {
            Cursor cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.MediaColumns.TITLE},
                    BaseColumns._ID + "=?",
                    new String[]{String.valueOf(id)},
                    null);
            try {
                if (cursor != null && cursor.getCount() == 1) {
                    cursor.moveToFirst();
                    Settings.System.putString(resolver, Settings.System.RINGTONE, uri.toString());
                    final String message = context.getString(R.string.x_has_been_set_as_ringtone, cursor.getString(0));
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } catch (SecurityException ignored) {
        }
    }
}
