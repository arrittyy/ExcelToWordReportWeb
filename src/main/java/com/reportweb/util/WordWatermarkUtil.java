package com.reportweb.util;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPicture;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;

import java.io.IOException;
import java.io.InputStream;

/**
 * 总报告正文节页眉水印：将平铺 PNG 以 VML 衬于文字下方铺满页边距区域。
 */
public final class WordWatermarkUtil {

    private static final String WATERMARK_RESOURCE = "/word/rundian-watermark.png";

    /**
     * A4 正文可印区域近似尺寸（pt），相对页边距居中。
     */
    private static final String VML_SHAPE_STYLE =
            "position:absolute;margin-left:0;margin-top:0;width:467.5pt;height:669.5pt;"
                    + "z-index:-251657216;mso-position-horizontal:center;"
                    + "mso-position-horizontal-relative:margin;mso-position-vertical:center;"
                    + "mso-position-vertical-relative:margin";

    private WordWatermarkUtil() {}

    public static void appendSummaryWatermark(XWPFHeader header) throws IOException {
        byte[] imageBytes = loadWatermarkBytes();
        String relId;
        try {
            relId = header.addPictureData(imageBytes, XWPFDocument.PICTURE_TYPE_PNG);
        } catch (InvalidFormatException e) {
            throw new IOException("Failed to register watermark image", e);
        }
        long shapeId = System.nanoTime() % 1_000_000_000L;

        XWPFParagraph paragraph = header.createParagraph();
        XWPFRun run = paragraph.createRun();
        CTR ctr = run.getCTR();
        CTPicture pict = ctr.addNewPict();
        try {
            pict.set(XmlObject.Factory.parse(buildShapeXml(relId, shapeId)));
        } catch (XmlException e) {
            throw new IOException("Failed to build watermark VML", e);
        }
    }

    private static byte[] loadWatermarkBytes() throws IOException {
        try (InputStream in = WordWatermarkUtil.class.getResourceAsStream(WATERMARK_RESOURCE)) {
            if (in == null) {
                throw new IOException("Watermark resource not found: " + WATERMARK_RESOURCE);
            }
            return in.readAllBytes();
        }
    }

    private static String buildShapeXml(String relId, long shapeId) {
        return "<v:shape xmlns:v=\"urn:schemas-microsoft-com:vml\""
                + " xmlns:o=\"urn:schemas-microsoft-com:office:office\""
                + " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\""
                + " id=\"WordPictureWatermark" + shapeId + "\""
                + " o:spid=\"_x0000_s" + shapeId + "\""
                + " type=\"#_x0000_t75\""
                + " style=\"" + VML_SHAPE_STYLE + "\""
                + " o:allowincell=\"f\""
                + " stroked=\"f\">"
                + "<v:imagedata r:id=\"" + relId + "\" o:title=\"rundian-watermark\"/>"
                + "</v:shape>";
    }
}
