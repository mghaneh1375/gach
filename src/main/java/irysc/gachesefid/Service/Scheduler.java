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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Main.GachesefidApplication.rssRepository;

@Component
public class Scheduler {

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

}
