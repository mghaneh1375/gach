package irysc.gachesefid.Utility.PDF;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import irysc.gachesefid.Exception.InvalidFieldsException;
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
import java.net.URL;

import static irysc.gachesefid.Utility.StaticValues.DEV_MODE;

public class Common {

    public static String baseDir = (DEV_MODE) ? FileUtils.uploadDir_dev + "certificationsPDF/" :
            FileUtils.uploadDir + "certificationsPDF/";
    public static PDFont font;
    public static PDFont farsiFont;
    public static PDFont farsiFontBold;
    public static PDFont fontBold;

    static void drawCommon(PDDocument document,
                           PDPageContentStream contentStream,
                           PDRectangle mediaBox) {

        try {
            drawBorder(contentStream, mediaBox);

            PDImageXObject image
                    = PDImageXObject.createFromFile(baseDir + "a.png", document);

            contentStream.drawImage(image, mediaBox.getUpperRightX() - 150, mediaBox.getUpperRightY() - 60, 130, 45);

            PDImageXObject image2
                    = PDImageXObject.createFromFile(baseDir + "b.png", document);

            contentStream.drawImage(image2, 20, mediaBox.getUpperRightY() - 60, 98, 45);
        } catch (Exception ignore) {
        }
    }

    static File saveImage(String imageUrl, String destinationFile) throws IOException, InvalidFieldsException {

        URL url = new URL(imageUrl);
        InputStream is = url.openStream();

        File f = new File(destinationFile);

        if (!f.createNewFile())
            throw new InvalidFieldsException("can not create file");

        OutputStream os = new FileOutputStream(f);

        byte[] b = new byte[2048];
        int length;

        while ((length = is.read(b)) != -1) {
            os.write(b, 0, length);
        }

        is.close();
        os.close();

        return f;
    }

    static void drawQR(PDDocument document,
                       PDPageContentStream contentStream,
                       PDRectangle mediaBox,
                       int sx, int sy, int w, int w2,
                       String validateUrl) {

        try {

            if (sx != -1 && sy != -1) {
                float marginTop = mediaBox.getHeight() - sx;
                myShowText("Der QR-Code dient zur Überprüfung dieses Dokuments.", contentStream, mediaBox, 8, marginTop, mediaBox.getWidth() - 85, false);
                marginTop = mediaBox.getHeight() - sy;
                myShowText("Es ist zu beachten, dass die Verifizierungsdomain mit ,,okft.org\" ist.", contentStream, mediaBox, 8, marginTop, mediaBox.getWidth() - 85, false);
            }

            ByteArrayOutputStream stream = QRCode
                    .from(validateUrl)
                    .withSize(w, w)
                    .stream();

            ByteArrayInputStream bis = new ByteArrayInputStream(stream.toByteArray());

            File outputFile = new File(baseDir + "image.jpg");
            ImageIO.write(ImageIO.read(bis), "jpg", outputFile);

            PDImageXObject image3
                    = PDImageXObject.createFromFile(outputFile.getAbsolutePath(), document);

            contentStream.drawImage(image3, 25, sy == -1 ? 25 : 15, w2, w2);
            outputFile.delete();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static float drawLevelTable(PDPageContentStream contentStream,
                                PDRectangle mediaBox,
                                float marginTop) {

        myShowText("Elementar", contentStream, mediaBox, 12, marginTop, -4, true);
        myShowText("Selbstständige", contentStream, mediaBox, 12, marginTop, -5, true);
        myShowText("Kompetente", contentStream, mediaBox, 12, marginTop, -6, true);

        marginTop += 20;
        myShowText("Sprachverwendung", contentStream, mediaBox, 12, marginTop, -4, false);
        myShowText("Sprachverwendung", contentStream, mediaBox, 12, marginTop, -5, false);
        myShowText("Sprachverwendung", contentStream, mediaBox, 12, marginTop, -6, false);

        marginTop += 20;
        myShowText("A 1", contentStream, mediaBox, 12, marginTop, -4, false);
        myShowText("B 1/1", contentStream, mediaBox, 12, marginTop, -5, false);
        myShowText("C 1/1", contentStream, mediaBox, 12, marginTop, -6, false);

        marginTop += 20;
        myShowText("A 2/1", contentStream, mediaBox, 12, marginTop, -4, false);
        myShowText("B 1/2", contentStream, mediaBox, 12, marginTop, -5, false);
        myShowText("C 1/2", contentStream, mediaBox, 12, marginTop, -6, false);

        marginTop += 20;
        myShowText("A 2/2", contentStream, mediaBox, 12, marginTop, -4, false);
        myShowText("B 2/1", contentStream, mediaBox, 12, marginTop, -5, false);
        myShowText("C 1/3", contentStream, mediaBox, 12, marginTop, -6, false);

        marginTop += 20;
        myShowText(" ", contentStream, mediaBox, 12, marginTop, -4, false);
        myShowText("B 2/2", contentStream, mediaBox, 12, marginTop, -5, false);
        myShowText("C 1/4", contentStream, mediaBox, 12, marginTop, -6, false);

        marginTop += 20;
        myShowText(" ", contentStream, mediaBox, 12, marginTop, -4, false);
        myShowText("B 2/3", contentStream, mediaBox, 12, marginTop, -5, false);
        myShowText("C 2", contentStream, mediaBox, 12, marginTop, -6, false);

        return marginTop;
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
            ex.printStackTrace();
        }
        finally {
            try {
                contentStream.endText();
            }
            catch (Exception e) {}
        }
    }

    static void myShowTextInHalf(String text, PDPageContentStream contentStream,
                                 PDRectangle mediaBox, int fontSize, float marginTop,
                                 float marginLeftOrRight, boolean isLeft,
                                 PDFont font) {
        try {

            float titleWidth = font.getStringWidth(text) / 1000 * fontSize;

            if (isLeft)
                marginLeftOrRight = marginLeftOrRight == -1 ?
                        ((mediaBox.getWidth() - 40) / 2 - titleWidth) / 2 :
                        (mediaBox.getWidth() - 40) / 2 - titleWidth + marginLeftOrRight;
            else
                marginLeftOrRight = marginLeftOrRight == -1 ?
                        (mediaBox.getWidth() - 40) / 2 + 20 + ((mediaBox.getWidth() - 40) / 2 - titleWidth) / 2 :
                        mediaBox.getWidth() - titleWidth - marginLeftOrRight - 30;

            contentStream.beginText();
            contentStream.setFont(font, fontSize);

            contentStream.newLineAtOffset(marginLeftOrRight, mediaBox.getHeight() - marginTop);

            contentStream.showText(text);
            contentStream.endText();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static void myShowTextInline(String text, PDPageContentStream contentStream,
                                 int fontSize, boolean isBold) {
        try {
            contentStream.setFont((isBold) ? fontBold : font, fontSize);
            contentStream.showText(text);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static void myShowTextInlineFarsi(String text, PDPageContentStream contentStream,
                                      int fontSize, boolean isBold) {
        try {
            contentStream.setFont((isBold) ? farsiFontBold : farsiFont, fontSize);
            contentStream.showText(text);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static void myShowTextInline2(String text, PDPageContentStream contentStream,
                                  int fontSize, boolean isBold, float sx, float sy) {
        try {
            contentStream.setFont((isBold) ? fontBold : font, fontSize);
            float stringWidth = fontSize * font.getStringWidth(text) / 1000;
            float lineEndPoint = sx + stringWidth;

            contentStream.setStrokingColor(Color.BLACK);

            //begin to draw our line
            contentStream.setLineWidth(1);
            contentStream.moveTo(sx, sy - 2);
            contentStream.lineTo(lineEndPoint, sy - 2);
            contentStream.stroke();

        } catch (Exception ex) {
            ex.printStackTrace();
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
            e.printStackTrace();
        }
    }

    static void drawBorder(PDPageContentStream contentStream, PDRectangle mediaBox) {
        try {
            contentStream.setStrokingColor(Color.RED);
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
            x.printStackTrace();
        }
    }

    static void drawHalfBorder(PDPageContentStream contentStream, PDRectangle mediaBox, boolean isLeft) {

        try {
            contentStream.setStrokingColor(Color.RED);
            contentStream.setLineWidth((float) 1.3);
            if (isLeft)
                contentStream.addRect(10, 10, (mediaBox.getWidth() - 40) / 2, mediaBox.getHeight() - 20);
            else
                contentStream.addRect((mediaBox.getWidth() - 40) / 2 + 20, 10, (mediaBox.getWidth() - 40) / 2, mediaBox.getHeight() - 20);

            contentStream.stroke();
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    static String capitalize(String str) {

        if (str == null || str.isEmpty())
            return str;

        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    static String bidiReorder(String text) {
        try {
            text = Tools.fa(text);
            Bidi bidi = new Bidi((new ArabicShaping(ArabicShaping.LETTERS_SHAPE)).shape(text), 127);
            bidi.setReorderingMode(0);
            return bidi.writeReordered(2);

        } catch (ArabicShapingException ase3) {
            return text;
        }
    }

    static void drawCheckBox(PDPageContentStream contentStream, PDRectangle mediaBox, float marginTop,
                             String latin, String farsi, String answer,
                             Boolean isFill, boolean bold, int tx) throws Exception {

        if(isFill != null) {
            contentStream.setStrokingColor(Color.BLACK);
            contentStream.setLineWidth((float) 0.6);
            contentStream.addRect(tx, mediaBox.getHeight() - marginTop - 3, 14, 14);

            if (isFill) {
                contentStream.setNonStrokingColor(Color.BLACK);
                contentStream.fill();
            } else
                contentStream.stroke();
        }

        contentStream.beginText();
        contentStream.newLineAtOffset(isFill == null ? tx : tx + 20, mediaBox.getHeight() - marginTop);

        if(latin != null)
            myShowTextInline(latin, contentStream, 12, bold);

        if(farsi != null)
            myShowTextInlineFarsi(bidiReorder(farsi), contentStream, 11, false);

        if(answer != null)
            myShowTextInlineFarsi(bidiReorder(answer), contentStream, 11, false);

        contentStream.endText();
    }
}
