package irysc.gachesefid.Validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = LinkValidator.class)
@Target( { ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface LinkConstraint {
    String message() default "Invalid link";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
