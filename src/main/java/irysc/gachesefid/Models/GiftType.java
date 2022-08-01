package irysc.gachesefid.Models;

public enum GiftType {

    COIN, MONEY, OFFCODE;

    public String getName() {
        return name().toLowerCase();
    }
}
