package irysc.gachesefid.models;

public enum KindQuiz {

    OPEN, REGULAR, ONLINE;

    public String getName() {
        return name().toLowerCase();
    }
}
