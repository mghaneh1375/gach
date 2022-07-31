package irysc.gachesefid.Controllers.Python;

import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Utility.FileUtils;
import irysc.gachesefid.Utility.Utility;
import org.apache.commons.io.IOUtils;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static irysc.gachesefid.Main.GachesefidApplication.iryscQuizRepository;
import static irysc.gachesefid.Utility.StaticValues.*;

public class CVController {

    private final static boolean LINUX = false;
    private final static String BASE = FileUtils.baseDir_dev + "java/irysc/gachesefid/CV/";

    private static byte[] toByteArray(BufferedImage bi, String format)
            throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bi, format, baos);
        return baos.toByteArray();

    }

    public static String correct(
//            MultipartFile file,
            ObjectId quizId, ObjectId userId,
            int col, int row, int total,
            int eyes, int choices
    ) {

        Document doc = iryscQuizRepository.findById(quizId);

        List<Document> students = doc.getList("students", Document.class);
        Document student = irysc.gachesefid.Utility.Utility.searchInDocumentsKeyVal(
                students, "_id", userId
        );

        if (student == null)
            return JSON_NOT_VALID_ID;

        String file = student.getString("answer_sheet");

//        PairValue p = FileUtils.uploadFileWithPath(file, BASE);
//
//        if (p == null)
//            return Utility.generateErr("file upload err");

        String filename_abs = new File(FileUtils.uploadDir_dev + "answer_sheets/" + file).getAbsolutePath();
        String filename = file;

        StringBuilder answers = new StringBuilder("1");
        for (int i = 0; i < total - 1; i++)
            answers.append("-1");

        System.out.println(answers.toString());
        String output = DEV_MODE ? FileUtils.uploadDir_dev + "answer_sheets/" + filename :
                FileUtils.uploadDir + "answer_sheets/" + filename;

        ProcessBuilder processBuilder = new ProcessBuilder(
                LINUX ? "python" : FileUtils.baseDir_dev + "java/irysc/gachesefid/CV/venv/Scripts/python.exe",
                resolvePythonScriptPath("corrector.py"),
                "-i",
                filename_abs,
                "-t",
                (col * row) + "",
                "-e",
                eyes + "",
                "-d",
                "3",
                "-c",
                choices + "",
                "-a",
                answers.toString(),
                "-o",
                output
        );


        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            String results = readProcessOutput(process.getInputStream());

//            FileUtils.removeFileWithSpecificPath(filename, BASE);

            return Utility.generateSuccessMsg("result", results,
                    new PairValue("path", STATICS_SERVER + "answer_sheets/" + filename)
            );
        } catch (Exception x) {
//            FileUtils.removeFileWithSpecificPath(filename, BASE);
            x.printStackTrace();
            return Utility.generateErr(x.getMessage());
        }
    }

    private static String resolvePythonScriptPath(String script) {
        return BASE + script;
    }

    private static String readProcessOutput(InputStream inputStream) {
        try {
            return IOUtils.toString(inputStream, "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "err";
    }
}
