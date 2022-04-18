package irysc.gachesefid.Utility.PDF;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static irysc.gachesefid.Utility.PDF.Common.*;


public class PDFUtils {


    public static File createExam(ArrayList<String> files) {

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

            myShowText(bidiReorder("چرک نویس"), contentStream, mediaBox, 14, 80, mediaBox.getWidth() - 40, false);

            contentStream.setStrokingColor(Color.BLACK);
            contentStream.setLineWidth((float) 0.4);
            contentStream.addLine(tempSpaceWidth, 20, tempSpaceWidth, mediaBox.getHeight() - marginFromTop - 30);
            contentStream.stroke();

            drawCommon(document, contentStream, mediaBox);

            float marginTop = marginFromTop;
            float totalH = mediaBox.getHeight() - 20;

            int w = (int) (mediaBox.getWidth() - 40 - tempSpaceWidth);
            boolean first = true;

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

            String filename = baseDir + "exam.pdf";

            document.save(filename);
            document.close();

            return new File(filename);

        }
        catch (Exception x) {
            x.printStackTrace();
            return null;
        }

    }



}
