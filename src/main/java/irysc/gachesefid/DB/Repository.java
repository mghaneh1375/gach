package irysc.gachesefid.DB;

import irysc.gachesefid.Utility.Cache;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.HashMap;


public class Repository {

    static HashMap<String, ArrayList<Cache>> generalCached = new HashMap<>();

    public static void clearCache(String table) {
        if (generalCached.containsKey(table))
            generalCached.put(table, new ArrayList<>());
    }

    static void updateCache(String section, Document newDoc) {

        if (!generalCached.containsKey(section))
            return;

        ArrayList<Cache> cached = generalCached.get(section);
        ObjectId id = newDoc.getObjectId("_id");

        for (int i = 0; i < cached.size(); i++) {

            if (cached.get(i).equals(id)) {

                if (cached.get(i).checkExpiration()) {
                    cached.get(i).setValue(newDoc);
                    return;
                }

                cached.remove(i);
                return;
            }

        }

    }

    static Document isInCache(String section, Object id) {

        if (!generalCached.containsKey(section))
            return null;

        ArrayList<Cache> cached = generalCached.get(section);

        for (int i = 0; i < cached.size(); i++) {

            if (cached.get(i).equals(id)) {

                if (cached.get(i).checkExpiration()) {
                    return (Document) cached.get(i).getValue();
                }

                cached.remove(i);
                return null;
            }

        }

        return null;
    }

    static void removeFromCache(String section, Object id) {

        if (!generalCached.containsKey(section))
            return;

        ArrayList<Cache> cached = generalCached.get(section);

        for (int i = 0; i < cached.size(); i++) {

            if (cached.get(i).equals(id)) {
                cached.remove(i);
                return;
            }

        }
    }


    static void removeBatchFromCache(String section, ArrayList<Object> ids) {

        if (!generalCached.containsKey(section))
            return;

        ArrayList<Cache> cached = generalCached.get(section);
        cached.removeIf(itr -> ids.contains(itr.getKey()));
    }


    static void addToCache(String section, Document doc, Object secKey, int limit, int expirationSec) {

        if (!generalCached.containsKey(section))
            generalCached.put(section, new ArrayList<>());

        ArrayList<Cache> cached = generalCached.get(section);

        if (cached.size() >= limit)
            cached.remove(0);

        cached.add(new Cache(expirationSec, doc, doc.getObjectId("_id"), secKey));
    }

    static void addToCache(String section, Document doc, int limit, int expirationSec) {

        if (!generalCached.containsKey(section))
            generalCached.put(section, new ArrayList<>());

        ArrayList<Cache> cached = generalCached.get(section);

        if (cached.size() >= limit)
            cached.remove(0);

        cached.add(new Cache(expirationSec, doc, doc.getObjectId("_id")));
    }

}
