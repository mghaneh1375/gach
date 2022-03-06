package irysc.gachesefid.Validator;

import org.bson.types.ObjectId;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import static irysc.gachesefid.Utility.StaticValues.MAX_OBJECT_ID_SIZE;
import static irysc.gachesefid.Utility.StaticValues.MIN_OBJECT_ID_SIZE;


public class ObjectIdValidator implements
        ConstraintValidator<ObjectIdConstraint, ObjectId> {


    @Override
    public void initialize(ObjectIdConstraint constraintAnnotation) {

    }

    @Override
    public boolean isValid(ObjectId o, ConstraintValidatorContext constraintValidatorContext) {
        return o.toString().length() >= MIN_OBJECT_ID_SIZE && o.toString().length() <= MAX_OBJECT_ID_SIZE;
    }

    public static boolean isValid(String str) {
        return str.length() >= MIN_OBJECT_ID_SIZE && str.length() <= MAX_OBJECT_ID_SIZE;
    }
}
