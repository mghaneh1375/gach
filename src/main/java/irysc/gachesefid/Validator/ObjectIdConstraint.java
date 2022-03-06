package irysc.gachesefid.Validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ObjectIdValidator.class)
@Target( { ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ObjectIdConstraint {
    String message() default "Invalid Object Id";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
