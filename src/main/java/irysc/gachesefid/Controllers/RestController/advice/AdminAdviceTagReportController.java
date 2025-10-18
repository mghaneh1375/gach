package irysc.gachesefid.Controllers.RestController.advice;

import irysc.gachesefid.Dto.dashboard.Advisor.ReportProblemDigestDto;
import irysc.gachesefid.Dto.ResponseDto;
import irysc.gachesefid.Dto.advice.AdviceTagReportDto;
import irysc.gachesefid.Dto.advice.CreateAdviceTagReportDto;
import irysc.gachesefid.Service.advice.AdviceTagReportService;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;

@RestController
@Validated
@RequestMapping(value = "/api/admin/advice_tag_report")
public class AdminAdviceTagReportController {

    @Autowired
    private AdviceTagReportService adviceTagReportService;

    @PutMapping(value = "setReportAsSeen/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setReportAsSeen(@PathVariable @ObjectIdConstraint ObjectId id) {
        adviceTagReportService.setReportAsSeen(id);
    }

    @GetMapping(value = "getAdviceReports")
    @ResponseBody
    public ResponseEntity<ResponseDto<List<ReportProblemDigestDto>>> getAdviceReports(
            @RequestParam(name = "pageIndex") @Min(1) @Max(100000) int pageIndex,
            @RequestParam(required = false, name = "needTotalCount") Boolean needTotalCount,
            @RequestParam(required = false, name = "advisorId") ObjectId advisorId,
            @RequestParam(required = false, name = "from") Long from,
            @RequestParam(required = false, name = "to") Long to,
            @RequestParam(required = false, name = "showJustUnSeen") Boolean showJustUnSeen,
            @RequestParam(required = false, name = "justSendFromStudent") Boolean justSendFromStudent,
            @RequestParam(required = false, name = "justSendFromTeacher") Boolean justSendFromTeacher
    ) {
        return adviceTagReportService.getAdviceReports(
                from, to, showJustUnSeen, advisorId, null,
                justSendFromStudent, justSendFromTeacher,
                pageIndex, needTotalCount
        );
    }

    @GetMapping(value = "getAllReportTags")
    @ResponseBody
    public ResponseEntity<ResponseDto<List<AdviceTagReportDto>>> getAllReportTags() {
        return adviceTagReportService.getAllReportTags(null, true);
    }

    @PostMapping(value = "createReportTag")
    @ResponseBody
    public ResponseEntity<String> createReportTag(
            @RequestBody @Valid CreateAdviceTagReportDto dto
    ) {
        return adviceTagReportService.createTag(dto);
    }

    @PutMapping(value = "editReportTag/{tagId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editReportTag(
            @PathVariable @ObjectIdConstraint ObjectId tagId,
            @RequestBody @Valid CreateAdviceTagReportDto dto
    ) {
        adviceTagReportService.editTag(tagId, dto);
    }

    @DeleteMapping(value = "removeTags")
    @ResponseBody
    public String removeTags(
            @RequestBody @StrongJSONConstraint(
                    params = {"items"}, paramsType = {JSONArray.class}
            ) String jsonStr
    ) {
        return adviceTagReportService.removeTags(new JSONObject(jsonStr).getJSONArray("items"));
    }
}
