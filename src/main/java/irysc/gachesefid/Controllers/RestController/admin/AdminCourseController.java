package irysc.gachesefid.Controllers.RestController.admin;

import irysc.gachesefid.Dto.courseIntroduction.CreateCourseIntroductionDto;
import irysc.gachesefid.Service.admin.AdminCourseService;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/admin/course")
public class AdminCourseController {

    private final AdminCourseService adminCourseService;

    @GetMapping(value = "/list")
    @ResponseBody
    public String list() {
        return adminCourseService.list();
    }

    @PostMapping(value = "/")
    @ResponseBody
    public String store(
            @RequestBody @NotNull @Valid CreateCourseIntroductionDto dto
    ) {
        return adminCourseService.store(dto);
    }

    @PutMapping(value = "/{id}")
    @ResponseBody
    public String update(
            @PathVariable @ObjectIdConstraint ObjectId id,
            @RequestBody @NotNull @Valid CreateCourseIntroductionDto dto
            ) {
        return adminCourseService.update(id, dto);
    }

    @DeleteMapping(value = "/{id}")
    public String remove(
            @PathVariable @ObjectIdConstraint ObjectId id
    ) {
        return adminCourseService.remove(id);
    }

}
