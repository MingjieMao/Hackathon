package com.example.myapplication;

import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

final class ImageStorage {
    private static final String IMAGE_DIRECTORY = "image_attachments";

    private ImageStorage() {
    }

    static Uri copyToLocalImage(Context context, Uri sourceUri) throws IOException {
        String extension = resolveExtension(context, sourceUri);
        File directory = new File(context.getFilesDir(), IMAGE_DIRECTORY);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Could not create image attachment directory.");
        }

        File destination = new File(directory, UUID.randomUUID() + "." + extension);
        try (InputStream input = context.getContentResolver().openInputStream(sourceUri);
             FileOutputStream output = new FileOutputStream(destination)) {
            if (input == null) {
                throw new IOException("Could not open selected image.");
            }
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        }
        return Uri.fromFile(destination);
    }

    private static String resolveExtension(Context context, Uri sourceUri) {
        String mimeType = context.getContentResolver().getType(sourceUri);
        String extension = mimeType == null ? null : MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        if (extension == null || extension.isEmpty()) {
            extension = MimeTypeMap.getFileExtensionFromUrl(sourceUri.toString());
        }
        return extension == null || extension.isEmpty() ? "jpg" : extension;
    }
}
