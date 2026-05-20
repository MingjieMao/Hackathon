package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

final class ImageAttachmentViewer {
    private ImageAttachmentViewer() {
    }

    static void show(Context context, Uri imageUri, int titleResId) {
        ImageView preview = new ImageView(context);
        preview.setAdjustViewBounds(true);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setImageURI(imageUri);
        int padding = Math.round(12 * context.getResources().getDisplayMetrics().density);
        preview.setPadding(0, padding, 0, padding);

        new MaterialAlertDialogBuilder(context)
                .setTitle(titleResId)
                .setView(preview)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save_image, (dialog, which) -> save(context, imageUri))
                .show();
    }

    private static void save(Context context, Uri sourceUri) {
        String mimeType = context.getContentResolver().getType(sourceUri);
        if (mimeType == null) {
            mimeType = "image/jpeg";
        }

        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        if (extension == null || extension.isEmpty()) {
            extension = "jpg";
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "campus_image_" + System.currentTimeMillis() + "." + extension);
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CampusModeration");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        Uri destinationUri = null;
        try {
            destinationUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (destinationUri == null) {
                throw new IOException("Could not create image destination.");
            }

            try (InputStream input = context.getContentResolver().openInputStream(sourceUri);
                 OutputStream output = context.getContentResolver().openOutputStream(destinationUri)) {
                if (input == null || output == null) {
                    throw new IOException("Could not open image streams.");
                }
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues completedValues = new ContentValues();
                completedValues.put(MediaStore.Images.Media.IS_PENDING, 0);
                context.getContentResolver().update(destinationUri, completedValues, null, null);
            }
            Toast.makeText(context, R.string.toast_image_saved, Toast.LENGTH_SHORT).show();
        } catch (IOException | SecurityException exception) {
            if (destinationUri != null) {
                context.getContentResolver().delete(destinationUri, null, null);
            }
            Toast.makeText(context, R.string.toast_image_save_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
