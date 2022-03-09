package irysc.gachesefid.Models;

public enum KindQuiz {

    OPEN, REGULAR, ONLINE, TASHRIHI;

    public String getName() {
        return name().toLowerCase();
    }
}
