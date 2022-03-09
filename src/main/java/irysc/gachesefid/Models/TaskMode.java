package irysc.gachesefid.Models;

public enum TaskMode {

    HW, QUIZ;

    public String getName() {
        return name().toLowerCase();
    }
}
