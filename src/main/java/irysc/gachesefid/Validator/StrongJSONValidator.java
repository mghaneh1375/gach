package irysc.gachesefid.Validator;

import irysc.gachesefid.Utility.Positive;
import org.bson.types.ObjectId;
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
                if (!jsonObject.has(itr) || !checkClasses(valueListType[i], jsonObject.get(itr))) {
                    System.out.println(itr);
                    return false;
                }

                i++;
            }

            List<String> l1 = Arrays.asList(valueList);
            List<String> l2 = Arrays.asList(optionalValueList);
            int idx;

            for (String key : jsonObject.keySet()) {

                if (jsonObject.get(key) instanceof String && jsonObject.getString(key).isEmpty()) {
                    System.out.println(key);
                    return false;
                }

                idx = l1.indexOf(key);
                if (idx != -1)
                    continue;

                idx = l2.indexOf(key);
                if (idx == -1 || !checkClasses(optionalValueListType[idx], jsonObject.get(key))) {
                    System.out.println(key);
                    return false;
                }
            }
            return true;
        } catch (Exception x) {
            return false;
        }
    }

    private boolean checkClasses(Class a, Object value) {

        if(a.equals(Object.class))
            return true;

        if(a.equals(ObjectId.class))
            return ObjectIdValidator.isValid(value.toString());

        if(a.isEnum())
            return EnumValidatorImp.isValid(value.toString(), a);

        Class b = value.getClass();

        if(a.equals(Number.class) && (b.equals(Integer.class) || b.equals(Double.class) || b.equals(Float.class))) {
//            || b.equals(String.class)

            if(value instanceof Integer && (int)value < 0)
                return false;

            if(value instanceof Double && (double)value < 0)
                return false;

            if(value instanceof Float && (float)value < 0)
                return false;

//            if(value instanceof String) {
//
//                Number n;
//
//                try {
//                    n = Double.parseDouble(value.toString());
//                }
//                catch (Exception x) {
//                    try {
//                        n = Float.parseFloat(value.toString());
//                    }
//                    catch (Exception xx) {
//                        try {
//                            n = Integer.parseInt(value.toString());
//                        }
//                        catch (Exception xxx) {
//                            return false;
//                        }
//                    }
//                }
//
//                if(n.doubleValue() < 0)
//                    return false;
//
//                value = n.doubleValue();
//                return true;
//            }

            return true;
        }

        if(a.equals(Positive.class) && b.equals(Integer.class) && (int)value >= 0)
            return true;

        if(a.equals(Positive.class) && b.equals(String.class)) {

            try {
                value = Integer.parseInt(value.toString());
                return (int)value >= 0;
            }
            catch (Exception x) {
                return false;
            }
        }

        if(a.equals(String.class) && b.equals(Integer.class))
            return true;

        return a.equals(b);
    }

}
