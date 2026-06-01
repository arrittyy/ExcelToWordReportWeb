package com.reportweb.util;

import org.apache.poi.xwpf.usermodel.XWPFDocument;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;

/**
 * Word 嵌入图片前压缩：POI 会把原始字节完整打进 docx，显示尺寸缩小并不能减小体积。
 * 此处将大图缩小最长边并优先转为 JPEG（无透明通道），以显著降低 docx 大小与下载时间。
 */
public final class WordEmbedImageUtil {

    /** 最长边像素上限（报告附图无需上万像素原图） */
    private static final int MAX_EDGE_PIXELS = 1920;
    /** 文件较小且分辨率不高时沿用原文件，避免无谓 CPU */
    private static final long SMALL_FILE_SKIP_BYTES = 80_000L;
    private static final int SMALL_IMAGE_MAX_EDGE = 900;
    private static final float JPEG_QUALITY = 0.82f;

    private WordEmbedImageUtil() {}

    public record PreparedPicture(InputStream stream, int poiPictureType, String embedFileName) implements AutoCloseable {
        @Override
        public void close() throws IOException {
            stream.close();
        }
    }

    /**
     * 报告附图、目视图等：大图缩小并压缩后嵌入。
     */
    public static PreparedPicture prepare(Path path, String originalFileName) throws IOException {
        Objects.requireNonNull(path, "path");
        String baseName = (originalFileName != null && !originalFileName.isBlank())
                ? originalFileName
                : path.getFileName().toString();

        if (!Files.isRegularFile(path)) {
            throw new IOException("Image file not found: " + path);
        }

        byte[] raw = Files.readAllBytes(path);
        long sz = raw.length;

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(raw));
        if (img == null) {
            return new PreparedPicture(new ByteArrayInputStream(raw), guessPictureType(baseName), baseName);
        }

        int origMaxEdge = Math.max(img.getWidth(), img.getHeight());
        boolean tinyPassThrough = sz <= SMALL_FILE_SKIP_BYTES && origMaxEdge <= SMALL_IMAGE_MAX_EDGE;
        if (tinyPassThrough) {
            return new PreparedPicture(new ByteArrayInputStream(raw), guessPictureType(baseName), baseName);
        }

        BufferedImage work = img;
        boolean scaled = origMaxEdge > MAX_EDGE_PIXELS;
        if (scaled) {
            work = scaleDown(work, MAX_EDGE_PIXELS);
        }

        boolean alpha = hasTransparency(work);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String embedName;
        int poiType;
        if (alpha) {
            ImageIO.write(work, "png", baos);
            embedName = replaceExtension(baseName, ".png");
            poiType = XWPFDocument.PICTURE_TYPE_PNG;
        } else {
            writeJpeg(work, baos, JPEG_QUALITY);
            embedName = replaceExtension(baseName, ".jpg");
            poiType = XWPFDocument.PICTURE_TYPE_JPEG;
        }

        byte[] out = baos.toByteArray();
        if (!scaled && out.length >= raw.length) {
            return new PreparedPicture(new ByteArrayInputStream(raw), guessPictureType(baseName), baseName);
        }
        return new PreparedPicture(new ByteArrayInputStream(out), poiType, embedName);
    }

    /**
     * 签名等小图：仅在体积大或分辨率过高时再压缩，避免糊掉常见小签名图。
     */
    public static PreparedPicture prepareSignature(Path path, String originalFileName) throws IOException {
        Objects.requireNonNull(path, "path");
        String baseName = originalFileName != null ? originalFileName : path.getFileName().toString();
        if (!Files.isRegularFile(path)) {
            throw new IOException("Image file not found: " + path);
        }
        byte[] raw = Files.readAllBytes(path);
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(raw));
        if (img == null) {
            return new PreparedPicture(new ByteArrayInputStream(raw), guessPictureType(baseName), baseName);
        }
        int maxEdge = Math.max(img.getWidth(), img.getHeight());
        if (raw.length <= 150_000 && maxEdge <= 512) {
            return new PreparedPicture(new ByteArrayInputStream(raw), guessPictureType(baseName), baseName);
        }
        return prepare(path, baseName);
    }

    private static boolean hasTransparency(BufferedImage img) {
        if (img.getColorModel() != null && img.getColorModel().hasAlpha()) {
            return true;
        }
        return img.getTransparency() != BufferedImage.OPAQUE;
    }

    private static BufferedImage scaleDown(BufferedImage src, int maxEdge) {
        int w = src.getWidth();
        int h = src.getHeight();
        int m = Math.max(w, h);
        if (m <= maxEdge) {
            return src;
        }
        double scale = maxEdge / (double) m;
        int nw = Math.max(1, (int) Math.round(w * scale));
        int nh = Math.max(1, (int) Math.round(h * scale));
        int type = hasTransparency(src) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage dst = new BufferedImage(nw, nh, type);
        Graphics2D g = dst.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(src, 0, 0, nw, nh, null);
        } finally {
            g.dispose();
        }
        return dst;
    }

    private static void writeJpeg(BufferedImage src, ByteArrayOutputStream os, float quality) throws IOException {
        BufferedImage rgb;
        if (src.getType() == BufferedImage.TYPE_INT_RGB) {
            rgb = src;
        } else {
            rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            try {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
                g.drawImage(src, 0, 0, null);
            } finally {
                g.dispose();
            }
        }
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            ImageIO.write(rgb, "jpg", os);
            return;
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(os)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(rgb, null, null), param);
        }
        writer.dispose();
    }

    private static int guessPictureType(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            return XWPFDocument.PICTURE_TYPE_JPEG;
        }
        String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case "png" -> XWPFDocument.PICTURE_TYPE_PNG;
            case "gif" -> XWPFDocument.PICTURE_TYPE_GIF;
            case "bmp" -> XWPFDocument.PICTURE_TYPE_BMP;
            default -> XWPFDocument.PICTURE_TYPE_JPEG;
        };
    }

    private static String replaceExtension(String name, String newExt) {
        int dot = name.lastIndexOf('.');
        if (dot <= 0) {
            return name + newExt;
        }
        return name.substring(0, dot) + newExt;
    }
}
