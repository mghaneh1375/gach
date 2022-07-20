package irysc.gachesefid.Models;

public class FormField {

    public boolean isMandatory;
    public String title;
    public String key;
    public String help;
    public boolean isJustNum;

    public FormField(boolean isMandatory, String key, String title,
                     String help, boolean isJustNum
    ) {
        this.isMandatory = isMandatory;
        this.key = key;
        this.title = title;
        this.help = help;
        this.isJustNum = isJustNum;
    }
}
