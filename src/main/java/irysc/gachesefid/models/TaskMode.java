package irysc.gachesefid.models;

public enum TaskMode {

    HW, QUIZ;

    public String getName() {
        return name().toLowerCase();
    }
}
