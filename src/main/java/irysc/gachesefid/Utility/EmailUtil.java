package irysc.gachesefid.Utility;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class EmailUtil {
//    private static String fontBase64;
//
//    static {
//        try {
//            fontBase64 = Files.readString(Paths.get(
//                    (
//                            StaticValues.DEV_MODE
//                                    ? FileUtils.uploadDir_dev
//                                    : FileUtils.uploadDir
//                    ) + "vazirmatn.b64"
//            ));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

    static void headerSection(StringBuilder content) {
        content.append("<html><head>")
                .append("<style>")
//                .append(String.format(
//                        "@font-face {" +
//                                "font-family: 'Vazirmatn';" +
//                                "src: url(data:font/woff2;base64,%s) format('woff2');" +
//                                "font-weight: normal;" +
//                                "font-style: normal;" +
//                                "}", fontBase64
//                ))
//                .append("@import url('https://fonts.googleapis.com/css2?family=Vazirmatn&display=swap');")
                .append("body, div, h3 { font-family: 'Vazirmatn', Tahoma, Arial, sans-serif; }")
                .append("</style></head><body>")
                .append("<div style='margin-right: 10%; margin-left: 10%; width: 80%; max-width: 700px;'>")
                .append("<div style='direction: rtl;")
                .append("max-width: 700px;")
                .append("width: 100%;")
                .append("align-self: center;")
                .append("padding: 40px;'>")
                .append("\n")
                .append("<div style='clear: both; height: 80px;'>")
                .append("\n")
        ;
    }

    static void emailFooterSection(StringBuilder content) {
        content.append("<div style='height: 120px; text-align: right; direction: rtl; font-weight: bolder; padding: 5px; margin-top: 20px; width: 100%'>");
        content.append("<p style='margin-top: 20px; margin-right: 10px; font-size: 0.9em'>به ما سر بزنید. نشانی سایت : </p>");
        content.append("<div style='font-size: 0.9em; margin-right: 10px;'><a href='https://e.irysc.com'>https://e.irysc.com</a></div>");
        content.append("<p style='font-size: 0.9em; margin-right: 10px;'>021-91096320</p>");
        content.append("</div>");
    }
}
