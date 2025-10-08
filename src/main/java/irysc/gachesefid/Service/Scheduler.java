package irysc.gachesefid.Service;


import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;

@Component
public class Scheduler {

    @Autowired
    private GeneralCacheService generalCacheService;

    private final static String RSS_URL = "https://www.irysc.com/category/irysc-news/gachesefid/feed";

    @Scheduled(cron = "0 40 20 * * *")
    public void rss() {

        List<Document> news = new ArrayList<>();

        try {

            try (XmlReader reader = new XmlReader(new URL(RSS_URL))) {
                SyndFeed feed = new SyndFeedInput().build(reader);

                for (SyndEntry entry : feed.getEntries()) {

                    List<String> contents = entry.getContents().stream().map(SyndContent::getValue).collect(Collectors.toList());
                    String img = null;
                    int idx = 0;

                    while (img == null && idx < contents.size()) {
                        org.jsoup.nodes.Document html = Jsoup.parse(contents.get(idx));
                        Elements elements = html.getElementsByTag("img");

                        if(elements.size() > 0)
                            img = elements.get(0).attr("src");
                        idx++;
                    }

                    news.add(new Document()
                            .append("link", entry.getLink())
                            .append("description", entry.getDescription().getValue())
                            .append("title", entry.getTitle())
                            .append("date_ts", entry.getPublishedDate().getTime())
                            .append("date", Utility.getSolarDate(entry.getPublishedDate().getTime()))
                            .append("main_image", img)
                            .append("categories", entry.getCategories().stream().map(SyndCategory::getName).collect(Collectors.toList()))
                            .append("contents", contents)
                    );
                }
            }

            news.sort(Comparator.comparing(o -> o.getLong("date_ts"), Comparator.reverseOrder()));

            int today = Utility.getToday();

            rssRepository.deleteMany(eq("date", today));
            rssRepository.insertOne(new Document("news", news).append("today", today));

        }  catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Scheduled(cron = "0 0 */2 * * ?")
    public void refreshGeneralCache() {
        generalCacheService.getInfo();
    }

    @Scheduled(cron = "0 0 4 * * ?")
    public void removeExpiredAdviceRequests() {
        long curr = System.currentTimeMillis();
        contentRepository.deleteMany(and(
                and(
                        eq("answer", "accept"),
                        exists("paid_at", false),
                        lt("answer_at", curr - PAY_ADVICE_REQUEST_EXPIRATION_MSEC)
                )
        ));
        contentRepository.deleteMany(and(
                and(
                        eq("answer", "pending"),
                        lt("created_at", curr - ANSWER_ADVICE_REQUEST_EXPIRATION_MSEC)
                )
        ));
    }

    @Scheduled(cron = "0 30 4 * * ?")
    public void removeExpiredTeachSchedules() {
        teachScheduleRepository.deleteMany(and(
                exists("students", false),
                or(
                        and(
                                exists("start_at"),
                                lte("start_at", System.currentTimeMillis() - ONE_MONTH_MIL_SEC)
                        ),
                        and(
                                exists("end_date"),
                                lte("end_date", System.currentTimeMillis() - ONE_MONTH_MIL_SEC)
                        )
                )
        ));
    }
}
