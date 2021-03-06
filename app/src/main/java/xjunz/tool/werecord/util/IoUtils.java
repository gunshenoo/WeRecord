/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.util;

import androidx.annotation.NonNull;

import org.apaches.commons.codec.DecoderException;
import org.apaches.commons.codec.binary.Hex;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

import xjunz.tool.werecord.App;

public class IoUtils {

    public static void transferStream(@NotNull InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[10 * 1024];
        int count;
        while ((count = in.read(buffer)) != -1) {
            out.write(buffer, 0, count);
        }
        out.flush();
        in.close();
        out.close();
    }

    public static void transferFileViaChannel(@NotNull FileInputStream in, @NotNull FileOutputStream out) throws IOException {
        FileChannel inChannel = in.getChannel();
        FileChannel outChannel = out.getChannel();
        inChannel.transferTo(0, in.available(), outChannel);
        inChannel.close();
        outChannel.close();
    }

    public static void transferStreamNoCloseOutStream(@NotNull InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[10 * 1024];
        int count;
        while ((count = in.read(buffer)) != -1) {
            out.write(buffer, 0, count);
        }
        in.close();
    }


    public static void serializeToStorage(Object obj, String path) {
        try {
            FileOutputStream fos = new FileOutputStream(path);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.close();
            byte[] bytes = bos.toByteArray();
            String hex = String.valueOf(Hex.encodeHex(bytes));
            bos.close();
            ByteArrayInputStream bis = new ByteArrayInputStream(hex.getBytes());
            transferStream(bis, fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    public static String serializeToString(Object obj) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.close();
            byte[] bytes = bos.toByteArray();
            String hex = String.valueOf(Hex.encodeHex(bytes));
            bos.close();
            return hex;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Nullable
    public static <T> T deserializeFromStorage(String path, @NotNull Class<T> t) {
        try {
            FileInputStream fis = new FileInputStream(path);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            transferStream(fis, bos);
            byte[] decoded = Hex.decodeHex(bos.toString());
            ByteArrayInputStream bis = new ByteArrayInputStream(decoded);
            ObjectInputStream ois = new ObjectInputStream(bis);
            bis.close();
            ois.close();
            try {
                return t.cast(ois.readObject());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException | DecoderException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    public static <T> T deserializeFromString(String src, @NotNull Class<T> t) {
        try {
            byte[] decoded = Hex.decodeHex(src);
            ByteArrayInputStream bis = new ByteArrayInputStream(decoded);
            ObjectInputStream ois = new ObjectInputStream(bis);
            bis.close();
            ois.close();
            try {
                return t.cast(ois.readObject());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException | DecoderException e) {
            e.printStackTrace();
        }
        return null;
    }

    @NotNull
    @Contract("_ -> new")
    public static String readStackTraceFromThrowable(@NotNull Throwable e) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(out);
        e.printStackTrace(stream);
        return new String(out.toByteArray());
    }

    public static void deleteFile(@NotNull File file) {
        if (!file.delete()) {
            LogUtils.error("Failed to delete file: " + file.getPath());
        }
    }

    public static void deleteFileSync(@NonNull File file) {
        new Thread(() -> deleteFile(file)).start();
    }

    @NotNull
    public static String readAssetAsString(String assetName) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        IoUtils.transferStream(App.getContext().getAssets().open(assetName), outputStream);
        return outputStream.toString(StandardCharsets.UTF_8.name());
    }
}
