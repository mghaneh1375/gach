package irysc.gachesefid.Digests;

public class Question {

    private int id;
    private int ans;
    private int lessonId;
    private String lesson;

    public Question(int id, int ans, int lessonId, String lesson) {
        this.id = id;
        this.ans = ans;
        this.lessonId = lessonId;
        this.lesson = lesson;
    }

    public int getId() {
        return id;
    }

    public int getAns() {
        return ans;
    }

    public int getLessonId() {
        return lessonId;
    }

    public String getLesson() {
        return lesson;
    }
}
