package irysc.gachesefid.Controllers;

import irysc.gachesefid.DB.IRYSCQuizRepository;
import irysc.gachesefid.DB.QuestionRepository;
import irysc.gachesefid.DB.TicketRepository;
import irysc.gachesefid.Models.UploadSection;
import irysc.gachesefid.Utility.FileUtils;
import org.springframework.web.multipart.MultipartFile;

import static irysc.gachesefid.Utility.FileUtils.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.generateErr;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

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

        else if(section.equalsIgnoreCase(UploadSection.CK.getName())) {

            if (file.getSize() > MAX_FILE_SIZE)
                return generateErr("حداکثر حجم مجاز، " + MAX_FILE_SIZE + " مگ است.");

            String fileType = uploadDocOrMultimediaFile(file);

            if (fileType == null)
                return generateErr("فرمت فایل موردنظر معتبر نمی باشد.");

            String filename = FileUtils.uploadFile(file, "ck");
            if (filename == null)
                return generateErr("فایل موردنظر معتبر نمی باشد.");

            return generateSuccessMsg("url", STATICS_SERVER +  "ck/" + filename);
        }

        return JSON_OK;
    }

}
