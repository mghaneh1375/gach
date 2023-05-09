package irysc.gachesefid.Models;

public enum TicketSection {

    QUIZ, CLASS, UPGRADELEVEL, REQUESTMOENY, ACCESS;

    public String getName() {
        return name().toLowerCase();
    }
}
