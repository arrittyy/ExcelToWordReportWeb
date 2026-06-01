package com.reportweb.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 将独立 docx 的正文段落/表格追加到目标文档；将 PDF 按页渲染为图片插入 Word。
 */
public final class SummaryWordAppendUtil {
    private SummaryWordAppendUtil() {
    }

    public static void appendDocxBody(XWPFDocument target, Path docxPath) throws Exception {
        try (InputStream in = Files.newInputStream(docxPath);
             XWPFDocument source = new XWPFDocument(in)) {
            for (org.apache.poi.xwpf.usermodel.IBodyElement el : source.getBodyElements()) {
                if (el instanceof XWPFParagraph) {
                    XWPFParagraph sp = (XWPFParagraph) el;
                    XWPFParagraph dp = target.createParagraph();
                    org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP copiedParagraph =
                            (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP) sp.getCTP().copy();
                    stripParagraphSectionProperties(copiedParagraph);
                    dp.getCTP().set(copiedParagraph);
                } else if (el instanceof XWPFTable) {
                    XWPFTable st = (XWPFTable) el;
                    XWPFTable dt = target.createTable(1, 1);
                    org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl copiedTable =
                            (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl) st.getCTTbl().copy();
                    stripTableSectionProperties(copiedTable);
                    dt.getCTTbl().set(copiedTable);
                }
            }
        }
    }

    private static void stripParagraphSectionProperties(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP paragraph) {
        if (paragraph == null || !paragraph.isSetPPr()) {
            return;
        }
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr pPr = paragraph.getPPr();
        if (pPr.isSetSectPr()) {
            pPr.unsetSectPr();
        }
    }

    private static void stripTableSectionProperties(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl table) {
        if (table == null) {
            return;
        }
        for (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow row : table.getTrArray()) {
            for (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTc cell : row.getTcArray()) {
                for (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP paragraph : cell.getPArray()) {
                    stripParagraphSectionProperties(paragraph);
                }
                for (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl nestedTable : cell.getTblArray()) {
                    stripTableSectionProperties(nestedTable);
                }
            }
        }
    }

    /**
     * 将 PDF 每页渲染为图片插入文档（约 150 DPI，避免过大）。
     */
    public static void appendPdfPagesAsImages(XWPFDocument target, Path pdfPath) throws Exception {
        try (PDDocument pdf = Loader.loadPDF(pdfPath.toFile())) {
            PDFRenderer renderer = new PDFRenderer(pdf);
            int pages = pdf.getNumberOfPages();
            for (int i = 0; i < pages; i++) {
                if (i > 0) {
                    XWPFParagraph br = target.createParagraph();
                    br.createRun().addBreak(BreakType.PAGE);
                }
                BufferedImage image = renderer.renderImageWithDPI(i, 150, ImageType.RGB);
                ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
                ImageIO.write(image, "png", pngOut);
                byte[] pngBytes = pngOut.toByteArray();

                XWPFParagraph p = target.createParagraph();
                p.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun run = p.createRun();
                int w = image.getWidth();
                int h = image.getHeight();
                int maxW = Units.toEMU(450);
                int emuW = maxW;
                int emuH = (int) ((long) maxW * h / w);
                try (ByteArrayInputStream bis = new ByteArrayInputStream(pngBytes)) {
                    run.addPicture(bis, XWPFDocument.PICTURE_TYPE_PNG, "page" + (i + 1) + ".png", emuW, emuH);
                }
            }
        }
    }

    public static void addSectionTitle(XWPFDocument document, String title) {
        XWPFParagraph p = document.createParagraph();
        p.setSpacingAfter(200);
        XWPFRun r = p.createRun();
        r.setBold(true);
        r.setFontFamily("宋体");
        r.setFontSize(14);
        r.setText(title);
    }
}
