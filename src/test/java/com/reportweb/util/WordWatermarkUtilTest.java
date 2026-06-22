package com.reportweb.util;

import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WordWatermarkUtilTest {

    @Test
    void appendSummaryWatermark_addsVmlImageDataToHeader() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
            XWPFHeaderFooterPolicy policy = new XWPFHeaderFooterPolicy(document, sectPr);
            XWPFHeader header = policy.createHeader(XWPFHeaderFooterPolicy.DEFAULT);

            WordWatermarkUtil.appendSummaryWatermark(header);

            String headerXml = header.getParagraphs().get(0).getCTP().xmlText();
            assertTrue(headerXml.contains("imagedata"), "header should contain VML imagedata");
            assertTrue(headerXml.contains("WordPictureWatermark"), "header should contain watermark shape id");
            assertTrue(headerXml.contains("r:id="), "header should reference embedded image relationship");
            assertTrue(header.getParagraphs().size() >= 1, "header should have watermark paragraph");
        }
    }

    @Test
    void appendSummaryWatermark_persistsInSavedDocxHeaderPart() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
            XWPFHeaderFooterPolicy policy = new XWPFHeaderFooterPolicy(document, sectPr);
            XWPFHeader header = policy.createHeader(XWPFHeaderFooterPolicy.DEFAULT);
            WordWatermarkUtil.appendSummaryWatermark(header);
            document.createParagraph().createRun().setText("body");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);

            boolean headerHasWatermark = false;
            try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(out.toByteArray()))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.getName().startsWith("word/header") && entry.getName().endsWith(".xml")) {
                        String xml = new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        if (xml.contains("WordPictureWatermark") && xml.contains("imagedata")) {
                            headerHasWatermark = true;
                            break;
                        }
                    }
                }
            }
            assertTrue(headerHasWatermark, "saved docx header part should contain watermark VML");
        }
    }
}
