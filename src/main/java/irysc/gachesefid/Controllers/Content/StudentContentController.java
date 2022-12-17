package irysc.gachesefid.Controllers.Content;


import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.contentRepository;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.generateSuccessMsg;

public class StudentContentController {

    public static String distinctTags() {
        return generateSuccessMsg("data", contentRepository.distinctTags("tags"));
    }

    public static String distinctTeachers() {
        return generateSuccessMsg("data", contentRepository.distinctTags("teacher"));
    }

    public static String getAll(ObjectId userId,
                                boolean isAdmin,
                                String tag,
                                String title,
                                String teacher,
                                Boolean visibility,
                                Boolean hasCert,
                                Integer minPrice,
                                Integer maxPrice) {

        ArrayList<Bson> filters = new ArrayList<>();
        if(!isAdmin)
            filters.add(eq("visibility", true));

        if(!isAdmin && userId != null)
            filters.add(nin("users._id", userId));

        if(title != null)
            filters.add(regex("title", Pattern.compile(Pattern.quote(title), Pattern.CASE_INSENSITIVE)));

        if(tag != null)
            filters.add(in("tags", tag));

        if(hasCert != null)
            filters.add(exists("cert_id", hasCert));

        if(minPrice != null)
            filters.add(lte("price", minPrice));

        if(maxPrice != null)
            filters.add(gte("price", maxPrice));

        if(isAdmin) {

            if(visibility != null)
                filters.add(eq("visibility", visibility));

            if(teacher != null)
                filters.add(eq("teacher", teacher));

        }

        JSONArray data = new JSONArray();
        ArrayList<Document> docs = contentRepository.find(
                filters.size() == 0 ? null : and(filters),
                isAdmin ? CONTENT_DIGEST_FOR_ADMIN : CONTENT_DIGEST
        );

        for(Document doc : docs)
            data.put(irysc.gachesefid.Controllers.Content.Utility.convertDigest(doc, isAdmin));

        return generateSuccessMsg("data", data);
    }

    public static String get(boolean isAdmin, ObjectId userId, String slug) {

        Document content = contentRepository.findBySecKey(slug);
        if(content == null)
            return JSON_NOT_VALID_ID;

        return generateSuccessMsg("data", irysc.gachesefid.Controllers.Content.Utility.convert(
                content, isAdmin,
                isAdmin || userId != null && irysc.gachesefid.Utility.Utility.searchInDocumentsKeyValIdx(
                        content.getList("users", Document.class), "_id", userId
                ) != -1, true)
        );
    }

}
