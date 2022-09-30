package irysc.gachesefid.Controllers;

import java.util.Timer;
import java.util.TimerTask;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Security.JwtTokenFilter.blackListTokens;
import static irysc.gachesefid.Security.JwtTokenFilter.validateTokens;
import static irysc.gachesefid.Utility.StaticValues.*;

public class Jobs implements Runnable {

    @Override
    public void run() {
        Timer timer = new Timer();
        timer.schedule(new TokenHandler(), 0, 86400000); // 1 day
        timer.schedule(new SiteStatsHandler(), 86400000, 86400000); // 1 day
        timer.schedule(new RemoveRedundantCustomQuizzes(), 0, 86400000);
    }

    class TokenHandler extends TimerTask {

        public void run() {

            synchronized (validateTokens) {
                validateTokens.removeIf(itr -> !itr.isValidateYet());
            }

            synchronized (blackListTokens) {
                blackListTokens.removeIf(itr -> itr.getValue() < System.currentTimeMillis());
            }

        }
    }


    class SiteStatsHandler extends TimerTask {

        public void run() {
            SCHOOLS = schoolRepository.count(exists("user_id"));
            QUESTIONS = questionRepository.count(null);
            STUDENTS = userRepository.count(eq("level", false));
        }
    }

    class RemoveRedundantCustomQuizzes extends TimerTask {

        @Override
        public void run() {

            customQuizRepository.deleteMany(
                    and(
                            eq("status", "wait"),
                            lt("created_at", System.currentTimeMillis() - 1200000) // 20min
                    )
            );

        }
    }
}
