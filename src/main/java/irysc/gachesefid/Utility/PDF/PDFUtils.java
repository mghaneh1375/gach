package irysc.gachesefid.Utility.PDF;

import irysc.gachesefid.Controllers.Quiz.QuizAbstract;
import irysc.gachesefid.Utility.Utility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.bson.Document;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static irysc.gachesefid.Utility.PDF.Common.*;
import static irysc.gachesefid.Utility.Utility.printException;


public class PDFUtils {

    public static File createExam(ArrayList<String> files, String filename,
                                  Document quiz, String schoolName, String pic) {

        PDDocument document = new PDDocument();
        try {
            farsiFont = PDType0Font.load(document, new File(baseDir + "IRANSansWeb.ttf"));
            farsiFontBold = PDType0Font.load(document, new File(baseDir + "IRANSansWeb_Bold.ttf"));
        } catch (IOException e) {
            return null;
        }

        PDPage page = new PDPage();
        document.addPage(page);

        float marginFromTop = 60;

        try {

            PDPageContentStream contentStream = new PDPageContentStream(document, page, false, true, true);
            PDRectangle mediaBox = page.getMediaBox();

            int tempSpaceWidth = 120;
            int tempSpaceTop = 100;

            int marginRightHeader = 70;
            int marginTopHeader = 75;

            float marginTop = marginFromTop;
            float totalH = mediaBox.getHeight() - 20;

            int w = (int) (mediaBox.getWidth() - 40 - tempSpaceWidth);

            if(pic != null) {

                File avatarF = new File(pic);
                if(avatarF.exists()) {
                    PDImageXObject avatar = PDImageXObject.createFromFileByExtension(avatarF, document);
                    BufferedImage bimg = ImageIO.read(avatarF);
                    int wA = 60;
                    int h = wA * bimg.getHeight() / bimg.getWidth();
                    contentStream.drawImage(avatar, 30, totalH - 80, wA, h);
                    marginTopHeader = 80;
                    tempSpaceTop = 140;
                    marginTop += 50;
                }

            }


            myShowText(bidiReorder("چرک نویس"), contentStream, mediaBox, 14, tempSpaceTop, mediaBox.getWidth() - 40, false);

            contentStream.setStrokingColor(Color.BLACK);
            contentStream.setLineWidth((float) 0.4);
            contentStream.addLine(tempSpaceWidth, 20, tempSpaceWidth, mediaBox.getHeight() - marginFromTop - 30);
            contentStream.stroke();

            drawCommon(document, contentStream, mediaBox);

            boolean first = true;

            myShowText(bidiReorder("بسم الله الرحمن الرحیم"), contentStream, mediaBox, 12, 50, -1, true);
            myShowText(bidiReorder(" نام آزمون: " + quiz.getString("title")), contentStream, mediaBox, 8, marginTopHeader, marginRightHeader, false);

            if(schoolName != null) {
                marginRightHeader += 130;
                myShowText(bidiReorder(" نام مدرسه: " + schoolName), contentStream, mediaBox, 8, marginTopHeader, marginRightHeader, false);
            }

            if(quiz.containsKey("start")) {
                marginRightHeader += 190;
                myShowText(bidiReorder(" تاریخ برگزاری: " + Utility.getSolarDate(quiz.getLong("start"))), contentStream, mediaBox, 8, marginTopHeader, marginRightHeader, false);
            }

            int len = (int) Math.ceil(QuizAbstract.calcLenStatic(quiz) / 60);
            marginRightHeader += 90;
            myShowText(bidiReorder(" مدت آزمون: " + len + " دقیقه"), contentStream, mediaBox, 8, marginTopHeader, marginRightHeader, false);


            int i = 1;
            for(String file : files) {

                File f = new File(file);
                BufferedImage bimg = ImageIO.read(f);

                int h = w * bimg.getHeight() / bimg.getWidth();

                if(marginTop + h + 20 > totalH) {

                    contentStream.close();

                    PDPage nextPage = new PDPage();
                    document.addPage(nextPage);

                    contentStream = new PDPageContentStream(document, nextPage);
                    mediaBox = nextPage.getMediaBox();

                    drawCommon(document, contentStream, mediaBox);
                    marginTop = marginFromTop;

                    contentStream.setStrokingColor(Color.BLACK);
                    contentStream.setLineWidth((float) 0.4);
                    contentStream.addLine(tempSpaceWidth, 20, tempSpaceWidth, mediaBox.getHeight() - marginFromTop - 30);
                    contentStream.stroke();

                    first = true;
                }

                PDImageXObject image
                        = PDImageXObject.createFromFileByExtension(f, document);

                marginTop += h + 20;

                contentStream.drawImage(image, mediaBox.getWidth() - w - 20, totalH - marginTop, w, h);

                myShowText(bidiReorder(convertEnToPrNum(i + "") + " - "), contentStream, mediaBox, 14, marginTop - h + 74, 45, false);

                i++;

                if(first)
                    first = false;
                else {
                    contentStream.setStrokingColor(Color.BLACK);
                    contentStream.setLineWidth((float) 0.4);
                    contentStream.addLine(100 + tempSpaceWidth, totalH - marginTop + h + 10, mediaBox.getWidth() - 100, totalH - marginTop + h + 10);
                    contentStream.stroke();
                }

            }

            contentStream.close();
            document.save(filename);
            document.close();

            return new File(filename);
        }
        catch (Exception x) {
            printException(x);
            return null;
        }

    }

    public static File getCertificate(List<Document> params, List<String> values,
                                      String img, boolean isLandscape,
                                      int qrX, int qrY, int qrSize, String certId) {

        PDDocument document = new PDDocument();
        try {
            farsiFont = PDType0Font.load(document, new File(baseDir + "IRANSansWeb.ttf"));
            farsiFontBold = PDType0Font.load(document, new File(baseDir + "IRANSansWeb_Bold.ttf"));
        } catch (IOException e) {
            return null;
        }

        PDPage page = (isLandscape) ?
                new PDPage(new PDRectangle(PDRectangle.A5.getHeight(), PDRectangle.A5.getWidth())) :
                new PDPage();

        document.addPage(page);

        try {

            PDPageContentStream contentStream = new PDPageContentStream(document, page, false, true, true);
            PDRectangle mediaBox = page.getMediaBox();

            PDImageXObject image
                    = PDImageXObject.createFromFile(baseDir + img, document);

            contentStream.drawImage(image, 0, 0, mediaBox.getWidth(), mediaBox.getHeight());

            //28, 150, true
            for(int i = 0; i < params.size(); i++) {

                if(params.get(i).containsKey("is_center"))
                    myShowTextCenterWithOffsetFarsi(bidiReorder(values.get(i)),
                            contentStream, mediaBox,
                            params.get(i).getInteger("font_size"),
                            params.get(i).getInteger("y"),
                            params.get(i).getBoolean("is_bold"),
                            params.get(i).containsKey("center_offset") ?
                                    params.get(i).getInteger("center_offset") : 0
                    );
                else
                    myShowText(bidiReorder(values.get(i)), contentStream, mediaBox,
                            params.get(i).getInteger("font_size"),
                            params.get(i).getInteger("y"),
                            params.get(i).getInteger("x"),
                            params.get(i).getBoolean("is_bold")
                    );
            }

//            "http://192.168.43.157:3000/checkCert/" + certId
            drawQR(document, contentStream, (int) (mediaBox.getWidth() - qrX), qrY, Math.max(qrSize, 100), qrSize, "https://e.irysc.com/checkCert/" + certId);
            contentStream.close();

            String filename = baseDir + "exam.pdf";

            document.save(filename);
            document.close();

            return new File(filename);

        }
        catch (Exception x) {
            printException(x);
            return null;
        }

    }

    public static File getCertificate2(String course, int hours, String date) {

        PDDocument document = new PDDocument();
        try {
            farsiFont = PDType0Font.load(document, new File(baseDir + "IRANSansWeb.ttf"));
            farsiFontBold = PDType0Font.load(document, new File(baseDir + "IRANSansWeb_Bold.ttf"));
        } catch (IOException e) {
            return null;
        }

        PDPage page = new PDPage(new PDRectangle(PDRectangle.A5.getHeight(), PDRectangle.A5.getWidth()));
        document.addPage(page);

        try {

            PDPageContentStream contentStream = new PDPageContentStream(document, page, false, true, true);
            PDRectangle mediaBox = page.getMediaBox();

            PDImageXObject image
                    = PDImageXObject.createFromFile(baseDir + "cert2.jpg", document);

            contentStream.drawImage(image, 0, 0, mediaBox.getWidth(), mediaBox.getHeight());

            myShowText(bidiReorder(course), contentStream, mediaBox, 9, 220, 360, false);
            myShowText(bidiReorder(hours + ""), contentStream, mediaBox, 11, 225, 470, false);

            myShowText(bidiReorder(date), contentStream, mediaBox, 9, 330, 200, false);

            drawQR(document, contentStream, 24, 20, 100, 33, "https://google.com");

            contentStream.close();

            String filename = baseDir + "exam.pdf";

            document.save(filename);
            document.close();

            return new File(filename);

        }
        catch (Exception x) {
            printException(x);
            return null;
        }

    }

}
