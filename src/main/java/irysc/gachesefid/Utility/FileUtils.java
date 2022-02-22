package irysc.gachesefid.Utility;

import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Exception.InvalidFileTypeException;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import static irysc.gachesefid.Utility.StaticValues.DEV_MODE;

public class FileUtils {

    public final static String uploadDir = "/var/www/statics/";
    public final static String uploadDir_dev = "/var/www/statics/";
//    public final static String uploadDir_dev = "./src/main/resources/assets/";

    public final static String limboDir = "/var/www/statics/assets/limbo" + File.separator;
    public final static String limboDir_dev = "/var/www/statics/assets/limbo" + File.separator;
//    public final static String limboDir_dev = "./src/main/resources/assets/limbo" + File.separator;

    public static String uploadFile(MultipartFile file, String folder) {

        try {
            String[] splited = file.getOriginalFilename().split("\\.");
            String filename = System.currentTimeMillis() + "." + splited[splited.length - 1];

            Path copyLocation = Paths.get(DEV_MODE ?
                    uploadDir_dev + folder + File.separator + filename :
                    uploadDir + folder + File.separator + filename
            );
            Files.copy(file.getInputStream(), copyLocation, StandardCopyOption.REPLACE_EXISTING);

            return filename;
        } catch (Exception e) {
            System.out.println("Could not store file " + file.getOriginalFilename()
                    + ". Please try again!");
        }

        return null;
    }

    public static String uploadTempFile(MultipartFile file) {

        try {
            String[] splited = file.getOriginalFilename().split("\\.");
            String filename = System.currentTimeMillis() + "." + splited[splited.length - 1];

            Path copyLocation = Paths.get(DEV_MODE ?
                    limboDir_dev + filename :
                    limboDir + filename
            );

            Files.copy(file.getInputStream(), copyLocation, StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (Exception e) {
            System.out.println("Could not store file " + file.getOriginalFilename()
                    + ". Please try again!");
        }

        return null;
    }

    public static void removeFile(String filename, String folder) {

        Path location = Paths.get(DEV_MODE ?
                uploadDir_dev + folder + File.separator + filename :
                uploadDir + folder + File.separator + filename
        );

        try {
            Files.delete(location);
        } catch (Exception x) {
        }
    }

    public static void removeTempFile(String filename) {

        Path location = Paths.get(DEV_MODE ?
                limboDir_dev + filename :
                limboDir + filename
        );

        try {
            Files.delete(location);
        } catch (Exception x) {
        }
    }

    @Nullable
    public static String uploadImageOrPdfFile(MultipartFile file) {

        try {

            String fileType = (String) FileUtils.getFileType(Objects.requireNonNull(file.getOriginalFilename())).getKey();

            if (!fileType.equals("image") && !fileType.equals("pdf"))
                return null;

            return fileType;

        } catch (InvalidFileTypeException e) {
            return null;
        }
    }

    @Nullable
    public static String uploadImageOrPdfOrVoiceFile(MultipartFile file) {

        try {

            String fileType = (String) FileUtils.getFileType(Objects.requireNonNull(file.getOriginalFilename())).getKey();

            if (!fileType.equals("image") && !fileType.equals("pdf") && !fileType.equals("voice"))
                return null;

            return fileType;

        } catch (InvalidFileTypeException e) {
            return null;
        }
    }

    private static PairValue getFileType(String filename) throws InvalidFileTypeException {

        String[] splited = filename.split("\\.");
        String ext = splited[splited.length - 1];

        switch (ext.toLowerCase()) {
            case "jpg":
            case "png":
            case "jpeg":
            case "bmp":
            case "webp":
            case "gif":
                return new PairValue("image", ext);
            case "mp4":
            case "mov":
            case "avi":
            case "flv":
                return new PairValue("video", ext);
            case "mp3":
            case "ogg":
                return new PairValue("voice", ext);
            case "pdf":
                return new PairValue("pdf", ext);
            case "zip":
                return new PairValue("zip", ext);
            default:
                throw new InvalidFileTypeException(ext + " is not a valid extension");
        }
    }
}
