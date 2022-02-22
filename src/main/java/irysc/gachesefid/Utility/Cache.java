package irysc.gachesefid.Utility;

public class Cache {

    private long created;
    private int validSec; // تا چند ثانیه بعد معتبر باشد
    private Object value;
    private Object key;
    private Object secondaryKey;

    public Cache(int validSec, Object value, Object key) {
        this.key = key;
        this.value = value;
        this.validSec = validSec;
        created = System.currentTimeMillis();
        secondaryKey = null;
    }

    public Cache(int validSec, Object value, Object key, Object secondaryKey) {
        this.key = key;
        this.value = value;
        this.validSec = validSec;
        created = System.currentTimeMillis();
        this.secondaryKey = secondaryKey;
    }

    @Override
    public boolean equals(Object o) {

        if(o == null || key == null)
            return false;

        if(o.getClass().getName().equals(key.getClass().getName()))
            return o.equals(key);

        if(secondaryKey != null &&
                o.getClass().getName().equals(secondaryKey.getClass().getName()))
            return o.equals(secondaryKey);

        return false;
    }

    public boolean checkExpiration() {
        return ((System.currentTimeMillis() - created) / 1000) <= validSec;
    }

    public Object getValue() {
        return value;
    }

    public Object getKey() {
        return key;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
