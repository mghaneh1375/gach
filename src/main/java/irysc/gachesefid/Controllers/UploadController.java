package irysc.gachesefid.Controllers;

import irysc.gachesefid.DB.QuestionRepository;
import irysc.gachesefid.Models.UploadSection;
import org.springframework.web.multipart.MultipartFile;

import static irysc.gachesefid.Utility.FileUtils.unzip;
import static irysc.gachesefid.Utility.StaticValues.JSON_OK;
import static irysc.gachesefid.Utility.Utility.generateErr;

public class UploadController {

    public static String uploadFiles(MultipartFile file,
                                     String section) {

        if (section.equalsIgnoreCase(UploadSection.QUESTION.getName())) {

            try {

                unzip(file.getInputStream(), null,
                        QuestionRepository.FOLDER, true, false
                );

            } catch (Exception e) {
                return generateErr(e.getMessage());
            }

        }


        return JSON_OK;
    }

}
