package irysc.gachesefid.Models;

public enum GiftType {

    COIN, MONEY, OFFCODE, FREE;

    public String getName() {
        return name().toLowerCase();
    }
}
