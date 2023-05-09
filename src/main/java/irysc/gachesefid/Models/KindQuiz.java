package irysc.gachesefid.Models;

public enum KindQuiz {

    OPEN, REGULAR, HYBRID, ONLINE, TASHRIHI;

    public String getName() {
        return name().toLowerCase();
    }
}
