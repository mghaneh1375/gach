package irysc.gachesefid.Validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EngValidator.class)
@Target( { ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface EngConstraint {
    String message() default "Please type english";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
