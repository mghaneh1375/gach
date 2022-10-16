package irysc.gachesefid.Models;

public enum ExchangeMode {

    COIN_TO_MONEY, MONEY_TO_COIN;

    public String getName() {
        return name().toLowerCase();
    }
}
