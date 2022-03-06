package irysc.gachesefid.Validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = JSONValidator.class)
@Target( { ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface JSONConstraint {

    String[] params();
    String[] optionals() default {};
    String message() default "Invalid json";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
