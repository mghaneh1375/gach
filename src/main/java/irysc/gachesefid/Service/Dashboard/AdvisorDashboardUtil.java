package irysc.gachesefid.Service.Dashboard;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.model.Sorts;
import irysc.gachesefid.Dto.Dashboard.Advisor.CommentsAboutMeDigest;
import irysc.gachesefid.Dto.Dashboard.NotifDigestDto;
import irysc.gachesefid.Dto.Dashboard.TicketDigestDto;
import irysc.gachesefid.Models.CommentSection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;

@Service
public class AdvisorDashboardUtil {

    public List<CommentsAboutMeDigest> getMyLastComments(ObjectId userId) {
        List<Bson> filters = new ArrayList<>();
        filters.add(eq("ref_id", userId));
        filters.add(eq("status", "accept"));
        filters.add(or(
                eq("section", CommentSection.TEACH.getName()),
                eq("section", CommentSection.ADVISOR.getName())
        ));
        AggregateIterable<Document> docs =
                commentRepository.findWithJoinUser(
                        "user_id", "author",
                        match(and(filters)), null,
                        Sorts.descending("created_at"),
                        0, 3, project(USER_DIGEST)
                );
        List<CommentsAboutMeDigest> comments = new ArrayList<>();
        docs.forEach((Consumer<? super Document>) document -> comments.add(CommentsAboutMeDigest.buildFromDoc(document)));
        return comments;
    }

    public List<TicketDigestDto> getMyLastTickets(ObjectId userId) {
        ArrayList<Bson> constraints = new ArrayList<>() {
            {
                add(eq("advisor_id", userId));
                add(eq("section", "advisor"));
                add(eq("status", "pending"));
            }
        };
        AggregateIterable<Document> docs =
                ticketRepository.findWithJoinUser("user_id", "student",
                        match(and(constraints)),
                        project(TICKET_PROJECTION),
                        Sorts.descending("send_date"), 0, 5,
                        project(USER_DIGEST.append("accesses", 1))
                );
        List<TicketDigestDto> tickets = new ArrayList<>();
        docs.forEach((Consumer<? super Document>) document -> {
            tickets.add(
                    TicketDigestDto.convertDocToDto(document)
            );
        });

        return tickets;
    }

    public int getLastMonthSettled(ObjectId userId, long tilLastMonth) {
        return settlementRequestRepository.find(
                        and(
                                eq("user_id", userId),
                                eq("status", "paid"),
                                exists("paid_at"),
                                gte("paid_at", tilLastMonth)
                        ), JUST_AMOUNT
                )
                .stream().mapToInt(doc -> doc.getInteger("amount")).sum();
    }
}
