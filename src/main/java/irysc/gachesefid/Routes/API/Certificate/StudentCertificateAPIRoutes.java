package irysc.gachesefid.Routes.API.Certificate;

import irysc.gachesefid.Controllers.Certification.StudentCertification;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import org.apache.commons.io.FileUtils;
import org.bson.types.ObjectId;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import java.io.ByteArrayInputStream;
import java.io.File;

@Controller
@RequestMapping(path = "/api/certificate")
@Validated
public class StudentCertificateAPIRoutes extends Router {

    @GetMapping(path = "/issueMyCert/{certificateId}/{NID}")
    @ResponseBody
    public ResponseEntity<InputStreamResource> issueMyCert(@PathVariable @ObjectIdConstraint ObjectId certificateId,
                                                           @PathVariable @NotBlank String NID) {

        File f = StudentCertification.issueMyCert(certificateId, NID);
        if (f == null)
            return null;

        try {
            InputStreamResource file = new InputStreamResource(
                    new ByteArrayInputStream(FileUtils.readFileToByteArray(f))
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=certificate_2.pdf")
                    .contentType(MediaType.parseMediaType("application/pdf"))
                    .body(file);
        } catch (Exception x) {
            System.out.println(x.getMessage());
        }

        return null;
    }

    @PostMapping(path = "/verifyCert/{certificateId}/{id}")
    @ResponseBody
    public String verifyCert(@PathVariable @ObjectIdConstraint ObjectId certificateId,
                             @PathVariable @ObjectIdConstraint ObjectId id) {
        return StudentCertification.verifyCert(certificateId, id);
    }

    @GetMapping(path = "/issueCert/{certificateId}/{id}")
    @ResponseBody
    public ResponseEntity<InputStreamResource> issueCert(@PathVariable @ObjectIdConstraint ObjectId certificateId,
                                                         @PathVariable @ObjectIdConstraint ObjectId id) {
        File f = StudentCertification.issueCert(certificateId, id);
        if (f == null)
            return null;

        try {
            InputStreamResource file = new InputStreamResource(
                    new ByteArrayInputStream(FileUtils.readFileToByteArray(f))
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=certificate_2.pdf")
                    .contentType(MediaType.parseMediaType("application/pdf"))
                    .body(file);
        } catch (Exception x) {
            System.out.println(x.getMessage());
        }

        return null;
    }

    @GetMapping(path = "/getMyCerts/{NID}")
    @ResponseBody
    public String getMyCerts(@PathVariable @NotBlank String NID) {
        return StudentCertification.getMyCerts(NID);
    }

}
