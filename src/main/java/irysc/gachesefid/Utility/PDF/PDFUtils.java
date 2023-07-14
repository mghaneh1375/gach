package irysc.gachesefid.Utility.PDF;

import irysc.gachesefid.Controllers.Quiz.QuizAbstract;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.JalaliCalendar;
import irysc.gachesefid.Utility.Utility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import static irysc.gachesefid.Controllers.Advisor.Utility.getWeekDay;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.PDF.Common.*;
import static irysc.gachesefid.Utility.Utility.printException;


public class PDFUtils {

    private static int calcWord(String word) {
        int chars = word.length();
        int spaces = word.split("\\s+").length - 1;
        return chars * 2 + spaces * 3;
    }

    public static File exportSchedule(Document schedule, Document student) {

        PDDocument document = new PDDocument();
        try {
            farsiFont = PDType0Font.load(document, new File(baseDir + "IRANSansWeb.ttf"));
            farsiFontBold = PDType0Font.load(document, new File(baseDir + "IRANSansWeb_Bold.ttf"));
        } catch (IOException e) {
            return null;
        }

        PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
        document.addPage(page);

        HashMap<ObjectId, String> advisors = new HashMap<>();

        try {

            PDPageContentStream contentStream = new PDPageContentStream(document, page, false, true, true);
            PDRectangle mediaBox = page.getMediaBox();

            myShowText(bidiReorder("برنامه مشاوره هفتگی دانش آموز: " +
                    student.getString("first_name") + " " + student.getString("last_name")),
                    contentStream, mediaBox, 10, 20, -1, false
            );

            for (ObjectId advisorId : schedule.getList("advisors", ObjectId.class)) {

                Document advisor = userRepository.findById(advisorId);

                advisors.put(advisorId,
                        advisor.getString("first_name") + " " + advisor.getString("last_name")
                );
            }

            StringBuilder allAdvisors = new StringBuilder();
            boolean first = true;

            for(ObjectId advisorId : advisors.keySet()) {
                if(first) {
                    first = false;
                    allAdvisors.append(advisors.get(advisorId));
                }
                else {
                    allAdvisors.append(" - ").append(advisors.get(advisorId));
                }
            }

            myShowText(bidiReorder("مشاور / مشاوران: " + allAdvisors),
                    contentStream, mediaBox, 10, 35, -1, false
            );

            String pic = "irysc.png";
            File logo = new File(FileUtils.uploadDir_dev + "certificationsPDF/" + pic);

            if(logo.exists()) {

                PDImageXObject avatar = PDImageXObject.createFromFileByExtension(logo, document);
                BufferedImage bimg = ImageIO.read(logo);
                int wA = 60;
                int h = wA * bimg.getHeight() / bimg.getWidth();
                contentStream.drawImage(avatar, 30, mediaBox.getHeight() - 40, wA, h);
            }

            int marginTop = 65;
            int i = 0;
            int lastAdded = 0;

            String weekStartAt = schedule.getString("week_start_at");

            String[] splited = weekStartAt.split("\\/");

            JalaliCalendar jalaliCalendar = new JalaliCalendar(
                    Integer.parseInt(splited[0]), Integer.parseInt(splited[1]), Integer.parseInt(splited[2])
            );

            ArrayList<Integer> addedCols = new ArrayList<>();

            for (Document day : schedule.getList("days", Document.class)) {

                String dayStr = getWeekDay(day.getInteger("day"));
                String date;

                if(i % 2 == 1) {

                    PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
                    graphicsState.setNonStrokingAlphaConstant(0.3f);
                    contentStream.setGraphicsStateParameters(graphicsState);

                    contentStream.setNonStrokingColor(204, 204, 204);
                    contentStream.addRect(50, mediaBox.getHeight() - marginTop - 60, mediaBox.getWidth() - 80, 80);
                    contentStream.fill();

                    PDExtendedGraphicsState graphicsState2 = new PDExtendedGraphicsState();
                    graphicsState2.setNonStrokingAlphaConstant(1f);
                    contentStream.setGraphicsStateParameters(graphicsState2);

                    contentStream.setNonStrokingColor(Color.BLACK);

                }

                jalaliCalendar.add(Calendar.DAY_OF_MONTH, day.getInteger("day") - lastAdded);
                lastAdded = day.getInteger("day");

                String m = jalaliCalendar.get(Calendar.MONTH) < 10 ?
                        "0" + jalaliCalendar.get(Calendar.MONTH) :
                        jalaliCalendar.get(Calendar.MONTH) + "";

                String dddd = jalaliCalendar.get(Calendar.DAY_OF_MONTH) < 10 ?
                        "0" + jalaliCalendar.get(Calendar.DAY_OF_MONTH) :
                        jalaliCalendar.get(Calendar.DAY_OF_MONTH) + "";

                date = jalaliCalendar.get(Calendar.YEAR) + "/" + m + "/" + dddd;

                myShowText(bidiReorder(dayStr), contentStream, mediaBox, 12, marginTop,
                        day.getInteger("day") == 0 || day.getInteger("day") == 6 ? 80 : 90, false);


                int idx = 0;

                for(Document item : day.getList("items", Document.class)) {

                    int localMarginTop = marginTop - 10;
                    int right = (130 + idx * 90 + calcWord(item.getString("lesson")));

                    if(!addedCols.contains(idx)) {
                        addedCols.add(idx);
                        contentStream.setStrokingColor(Color.BLACK);
                        contentStream.setLineWidth((float) 0.4);
                        contentStream.addLine(mediaBox.getWidth() - (110 + idx * 90), mediaBox.getHeight() - 50, mediaBox.getWidth() - (110 + idx * 90), 10);
                        contentStream.stroke();
                    }

                    myShowText(bidiReorder(
                            item.getString("lesson")),
                            contentStream, mediaBox, 7, localMarginTop,
                            right, false);

                    localMarginTop += 12;

                    right = (130 + idx * 90 + calcWord(item.getString("tag")));

                    myShowText(bidiReorder(
                                    item.getString("tag")),
                            contentStream, mediaBox, 7, localMarginTop,
                            right, false);

                    localMarginTop += 12;
                    right = (130 + idx * 90 + calcWord(item.getInteger("duration") + " دقیقه"));

                    myShowText(bidiReorder(
                                    convertEnToPrNum(item.getInteger("duration") + "") + " دقیقه"),
                            contentStream, mediaBox, 7, localMarginTop,
                            right, false);

                    localMarginTop += 12;

                    if(item.containsKey("start_at")) {

                        right = (130 + idx * 90 + calcWord("زمان آعاز: " + item.getString("start_at")));
                        String tmp = item.getString("start_at");
                        String[] tmpSplited = tmp.split(":");
                        String tt = convertEnToPrNum(tmpSplited[0]) + ":" + convertEnToPrNum(tmpSplited[1]);

                        myShowText(bidiReorder(" زمان آغاز: " + tt),
                                contentStream, mediaBox, 7, localMarginTop,
                                right, false);

                        localMarginTop += 12;
                    }

                    if(item.containsKey("additional_label")) {

                        right = (130 + idx * 90 + calcWord(item.getString("additional_label") + ": " + item.getInteger("additional")));

                        myShowText(
                                bidiReorder(item.getString("additional_label") + ": " + convertEnToPrNum(item.getInteger("additional") + "")),
                                contentStream, mediaBox, 7, localMarginTop,
                                right, false);

                        localMarginTop += 12;
                    }

                    String advisor;

                    if(advisors.containsKey(item.getObjectId("advisor_id")))
                        advisor = advisors.get(item.getObjectId("advisor_id"));
                    else {
                        Document advisorDoc = userRepository.findById(item.getObjectId("advisor_id"));
                        advisor = advisorDoc.getString("first_name") + " " + advisorDoc.getString("last_name");
                    }

                    right = (130 + idx * 90 + calcWord("مشاور: " + advisor));

                    myShowText(
                            bidiReorder("مشاور: " + advisor),
                            contentStream, mediaBox, 6, localMarginTop,
                            right, false);

                    idx++;
                }

                marginTop += 20;

                String ss = date.replace("/", "");
                String d = convertEnToPrNum(ss);

                myShowText(bidiReorder(d.substring(0, 4) + "/" + d.substring(4, 6) + "/" + d.substring(6, 8)), contentStream, mediaBox, 12, marginTop, 100, false);

                contentStream.setStrokingColor(Color.BLACK);
                contentStream.setLineWidth((float) 0.4);
                contentStream.addLine(50, mediaBox.getHeight() - (65 + i * 80 + 60), mediaBox.getWidth() - 30, mediaBox.getHeight() - (65 + i * 80 + 60));
                contentStream.stroke();

                marginTop += 60;
                i++;
            }

            contentStream.close();
            String filename = System.currentTimeMillis() + ".pdf";

            document.save(filename);
            document.close();

            return new File(filename);
        }
        catch (Exception x) {
            x.printStackTrace();
        }

        return null;
    }

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

                myShowText(bidiReorder(convertEnToPrNum(i + "") + " - "), contentStream, mediaBox, 14, marginTop - h + 20, 45, false);

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
                                      int qrX, int qrY, int qrSize, String certId, String NID) {

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

            drawQR(document, contentStream, (int) (mediaBox.getWidth() - qrX), qrY, Math.max(qrSize, 100), qrSize, "https://e.irysc.com/checkCert/" + certId + "/" + NID);
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
