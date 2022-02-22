package irysc.gachesefid.Validator;

import org.json.JSONObject;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;

public class StrongJSONValidator implements
        ConstraintValidator<StrongJSONConstraint, String> {

    private String[] valueList = null;
    private Class[] valueListType = null;
    private String[] optionalValueList = null;
    private Class[] optionalValueListType = null;

    @Override
    public void initialize(StrongJSONConstraint constraintAnnotation) {
        valueList = constraintAnnotation.params();
        valueListType = constraintAnnotation.paramsType();
        optionalValueList = constraintAnnotation.optionals();
        optionalValueListType = constraintAnnotation.optionalsType();
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {

        if (valueList.length == 0 && (s == null || s.isEmpty()))
            return true;

        try {
            JSONObject jsonObject = new JSONObject(s);

            if (jsonObject.keySet().size() < valueList.length ||
                    jsonObject.keySet().size() > valueList.length + optionalValueList.length)
                return false;

            int i = 0;

            for (String itr : valueList) {
                if (!jsonObject.has(itr) || !checkClasses(valueListType[i], jsonObject.get(itr).getClass()))
                    return false;

                i++;
            }

            List<String> l1 = Arrays.asList(valueList);
            List<String> l2 = Arrays.asList(optionalValueList);
            int idx;

            for (String key : jsonObject.keySet()) {

                if (jsonObject.get(key) instanceof String && jsonObject.getString(key).isEmpty())
                    return false;

                idx = l1.indexOf(key);
                if (idx != -1)
                    continue;

                idx = l2.indexOf(key);

                if (idx == -1 || !checkClasses(optionalValueListType[idx], jsonObject.get(key).getClass()))
                    return false;
            }

            return true;
        } catch (Exception x) {
            return false;
        }
    }

    private boolean checkClasses(Class a, Class b) {

        if(a.equals(Object.class))
            return true;

        if(a.equals(Number.class) && (b.equals(Integer.class) || b.equals(Double.class) || b.equals(Float.class)))
            return true;

        return a.equals(b);
    }
}
