package irysc.gachesefid.Utility;

import net.glxn.qrgen.javase.QRCode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.bson.Document;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static irysc.gachesefid.Utility.StaticValues.DEV_MODE;


public class PDFUtils {

    private static PDFont font;
    private static PDFont fontBold;
    private static String baseDir = (DEV_MODE) ? FileUtils.uploadDir_dev + "certificationsPDF/" :
            FileUtils.uploadDir + "certificationsPDF/";

    public static File generateParticipationPDF(Document user, Document theClass) {

        String code = theClass.getObjectId("_id").toString() + "$$$" + user.getObjectId("_id").toString();

        File f = new File(baseDir + code + ".pdf");
        if (f.exists())
            return f;

        PDDocument document = new PDDocument();
        try {
            font = PDType0Font.load(document, new File(baseDir + "arialuni.ttf"));
            fontBold = PDType0Font.load(document, new File(baseDir + "Bold.ttf"));
        } catch (IOException e) {
            return null;
        }

        PDPage page = new PDPage();
        document.addPage(page);

        try {
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            PDRectangle mediaBox = page.getMediaBox();

            drawBorder(contentStream, mediaBox);

            PDImageXObject image
                    = PDImageXObject.createFromFile(baseDir + "a.png", document);

            contentStream.drawImage(image, mediaBox.getUpperRightX() - 150, mediaBox.getUpperRightY() - 60, 130, 45);

            PDImageXObject image2
                    = PDImageXObject.createFromFile(baseDir + "b.png", document);

            contentStream.drawImage(image2, 20, mediaBox.getUpperRightY() - 60, 98, 45);

            float marginTop = 80;
            myShowText("Teilnahmebestätigung", contentStream, mediaBox, 32, marginTop, -1, true);

            marginTop += 20;
            myShowText("B-ID: " + theClass.getString("name") + " / " + user.getString("student_id"), contentStream, mediaBox, 10, marginTop, 150, false);

            marginTop += 45;
            myShowText("Das Österreichische Kulturforum Teheran bestätigt, dass", contentStream, mediaBox, 10, marginTop, -1, false);

            marginTop += 20;
            String text = (user.getString("sex").equals("male")) ?
                    "Herr " + capitalize(user.getString("name_en")) + " " + user.getString("last_name_en").toUpperCase() :
                    "Frau " + capitalize(user.getString("name_en")) + " " + user.getString("last_name_en").toUpperCase();

            myShowText(text, contentStream, mediaBox, 14, marginTop, -1, false);
            marginTop += 17;

            Document schedule = (Document) theClass.get("schedule");
            Document term = (Document) theClass.get("term");

            marginTop += 50;
            myShowText("Verwendetes Lehrwerk:", contentStream, mediaBox, 14, marginTop, mediaBox.getWidth() - 50, false);
            marginTop += 50;
            myShowText(theClass.getString("book"), contentStream, mediaBox, 14, marginTop, -1, false);
            marginTop += 50;
            myShowText("Kursbezeichnungen:", contentStream, mediaBox, 14, marginTop, mediaBox.getWidth() - 50, true);

            marginTop += 25;

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

            marginTop += 60;

            contentStream.beginText();
            contentStream.newLineAtOffset(50, mediaBox.getHeight() - marginTop);
            myShowTextInline("Kursdauer: ", contentStream, 12, true);
            int totalMins = (theClass.getInteger("time") * theClass.getInteger("class_count"));
            totalMins += Math.floor(totalMins / 45.0) * 15;

            int hoursInWeek = schedule.getString("name").contains("tensiv") ? 12 : 6;

            myShowTextInline((totalMins / 60) + "Std. Pro Kurs, " + hoursInWeek + " Std. Pro Woche", contentStream, 12, false);
            contentStream.endText();

            marginTop += 20;

            List<Document> dates = theClass.getList("dates", Document.class);
            String firstClass = dates.get(0).getString("date");

            String[] startSplited = firstClass.split("/");
            String milady = JalaliCalendar.jalaliToGregorian(new JalaliCalendar.YearMonthDate(
                    startSplited[0], startSplited[1], startSplited[2]
            )).eigthDigitFormatReverse(".");

            contentStream.beginText();
            contentStream.newLineAtOffset(50, mediaBox.getHeight() - marginTop);
            myShowTextInline("Kursbeginn: ", contentStream, 12, true);
            myShowTextInline(milady, contentStream, 12, false);
            contentStream.endText();

            String[] today = Utility.getToday("/").split("/");
            String curr = JalaliCalendar.jalaliToGregorian(new JalaliCalendar.YearMonthDate(
                    today[0], today[1], today[2]
            )).eigthDigitFormatReverse(".");

            marginTop += 60;
            myShowText("Teheran, am " + curr, contentStream, mediaBox, 14, marginTop, -1, false);

            marginTop += 20;
            myShowText("Direktorin (Fr. Ges. Barbara GROSSE)", contentStream, mediaBox, 14, marginTop, -1, false);

            marginTop = mediaBox.getHeight() - 45;
            myShowText("Der QR-Code dient zur Überprüfung dieses Dokuments.", contentStream, mediaBox, 8, marginTop, mediaBox.getWidth() - 85, false);
            marginTop = mediaBox.getHeight() - 30;
            myShowText("Es ist zu beachten, dass die Verifizierungsdomain mit ,,okft.org\" ist.", contentStream, mediaBox, 8, marginTop, mediaBox.getWidth() - 85, false);

            ByteArrayOutputStream stream = QRCode
                    .from("https://okft.org/validateParticipationCertification/" + code)
                    .withSize(100, 100)
                    .stream();


            ByteArrayInputStream bis = new ByteArrayInputStream(stream.toByteArray());

            File outputFile = null;

            try {
                outputFile = new File(baseDir + "image.jpg");
                ImageIO.write(ImageIO.read(bis), "jpg", outputFile);

                PDImageXObject image3
                        = PDImageXObject.createFromFile(outputFile.getAbsolutePath(), document);

                contentStream.drawImage(image3, 25, 15, 60, 60);

            } catch (IOException e) {
                e.printStackTrace();
            }

            contentStream.close();
            document.save(baseDir + code + ".pdf");
            document.close();
            outputFile.delete();

            return new File(baseDir + code + ".pdf");
        } catch (Exception x) {
            x.printStackTrace();
        }

        return null;
    }

    private static void myShowText(String text, PDPageContentStream contentStream,
                                   PDRectangle mediaBox, int fontSize, float marginTop, float marginRight,
                                   boolean isBold) {
        try {
            contentStream.beginText();
            contentStream.setFont((isBold) ? fontBold : font, fontSize);
            centerText(text, (isBold) ? fontBold : font, fontSize, contentStream, mediaBox, marginTop, marginRight);

            contentStream.showText(text);
            contentStream.endText();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void myShowTextInline(String text, PDPageContentStream contentStream,
                                         int fontSize, boolean isBold) {
        try {
            contentStream.setFont((isBold) ? fontBold : font, fontSize);
            contentStream.showText(text);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void centerText(String text, PDFont font, int fontSize,
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

    private static void drawBorder(PDPageContentStream contentStream, PDRectangle mediaBox) {
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

    private static String capitalize(String str) {

        if (str == null || str.isEmpty())
            return str;

        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static void test() {

        PDDocument document = new PDDocument();

        PDPage page = new PDPage();
        document.addPage(page);

        try {
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            PDRectangle mediaBox = page.getMediaBox();

            ByteArrayOutputStream stream = QRCode
                    .from("https://okft.org/api/downloadSamplePDF")
                    .withSize(500, 500)
                    .stream();

            ByteArrayInputStream bis = new ByteArrayInputStream(stream.toByteArray());

            File outputFile = null;

            try {

                outputFile = new File(baseDir + "image.jpg");
                ImageIO.write(ImageIO.read(bis), "jpg", outputFile);

                PDImageXObject image3
                        = PDImageXObject.createFromFile(outputFile.getAbsolutePath(), document);

                contentStream.drawImage(image3, (mediaBox.getWidth() - 500) / 2, (mediaBox.getHeight() - 500) / 2, 500, 500);

            } catch (IOException e) {
                e.printStackTrace();
            }

            contentStream.close();
            document.save(baseDir + "qr.pdf");
            document.close();
            outputFile.delete();
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

}
