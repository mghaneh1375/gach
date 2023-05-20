package irysc.gachesefid.Models;

public enum AllKindQuiz {

    IRYSC, SCHOOL, CUSTOM, OPEN, CONTENT, HW, ONLINESTANDING;

    public String getName() {

        if(name().equalsIgnoreCase("onlineStanding"))
            return "onlineStanding";

        return name().toLowerCase();
    }
}
