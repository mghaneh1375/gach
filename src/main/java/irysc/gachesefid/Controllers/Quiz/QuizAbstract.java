package irysc.gachesefid.Controllers.Quiz;

import org.bson.Document;
import org.json.JSONObject;

abstract class QuizAbstract {

    abstract Document registry(Document student, Document quiz, int paid);

    abstract void quit(Document student, Document quiz);

    abstract String buy(Document student, Document quiz);

    abstract JSONObject convertDocToJSON(Document quiz, boolean isDigest, boolean isAdmin);
}
