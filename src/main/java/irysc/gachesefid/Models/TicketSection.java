package irysc.gachesefid.Models;

public enum TicketSection {

    QUIZ, CLASS, UPGRADELEVEL, REQUESTMOENY, ACCESS, ADVISOR;

    public String getName() {
        return name().toLowerCase();
    }
}
