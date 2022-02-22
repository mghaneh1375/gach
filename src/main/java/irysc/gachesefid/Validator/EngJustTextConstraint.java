package irysc.gachesefid.Validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EngJustTextValidator.class)
@Target( { ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface EngJustTextConstraint {
    String message() default "Please type just english letters";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
