package irysc.gachesefid.Models;

public enum HWAnswerType {

    WORD, PDF, AUDIO, VIDEO, IMAGE, POWERPOINT;

    public String getName() {
        return name().toLowerCase();
    }
}
