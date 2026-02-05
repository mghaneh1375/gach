package irysc.gachesefid.Controllers.RestController.advice;

import irysc.gachesefid.Dto.ResponseDto;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.Access;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Service.advice.AdviceTagReportService;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;

@RestController
@Validated
@RequestMapping(value = "/general/advice_tag_report")
public class AdviceTagReportController extends Router {

    @Autowired
    private AdviceTagReportService adviceTagReportService;

    @PutMapping(value = "setAdviceScheduleReportProblemsByAdvisor/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setAdviceScheduleReportProblemsByAdvisor(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId studentId,
            @RequestBody @StrongJSONConstraint(
                    params = {}, paramsType = {},
                    optionals = {"tagIds", "desc"}, optionalsType = {JSONArray.class, String.class}
            ) String jsonStr
    ) throws UnAuthException, NotAccessException, NotActivateAccountException {
        JSONObject jsonObject;
        if (jsonStr == null || jsonStr.isEmpty()) jsonObject = new JSONObject();
        else jsonObject = new JSONObject(jsonStr);
        Document user = getUser(request);
        if(!user.containsKey("students") ||
                user.getList("students", Document.class)
                        .stream()
                        .filter(document -> document.getObjectId("_id").equals(studentId))
                        .findFirst()
                        .isEmpty()
        )
            throw new NotAccessException();

        adviceTagReportService.setAdviceScheduleReportProblemsByAdvisor(
                user.getObjectId("_id"), studentId,
                jsonObject.has("tagIds") ? jsonObject.getJSONArray("tagIds") : null,
                jsonObject.has("desc") ? jsonObject.getString("desc") : null
        );
    }

    @PutMapping(value = "setAdviceScheduleReportProblemsByStudent/{advisorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setAdviceScheduleReportProblemsByStudent(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId advisorId,
            @RequestBody @StrongJSONConstraint(
                    params = {}, paramsType = {},
                    optionals = {"tagIds", "desc"}, optionalsType = {JSONArray.class, String.class}
            ) String jsonStr
    ) throws UnAuthException, NotAccessException, NotActivateAccountException {
        JSONObject jsonObject;
        if (jsonStr == null || jsonStr.isEmpty()) jsonObject = new JSONObject();
        else jsonObject = new JSONObject(jsonStr);
        Document user = getUser(request);
        if(!user.containsKey("my_advisors") ||
            !user.getList("my_advisors", ObjectId.class).contains(advisorId)
        )
            throw new NotAccessException();

        adviceTagReportService.setAdviceScheduleReportProblemsByStudent(
                user.getObjectId("_id"), advisorId,
                jsonObject.has("tagIds") ? jsonObject.getJSONArray("tagIds") : null,
                jsonObject.has("desc") ? jsonObject.getString("desc") : null
        );
    }

//    @GetMapping("list")
//    @ResponseBody
//    public ResponseEntity<ResponseDto> list(
//            HttpServletRequest request,
//            @RequestParam(name = "pageIndex") @Min(1) @Max(1000000) int pageIndex
//    ) throws UnAuthException {
//        UserTokenInfo userTokenInfo = getUserTokenInfo(request);
//        boolean isAdvisor = userTokenInfo.getAccesses().contains(Access.ADVISOR.getName());
//        return new ResponseEntity<>(
//                ResponseDto
//                        .builder(List.class)
//                        .data(
//                                adviceTagReportService.getAdviceReports(
//                                        null, null, false,
//                                        isAdvisor ? userTokenInfo.getId() : null,
//                                        isAdvisor ? null : userTokenInfo.getId(),
//                                        null, null,
//                                        null, pageIndex
//                                )
//                        )
//                        .status("ok")
//                        .build(),
//                HttpStatus.OK
//        );
//    }

    @DeleteMapping("removeReport/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeReport(
            HttpServletRequest request,
            @PathVariable @ObjectIdConstraint ObjectId id
    ) throws UnAuthException {
        adviceTagReportService.removeReport(
                getUserId(request), id
        );
    }

}
