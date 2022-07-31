package irysc.gachesefid.Models;

import irysc.gachesefid.Kavenegar.utils.PairValue;

import java.util.ArrayList;
import java.util.Arrays;

public class FormField {

    public boolean isMandatory;
    public String title;
    public String key;
    public String help;
    public boolean isJustNum;
    public ArrayList<PairValue> pairValues = null;

    public FormField(boolean isMandatory, String key, String title,
                     String help, boolean isJustNum
    ) {
        this.isMandatory = isMandatory;
        this.key = key;
        this.title = title;
        this.help = help;
        this.isJustNum = isJustNum;
    }

    public FormField(boolean isMandatory, String key, String title,
                     String help, PairValue ...pairValues
    ) {
        this.isMandatory = isMandatory;
        this.key = key;
        this.title = title;
        this.help = help;
        this.isJustNum = false;
        this.pairValues = new ArrayList<>();
        this.pairValues.addAll(Arrays.asList(pairValues));
    }
}
