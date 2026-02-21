package irysc.gachesefid.Controllers.RestController.admin;

import irysc.gachesefid.Service.admin.AdminCommandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@Validated
@RequestMapping("/admin/command")
public class AdminCommandController {

    @Autowired
    private AdminCommandService adminCommandService;

//    @GetMapping(value = "syncStudentsInAdvisors")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    public void syncStudentsInAdvisors() {
//        adminCommandService.syncStudentsInAdvisors();
//    }
//
//    @GetMapping(value = "convertAttachesToDoc")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    public void convertAttachesToDoc() {
//        adminCommandService.convertAttachesToDoc();
//    }

    @GetMapping("test")
    public ResponseEntity<Set<String>> test() {
        return adminCommandService.normalizeTags();
    }
}
