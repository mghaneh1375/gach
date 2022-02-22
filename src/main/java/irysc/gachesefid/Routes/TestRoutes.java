package irysc.gachesefid.Routes;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Random;


@Controller
@RequestMapping(path="/test")
public class TestRoutes {

    private final static Random random = new Random();


}
