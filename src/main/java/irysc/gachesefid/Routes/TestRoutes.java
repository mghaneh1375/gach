package irysc.gachesefid.Routes;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;


@RestController
@RequestMapping(path="/test")
public class TestRoutes {

    private final static Random random = new Random();


}
