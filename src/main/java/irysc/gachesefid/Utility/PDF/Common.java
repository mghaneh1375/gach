package irysc.gachesefid.Utility.PDF;

import com.ibm.icu.text.Bidi;
import irysc.gachesefid.Utility.FileUtils;
import net.glxn.qrgen.javase.QRCode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.*;

import static irysc.gachesefid.Utility.StaticValues.DEV_MODE;
import static irysc.gachesefid.Utility.Utility.printException;

public class Common {

    public static String baseDir = (DEV_MODE) ? FileUtils.uploadDir_dev + "certificationsPDF/" :
            FileUtils.uploadDir + "certificationsPDF/";
    public static PDFont font;
    public static PDFont farsiFont;
    public static PDFont farsiFontBold;

    static void drawCommon(PDDocument document,
                           PDPageContentStream contentStream,
                           PDRectangle mediaBox) {

        try {
            drawBorder(contentStream, mediaBox);

            PDImageXObject image
                    = PDImageXObject.createFromFile(baseDir + "irysc.png", document);

            contentStream.drawImage(image, mediaBox.getUpperRightX() - 150, mediaBox.getUpperRightY() - 75, 100, 58);

//            PDImageXObject image2
//                    = PDImageXObject.createFromFile(baseDir + "b.png", document);
//
//            contentStream.drawImage(image2, 20, mediaBox.getUpperRightY() - 60, 98, 45);

        } catch (Exception ignore) {}
    }

    static void drawQR(PDDocument document,
                       PDPageContentStream contentStream,
                       int sx, int sy, int w, int w2,
                       String validateUrl) {

        try {

            ByteArrayOutputStream stream = QRCode
                    .from(validateUrl)
                    .withSize(w, w)
                    .stream();

            ByteArrayInputStream bis = new ByteArrayInputStream(stream.toByteArray());

            File outputFile = new File(baseDir + "image.jpg");
            ImageIO.write(ImageIO.read(bis), "jpg", outputFile);

            PDImageXObject image3
                    = PDImageXObject.createFromFile(outputFile.getAbsolutePath(), document);

            contentStream.drawImage(image3, sx, sy, w2, w2);
            outputFile.delete();

        } catch (IOException e) {
            printException(e);
        }
    }

    static String convertEnToPrNum(String num) {

        String[] arabicChars = new String[] {
                "\u0660", "\u0661", "\u0662", "\u0663", "\u0664","\u0665", "\u0666", "\u0667", "\u0668", "\u0669"
        };

        StringBuilder newStr = new StringBuilder();

        for(int i = 0; i < num.length(); i++) {
            newStr.append(arabicChars[Integer.parseInt(num.charAt(i) + "")]);
        }

        return newStr.toString();
    }

    static void myShowText(String text, PDPageContentStream contentStream,
                           PDRectangle mediaBox, int fontSize, float marginTop,
                           float marginRight, boolean isBold) {
        try {
            contentStream.beginText();
            contentStream.setFont((isBold) ? farsiFontBold : farsiFont, fontSize);
            centerText(text, (isBold) ? farsiFontBold : farsiFont, fontSize, contentStream, mediaBox, marginTop, marginRight);

            contentStream.showText(text);
        } catch (Exception ex) {
            System.out.println("HEY ERR");
            printException(ex);
        }
        finally {
            try {
                contentStream.endText();
            }
            catch (Exception e) {}
        }
    }

    static void myShowTextCenterWithOffsetFarsi(String text, PDPageContentStream contentStream,
                           PDRectangle mediaBox, int fontSize, float marginTop,
                           boolean isBold, float offsetRight) {
        try {
            contentStream.beginText();
            contentStream.setFont((isBold) ? farsiFontBold : farsiFont, fontSize);

            float titleWidth = isBold ?
                    farsiFontBold.getStringWidth(text) / 1000 * fontSize :
                    farsiFont.getStringWidth(text) / 1000 * fontSize;

            contentStream.newLineAtOffset((mediaBox.getWidth() - offsetRight - titleWidth) / 2, mediaBox.getHeight() - marginTop);
            contentStream.showText(text);

        } catch (Exception ex) {
            System.out.println("HEY ERR");
            printException(ex);
        }
        finally {
            try {
                contentStream.endText();
            }
            catch (Exception e) {}
        }
    }


    static void centerText(String text, PDFont font, int fontSize,
                           PDPageContentStream stream, PDRectangle mediaBox,
                           float marginTop, float marginRight) {
        try {
            if (marginRight == -1) {
                float titleWidth = font.getStringWidth(text) / 1000 * fontSize;
                stream.newLineAtOffset((mediaBox.getWidth() - titleWidth) / 2, mediaBox.getHeight() - marginTop);
            } else if (marginRight == -2) {
                float titleWidth = font.getStringWidth(text) / 1000 * fontSize;
                stream.newLineAtOffset((mediaBox.getWidth() / 2 - titleWidth) / 2, mediaBox.getHeight() - marginTop);
            } else if (marginRight == -3) {
                float titleWidth = font.getStringWidth(text) / 1000 * fontSize;
                stream.newLineAtOffset((mediaBox.getWidth() / 2 - titleWidth) / 2 + mediaBox.getWidth() / 2, mediaBox.getHeight() - marginTop);
            } else if (marginRight == -4) {
                float titleWidth = font.getStringWidth(text) / 1000 * fontSize;
                stream.newLineAtOffset((mediaBox.getWidth() / 3 - titleWidth) / 2, mediaBox.getHeight() - marginTop);
            } else if (marginRight == -5) {
                float titleWidth = font.getStringWidth(text) / 1000 * fontSize;
                stream.newLineAtOffset((mediaBox.getWidth() / 3 - titleWidth) / 2 + mediaBox.getWidth() / 3, mediaBox.getHeight() - marginTop);
            } else if (marginRight == -6) {
                float titleWidth = font.getStringWidth(text) / 1000 * fontSize;
                stream.newLineAtOffset((mediaBox.getWidth() / 3 - titleWidth) / 2 + 2 * mediaBox.getWidth() / 3, mediaBox.getHeight() - marginTop);
            } else
                stream.newLineAtOffset(mediaBox.getWidth() - marginRight, mediaBox.getHeight() - marginTop);
        } catch (IOException e) {
            printException(e);
        }
    }

    static void drawBorder(PDPageContentStream contentStream, PDRectangle mediaBox) {
        try {
            contentStream.setStrokingColor(new Color(1, 50, 67));
            contentStream.setLineWidth((float) 0.4);
            contentStream.addRect(10, 10, mediaBox.getWidth() - 20, mediaBox.getHeight() - 20);
            contentStream.stroke();

            contentStream.setLineWidth((float) 1.3);
            contentStream.addRect(12, 12, mediaBox.getWidth() - 24, mediaBox.getHeight() - 24);
            contentStream.stroke();

            contentStream.setLineWidth((float) 0.4);
            contentStream.addRect(14, 14, mediaBox.getWidth() - 28, mediaBox.getHeight() - 28);
            contentStream.stroke();
        } catch (Exception x) {
            printException(x);
        }
    }

    static String bidiReorder(String text) {
        try {
            text = Tools.fa(text);
//            (new ArabicShaping(ArabicShaping.LETTERS_SHAPE)).shape(text)
            Bidi bidi = new Bidi(text, 127);
            bidi.setReorderingMode(0);
            return bidi.writeReordered(2);
        } catch (Exception ase3) {
            return text;
        }
    }
}
