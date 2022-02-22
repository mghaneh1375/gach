package irysc.gachesefid.Controllers;

import java.util.Timer;
import java.util.TimerTask;

import static irysc.gachesefid.Security.JwtTokenFilter.blackListTokens;
import static irysc.gachesefid.Security.JwtTokenFilter.validateTokens;

public class Jobs implements Runnable {

    @Override
    public void run() {
        Timer timer = new Timer();
        timer.schedule(new TokenHandler(), 0, 86400000); // 1 day
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

}
