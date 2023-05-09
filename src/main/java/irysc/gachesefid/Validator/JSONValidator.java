package irysc.gachesefid.Validator;

import org.json.JSONObject;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;

public class JSONValidator implements
    ConstraintValidator<JSONConstraint, String> {

    private String[] valueList = null;
    private String[] optionalValueList = null;

    @Override
    public void initialize(JSONConstraint constraintAnnotation) {
        valueList = constraintAnnotation.params();
        optionalValueList = constraintAnnotation.optionals();
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {

        if(valueList.length == 0 && (s == null || s.isEmpty()))
            return true;

        try {
            JSONObject jsonObject = new JSONObject(s);

            if(jsonObject.keySet().size() < valueList.length ||
                    jsonObject.keySet().size() > valueList.length + optionalValueList.length)
                return false;

            for (String itr : valueList) {
                if(!jsonObject.has(itr)) {
                    return false;
                }
            }

            List<String> l1 = Arrays.asList(valueList);
            List<String> l2 = Arrays.asList(optionalValueList);

            for(String key : jsonObject.keySet()) {
                if(!l1.contains(key) && !l2.contains(key))
                    return false;
            }

            return true;
        }
        catch (Exception x) {
            return false;
        }
    }
}
