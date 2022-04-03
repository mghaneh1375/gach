package irysc.gachesefid.Controllers.Quiz;

import org.bson.Document;

abstract class QuizAbstract {

    abstract void registry(Document student, Document quiz, int paid);

    abstract void quit(Document student, Document quiz);

    abstract String buy(Document student, Document quiz);
}
