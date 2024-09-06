package irysc.gachesefid.DB;

import irysc.gachesefid.Utility.Cache;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public class Repository {

    static ConcurrentHashMap<String, ArrayList<Cache>> generalCached = new ConcurrentHashMap<>();

    public static void clearCache(String table) {
        if (generalCached.containsKey(table))
            generalCached.put(table, new ArrayList<>());
    }

    static Document isInCache(String section, Object id) {

        if (!generalCached.containsKey(section))
            return null;

        ArrayList<Cache> cached = generalCached.get(section);

        for (int i = 0; i < cached.size(); i++) {

            if (cached.get(i).equals(id)) {

                if (cached.get(i).checkExpiration())
                    return (Document) cached.get(i).getValue();

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


    static void removeBatchFromCache(String section, List<Object> ids) {

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

        if(secKey != null)
            cached.add(new Cache(expirationSec, doc, doc.getObjectId("_id"), secKey));
        else
            cached.add(new Cache(expirationSec, doc, doc.getObjectId("_id")));
    }

}
