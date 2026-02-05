package irysc.gachesefid.Controllers.RestController.admin;

import irysc.gachesefid.Dto.courseIntroduction.AddSeoToIntroductionCourseDto;
import irysc.gachesefid.Dto.courseIntroduction.CreateCourseIntroductionDto;
import irysc.gachesefid.Service.admin.AdminCourseIntroductionService;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/admin/course")
public class AdminCourseController {

    private final AdminCourseIntroductionService adminCourseIntroductionService;

    @GetMapping(value = "/list")
    @ResponseBody
    public String list() {
        return adminCourseIntroductionService.list();
    }

    @PostMapping(value = "/")
    @ResponseBody
    public String store(
            @RequestBody @NotNull @Valid CreateCourseIntroductionDto dto
    ) {
        return adminCourseIntroductionService.store(dto);
    }

    @PutMapping(value = "/{id}")
    @ResponseBody
    public String update(
            @PathVariable @ObjectIdConstraint ObjectId id,
            @RequestBody @NotNull @Valid CreateCourseIntroductionDto dto
            ) {
        return adminCourseIntroductionService.update(id, dto);
    }

    @DeleteMapping(value = "/")
    @ResponseBody
    public String remove(
            @RequestBody @StrongJSONConstraint(
                    params = {"items"},
                    paramsType = {JSONArray.class}
            ) @NotBlank String jsonStr
    ) {
        return adminCourseIntroductionService.remove(
                new JSONObject(jsonStr).getJSONArray("items")
        );
    }

    @PutMapping("/seo/{id}")
    @ResponseBody
    public String addSeoTag(
            @PathVariable @ObjectIdConstraint ObjectId id,
            @RequestBody @NotNull @Valid AddSeoToIntroductionCourseDto dto
    ) {
        return adminCourseIntroductionService.addSeoTag(id, dto);
    }

    @DeleteMapping("/seo/{id}")
    @ResponseBody
    public String removeSeoTag(
            @PathVariable @ObjectIdConstraint ObjectId id,
            @RequestBody @NotBlank @StrongJSONConstraint(
                    params = {"key"}, paramsType = {String.class}
            ) String jsonStr
    ) {
        return adminCourseIntroductionService.removeSeoTag(id, new JSONObject(jsonStr).getString("key"));
    }

    @GetMapping("/seo/{id}")
    @ResponseBody
    public String getSeoTags(
            @PathVariable @ObjectIdConstraint ObjectId id
    ) {
        return adminCourseIntroductionService.getSeoTags(id);
    }
}
