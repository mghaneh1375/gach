package irysc.gachesefid.Models;

public enum TicketPriority {

    HIGH, AVG, LOW;

    public String getName() {
        return name().toLowerCase();
    }
}
