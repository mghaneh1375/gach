package irysc.gachesefid.Utility;

import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Exception.InvalidFileTypeException;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static irysc.gachesefid.Utility.StaticValues.DEV_MODE;


public class FileUtils {

    public final static String baseDir_dev = "./src/main/";

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

    public static boolean checkExist(String filename, String folder) {

        if(1 == 1)
        return true;
        Path location = Paths.get(DEV_MODE ?
                uploadDir_dev + folder + File.separator + filename :
                uploadDir + folder + File.separator + filename
        );

        return Files.exists(location);
    }

    public static String renameFile(String folder, String oldName, String newName) {

        Path location = Paths.get(DEV_MODE ?
                uploadDir_dev + folder :
                uploadDir + folder
        );

        if(newName == null) {
            newName = Utility.randInt() + "_" + System.currentTimeMillis() + ".";
            String[] splited = oldName.split("\\.");
            String ext = splited[splited.length - 1];
            newName += ext;
        }

        boolean success = new File(location.toString() + "/" + oldName).renameTo(
                new File(location.toString() + "/" + newName)
        );

        if(1 == 1)
            return newName;

        if(!success)
            return null;

        return newName;
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
    public static String uploadDocOrMultimediaFile(MultipartFile file) {

        try {

            String fileType = (String) FileUtils.getFileType(Objects.requireNonNull(file.getOriginalFilename())).getKey();

            if (!fileType.equals("image") && !fileType.equals("word") &&
                    !fileType.equals("pdf") && !fileType.equals("voice") &&
                    !fileType.equals("video") && !fileType.equals("excel") &&
                    !fileType.equals("powerpoint")
            )
                return null;

            return fileType;

        } catch (InvalidFileTypeException e) {
            return null;
        }
    }

    @Nullable
    public static String uploadMultimediaFile(MultipartFile file) {

        try {

            String fileType = (String) FileUtils.getFileType(Objects.requireNonNull(file.getOriginalFilename())).getKey();

            if (!fileType.equals("video"))
                return null;

            return fileType;

        } catch (InvalidFileTypeException e) {
            return null;
        }
    }

    @Nullable
    public static String uploadPdfOrMultimediaFile(MultipartFile file) {

        try {

            String fileType = (String) FileUtils.getFileType(Objects.requireNonNull(file.getOriginalFilename())).getKey();

            if (
                    !fileType.equals("image") && !fileType.equals("pdf") && !fileType.equals("voice") &&
                    !fileType.equals("powerpoint") && !fileType.equals("word") && !fileType.equals("video")
            )
                return null;

            return fileType;

        } catch (InvalidFileTypeException e) {
            return null;
        }
    }

    @Nullable
    public static String uploadImageFile(MultipartFile file) {

        try {

            String fileType = (String) FileUtils.getFileType(Objects.requireNonNull(file.getOriginalFilename())).getKey();

            if (!fileType.equals("image"))
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
            case "m4v":
            case "mov":
            case "mpeg":
            case "mkv":
            case "avi":
            case "flv":
                return new PairValue("video", ext);
            case "mp3":
            case "ogg":
                return new PairValue("voice", ext);
            case "pdf":
                return new PairValue("pdf", ext);
            case "xls":
            case "xlsx":
                return new PairValue("excel", ext);
            case "pptx":
            case "ppt":
                return new PairValue("powerpoint", ext);
            case "doc":
            case "docx":
                return new PairValue("word", ext);
            case "zip":
                return new PairValue("zip", ext);
            default:
                throw new InvalidFileTypeException(ext + " is not a valid extension");
        }
    }

    public static JSONArray getAppropriateExt(String type) {

        JSONArray jsonArray = new JSONArray();

        switch (type) {
            case "image":
                return jsonArray.put(".jpg").put(".png").put(".jpeg");
            case "video":
                return jsonArray.put(".mp4").put(".m4v")
                        .put(".mov").put("mpeg").put("mkv").put("avi")
                        .put("flv");
            case "audio":
                return jsonArray.put(".mp3").put(".ogg");
            case "pdf":
                return jsonArray.put(".pdf");
            case "excel":
                return jsonArray.put(".xls").put(".xlsx");
            case "powerpoint":
                return jsonArray.put(".pptx").put(".ppt");
            case "word":
                return jsonArray.put(".doc").put(".docx");
        }

        return jsonArray;
    }

    private static final int BUFFER_SIZE = 4096;

    public static void unzip(InputStream inputStream,
                             String zipFilePath,
                             String destDirectory,
                             boolean justFile,
                             boolean createDestDir) throws Exception {

        File destDir = new File(DEV_MODE ?
                uploadDir_dev + File.separator + destDirectory :
                uploadDir + File.separator + destDirectory
        );

        if (!destDir.exists()) {
            if(createDestDir)
                destDir.mkdir();
            else
                throw new InvalidFieldsException("dest directory not exist");
        }

        ZipInputStream zipIn;
        if(inputStream == null)
            zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        else
            zipIn = new ZipInputStream(inputStream);

        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = destDir.getPath() + File.separator + entry.getName();

            if (!entry.isDirectory()) { // if the entry is a file, extracts it
                extractFile(zipIn, filePath);
            }
            else { // if the entry is a directory, make the directory
                if(justFile)
                    continue;

                File dir = new File(filePath);
                dir.mkdirs();
            }

            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }

        zipIn.close();
    }

    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {

        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read;

        while ((read = zipIn.read(bytesIn)) != -1)
            bos.write(bytesIn, 0, read);

        bos.close();
    }
}
