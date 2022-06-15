package com.touchain.ipfs.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import java.io.File;


public class IpfsFileUtils {

    public static String findFile(String keyword) {
        String path = PathUtil.getIpfsPath();
        File[] files = new File(path).listFiles();
        if (files == null) {
            return null;
        }
        for (File file : files) {
            if (file.getName().contains(keyword)) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }

    public static String convertFileType(String fileType) {
        if(fileType == null){
            return null;
        }
        switch (fileType) {

            //图片类
            case "image/jpeg":
                return ".jpeg";

            case "image/jpg":
                return ".jpg";

            case "image/png":
                return ".png";

            case "image/gif":
                return ".gif";

            case "image/tiff":
                return ".tif";

            case "image/bmp":
                return ".bmp";

            case "image/x-icon":
                return ".ico";

            case "image/webp":
                return ".webp";

            //文本类
            case "text/plain":
                return ".txt";

            case "text/rtf":
                return ".txt";

            case "application/pdf":
                return ".pdf";

            case "text/html":
                return ".html";

            case "application/msword":
                return ".doc";

            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                return ".docx";

            case "application/vnd.android.package-archive":
                return ".apk";

            case "application/vnd.ms-works":
                return ".wps";

            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
                return ".xlsx";

            case "text/css":
                return ".css";

            case "application/json":
                return ".json";

            case "application/zip":
                return ".zip";

            case "application/vnd.ms-powerpoint":
                return ".ppt";

            case "application/vnd.openxmlformats-officedocument.presentationml.presentation":
                return ".pptx";

            case "application/x-shockwave-flash":
                return ".swf";

            case "application/x-bittorrent":
                return ".torrent";

            //音频类
            case "audio/x-aac":
                return ".aac";

            case "audio/mpeg":
                return ".mp3";

            case "audio/mp4a-latm":
                return ".m4a";

            case "audio/flac":
                return ".flac";

            case "audio/midi":
                return ".mid";

            case "audio/x-wav":
                return ".wav";

            case "audio/ogg":
                return ".ogg";

            case "audio/x-ms-wma":
                return ".wma";

            //视频类
            case "video/x-flv":
                return ".flv";

            case "video/x-f4v":
                return ".f4v";

            case "video/x-m4v'":
                return ".m4v";

            case "video/quicktime":
                return ".mov";

            case "application/vnd.rn-realmedia-vbr":
                return ".rmvb";

            case "application/x-msmediaview":
                return ".mvb";

            case "application/vnd.rn-realmedia":
                return ".rm";

            case "video/x-ms-wmv":
                return ".wmv";

            case "video/mp4":
                return ".mp4";

            case "video/x-matroska":
                return ".mkv";

            case "video/webm":
                return ".webm";

            case "video/3gpp":
                return ".3gp";

            case "video/x-msvideo":
                return ".avi";

        }

        if (fileType.startsWith("image")) {
            return ".jpg";
        } else if (fileType.startsWith("video")) {
            return ".mp4";
        }

        return ".txt";
    }

    public static boolean isImage(String filename) {
        return filename.endsWith(".jpeg")
                || filename.endsWith(".jpg")
                || filename.endsWith(".png")
                || filename.endsWith(".bmp")
                || filename.endsWith(".gif")
                || filename.endsWith(".tiff")
                || filename.endsWith(".ai")
                || filename.endsWith(".cdr")
                || filename.endsWith(".eps");
    }

    public static boolean isFile(String filename) {
        return filename.endsWith(".txt")
                || filename.endsWith(".doc")
                || filename.endsWith(".json")
                || filename.endsWith(".docx")
                || filename.endsWith(".xlsx")
                || filename.endsWith(".torrent")
                || filename.endsWith(".pdf")
                || filename.endsWith(".tif")
                || filename.endsWith(".wps")
                || filename.endsWith(".pptx")
                || filename.endsWith(".ppt");
    }

    public static boolean isMusic(String filename) {

        return filename.endsWith(".mp3")
                || filename.endsWith(".aac")
                || filename.endsWith(".m4a")
                || filename.endsWith(".flac")
                || filename.endsWith(".mid")
                || filename.endsWith(".ogg")
                || filename.endsWith(".wma")
                || filename.endsWith(".pcm")
                || filename.endsWith(".wav")
                || filename.endsWith(".aiff")
                || filename.endsWith(".alac");
    }

    public static boolean isVideo(String filename) {

        if(filename == null){
            return false;
        }

        return filename.endsWith(".mp4")
                || filename.endsWith(".flv")
                || filename.endsWith(".f4v")
                || filename.endsWith(".m4v")
                || filename.endsWith(".mov")
                || filename.endsWith(".rmvb")
                || filename.endsWith(".mvb")
                || filename.endsWith(".rm")
                || filename.endsWith(".wmv")
                || filename.endsWith(".mkv")
                || filename.endsWith(".webm")
                || filename.endsWith(".3gp")
                || filename.endsWith(".avi");
    }


    public static void copy(Context context, String text) {
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText("Label", text);
        clipboardManager.setPrimaryClip(clipData);
    }
}
