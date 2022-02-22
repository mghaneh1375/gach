package irysc.gachesefid.Validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = StrongJSONValidator.class)
@Target( { ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongJSONConstraint {

    String[] params();
    Class[] paramsType();
    String[] optionals() default {};
    Class[] optionalsType() default {};
    String message() default "Invalid json";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
