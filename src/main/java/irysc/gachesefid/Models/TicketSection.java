package irysc.gachesefid.Models;

public enum TicketSection {

    QUIZ, CLASS, UPGRADELEVEL, REQUESTMOENY;

    public String getName() {
        return name().toLowerCase();
    }
}
