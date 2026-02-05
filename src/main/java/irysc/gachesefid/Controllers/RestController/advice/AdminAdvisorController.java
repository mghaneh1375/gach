package irysc.gachesefid.Controllers.RestController.advice;

import irysc.gachesefid.Controllers.Teaching.TeachController;
import irysc.gachesefid.Dto.ResponseDto;
import irysc.gachesefid.Dto.advice.AdvisorDigestInfoDto;
import irysc.gachesefid.Dto.advice.AdvisorGeneralInfoDto;
import irysc.gachesefid.Service.advice.AdvisorService;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping(value = "/admin/advisor")
public class AdminAdvisorController {

    @Autowired
    private AdvisorService advisorService;

    @GetMapping(value = "/generalInfo/{advisorId}")
    @ResponseBody
    public ResponseEntity<ResponseDto<AdvisorGeneralInfoDto>> getGeneralInfo(
            @PathVariable @ObjectIdConstraint ObjectId advisorId
    ) {
        return advisorService.getGeneralInfo(advisorId);
    }


    @GetMapping(value = "getAllAdvisorsDigest")
    @ResponseBody
    public ResponseEntity<ResponseDto<List<AdvisorDigestInfoDto>>> getAllTeachersDigest() {
        return advisorService.getAllAdvisorsDigest();
    }

}
