package irysc.gachesefid.Routes.API.Certificate;

import irysc.gachesefid.Controllers.Certification.StudentCertification;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

@Controller
@RequestMapping(path = "/api/certificate")
@Validated
public class StudentCertificateAPIRoutes extends Router {

    @GetMapping(path = "/issueMyCert/{certificateId}/{NID}")
    @ResponseBody
    public String issueMyCert(HttpServletRequest request,
                              @PathVariable @ObjectIdConstraint ObjectId certificateId,
                              @PathVariable @NotBlank String NID) {
        StudentCertification.issueMyCert(certificateId, NID);
        return "ok";
    }

    @GetMapping(path = "/getMyCerts/{NID}")
    @ResponseBody
    public String getMyCerts(HttpServletRequest request,
                             @PathVariable @NotBlank String NID) {
        return StudentCertification.getMyCerts(NID);
    }

}
