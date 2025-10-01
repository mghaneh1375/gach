package irysc.gachesefid.Service.Dashboard;

import com.mongodb.client.model.Sorts;
import irysc.gachesefid.Dto.Dashboard.NotifDigestDto;
import irysc.gachesefid.Dto.Dashboard.TicketDigestDto;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.notifRepository;
import static irysc.gachesefid.Main.GachesefidApplication.ticketRepository;
import static irysc.gachesefid.Utility.StaticValues.ONE_WEEK_MIL_SEC;
import static irysc.gachesefid.Utility.StaticValues.TICKET_PROJECTION;

@Service
public class DashboardUtil {

    public List<NotifDigestDto> getMyLastNotifs(Document user) {
        return notifRepository.notifs(
                user.getList("events", Document.class)
                        .stream()
                        .filter(event -> !event.getBoolean("seen"))
                        .sorted(Collections.reverseOrder(Comparator.comparing(o -> o.getLong("created_at"))))
                        .limit(3)
                        .map(event -> event.getObjectId("notif_id"))
                        .collect(Collectors.toList())
        );
    }

    public List<TicketDigestDto> getMyLastTickets(ObjectId userId) {
        List<Document> docs =
                ticketRepository.findLimited(
                        and(
                                eq("user_id", userId),
                                gt("created_at", System.currentTimeMillis() - ONE_WEEK_MIL_SEC)
                        ),
                        TICKET_PROJECTION,
                        Sorts.descending("send_date"), 0, 5
                );
        List<TicketDigestDto> tickets = new ArrayList<>();
        docs.forEach((Consumer<? super Document>) document -> tickets.add(
                TicketDigestDto.convertDocToDto(document)
        ));

        return tickets;
    }
}
