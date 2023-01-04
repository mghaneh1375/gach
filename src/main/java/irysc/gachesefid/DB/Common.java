package irysc.gachesefid.DB;


import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UnwindOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Variable;
import com.mongodb.client.model.WriteModel;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.subjectRepository;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.StaticValues.*;

public abstract class Common extends Repository {

    MongoCollection<Document> documentMongoCollection = null;
    String table = "";
    String secKey = "";

    public boolean exist(Bson filter) {
        return documentMongoCollection.countDocuments(filter) > 0;
    }

    public ArrayList<Document> find(Bson filter, Bson project) {

        FindIterable<Document> cursor;

        if (project == null) {
            if (filter == null)
                cursor = documentMongoCollection.find();
            else
                cursor = documentMongoCollection.find(filter);
        } else {
            if (filter == null)
                cursor = documentMongoCollection.find().projection(project);
            else
                cursor = documentMongoCollection.find(filter).projection(project);
        }

        ArrayList<Document> result = new ArrayList<>();

        for (Document doc : cursor)
            result.add(doc);

        return result;
    }

    public ArrayList<ObjectId> findJustIds(Bson filter) {

        FindIterable<Document> cursor;

        if (filter == null)
            cursor = documentMongoCollection.find().projection(JUST_ID);
        else
            cursor = documentMongoCollection.find(filter).projection(JUST_ID);

        ArrayList<ObjectId> result = new ArrayList<>();

        for (Document doc : cursor)
            result.add(doc.getObjectId("_id"));

        return result;
    }

    public int count(Bson filter) {
        if (filter == null)
            return (int) documentMongoCollection.countDocuments();

        return (int) documentMongoCollection.countDocuments(filter);
    }

    public ArrayList<Document> find(Bson filter, Bson project, Bson orderBy) {

        FindIterable<Document> cursor;

        if (project == null) {
            if (filter == null)
                cursor = documentMongoCollection.find().sort(orderBy);
            else
                cursor = documentMongoCollection.find(filter).sort(orderBy);
        } else {
            if (filter == null)
                cursor = documentMongoCollection.find().projection(project).sort(orderBy);
            else
                cursor = documentMongoCollection.find(filter).projection(project).sort(orderBy);
        }

        ArrayList<Document> result = new ArrayList<>();

        for (Document doc : cursor)
            result.add(doc);

        return result;
    }

    public Document findOne(Bson filter, Bson project) {

        FindIterable<Document> cursor;
        if (project == null)
            cursor = documentMongoCollection.find(filter);
        else
            cursor = documentMongoCollection.find(filter).projection(project);

        if (cursor.iterator().hasNext())
            return cursor.iterator().next();

        return null;
    }

    public Document findOne(Bson filter, Bson project, Bson orderBy) {

        FindIterable<Document> cursor;
        if (project == null)
            cursor = documentMongoCollection.find(filter).sort(orderBy);
        else
            cursor = documentMongoCollection.find(filter).projection(project).sort(orderBy);

        if (cursor.iterator().hasNext())
            return cursor.iterator().next();

        return null;
    }

    public Document findOneAndDelete(Bson filter) {

        Document deleted = documentMongoCollection.findOneAndDelete(filter);

        if (deleted != null)
            removeFromCache(table, deleted.getObjectId("_id"));

        return deleted;
    }

    public Document findOneAndUpdate(Bson filter, Bson update) {
        return documentMongoCollection.findOneAndUpdate(filter, update);
    }

    public Document findOneAndUpdate(ObjectId id, Bson update) {
        return documentMongoCollection.findOneAndUpdate(eq("_id", id), update);
    }

    public synchronized Document findById(ObjectId id) {

        Document cached = isInCache(table, id);
        if (cached != null)
            return cached;

        FindIterable<Document> cursor = documentMongoCollection.find(eq("_id", id));
        if (cursor.iterator().hasNext()) {
            Document doc = cursor.iterator().next();

            if (!table.isEmpty()) {
                if (secKey.isEmpty())
                    addToCache(table, doc, CLASS_LIMIT_CACHE_SIZE, CLASS_EXPIRATION_SEC);
                else
                    addToCache(table, doc, secKey, CLASS_LIMIT_CACHE_SIZE, CLASS_EXPIRATION_SEC);
            }

            return doc;
        }

        return null;
    }

    public synchronized void clearBatchFromCache(ArrayList<Object> ids) {
        removeBatchFromCache(table, ids);
    }

    public synchronized void clearFromCache(Object o) {
        removeFromCache(table, o);
    }

    public synchronized Document findBySecKey(Object val) {

        Document cached = isInCache(table, val);
        if (cached != null)
            return cached;

        FindIterable<Document> cursor = documentMongoCollection.find(eq(secKey, val));

        if (cursor.iterator().hasNext()) {

            Document doc = cursor.iterator().next();

            if (!table.isEmpty())
                addToCache(table, doc, val, CLASS_LIMIT_CACHE_SIZE, CLASS_EXPIRATION_SEC);

            return doc;
        }

        return null;
    }

    public String insertOneWithReturn(Document document) {
        documentMongoCollection.insertOne(document);
        return new JSONObject().put("status", "ok")
                .put("id", document.getObjectId("_id").toString())
                .toString();
    }

    public ObjectId insertOneWithReturnId(Document document) {
        documentMongoCollection.insertOne(document);
        return document.getObjectId("_id");
    }

    public void insertOne(Document newDoc) {
        documentMongoCollection.insertOne(newDoc);
    }

    public void updateOne(ObjectId objectId, Bson update) {
        documentMongoCollection.updateOne(eq("_id", objectId), update);
    }

    public void updateOne(Bson filter, Bson update) {
        documentMongoCollection.updateOne(filter, update);
    }

    public void updateOne(Bson filter, Bson update, UpdateOptions options) {
        documentMongoCollection.updateOne(filter, update, options);
    }

    public void updateMany(Bson filter, Bson update) {
        documentMongoCollection.updateMany(filter, update);
    }

    public void replaceOne(Bson filter, Document newDoc) {
        documentMongoCollection.replaceOne(filter, newDoc);
        removeFromCache(table, newDoc.getObjectId("_id"));
    }

    public void replaceOne(ObjectId id, Document newDoc) {
        documentMongoCollection.replaceOne(eq("_id", id), newDoc);
        removeFromCache(table, id);
    }

    abstract void init();

    public void deleteOne(Bson filter) {

        Document deleted = documentMongoCollection.findOneAndDelete(filter);

        if (deleted != null)
            removeFromCache(table, deleted.getObjectId("_id"));
    }

    public void deleteOne(ObjectId id) {

        Document deleted = documentMongoCollection.findOneAndDelete(
                eq("_id", id)
        );

        if (deleted != null)
            removeFromCache(table, deleted.getObjectId("_id"));
    }

    public void deleteMany(Bson filter) {

        FindIterable<Document> cursor = documentMongoCollection.
                find(filter)
                .projection(JUST_ID);

        for (Document doc : cursor)
            removeFromCache(table, doc.getObjectId("_id"));

        documentMongoCollection.deleteMany(filter);
    }

    public AggregateIterable<Document> findWithJoinUser(String userKey, String finalUserKey,
                                                        Bson match, Bson project,
                                                        Bson sortFilter) {

        List<Bson> filters = new ArrayList<>();

        if (match != null)
            filters.add(match);

        if (userKey != null) {

            filters.add(lookup("user",
                    Collections.singletonList(new Variable<>("myId", "$" + userKey)), Arrays.asList(
                            match(expr(new Document("$eq", Arrays.asList("$_id", "$$myId")))),
                            project(USER_DIGEST)
                    ), finalUserKey));

            filters.add(unwind("$" + finalUserKey, new UnwindOptions().preserveNullAndEmptyArrays(true)));

        }

        if (project != null)
            filters.add(project);

        if (sortFilter != null)
            filters.add(sort(sortFilter));

        return documentMongoCollection.aggregate(filters);
    }


    // read only
    public ArrayList<Document> findByIds(List<ObjectId> objectIds, boolean preserveOrder) {

        ArrayList<Document> infos = new ArrayList<>();
        ArrayList<Document> tmp = new ArrayList<>();

//        if(preserveOrder) {
//            for(int i = 0; i < objectIds.size(); i++)
//                infos.add(null);
//        }

        FindIterable<Document> cursor =
                documentMongoCollection.find(in("_id", objectIds));

        for (Document doc : cursor)
            tmp.add(doc);

        for(ObjectId oId : objectIds) {

            boolean find = false;
            for (Document doc : tmp) {
                if(doc.getObjectId("_id").equals(oId)) {
                    infos.add(doc);
                    find = true;
                    break;
                }
            }

            if(!find)
                return null;
        }

        if(1 == 1)
            return infos;

        if(preserveOrder) {
            int idx, counter = 0;
            for (Document doc : cursor) {
                idx = objectIds.indexOf(doc.getObjectId("_id"));
                infos.set(idx, doc);
                counter++;
            }
            if(counter != objectIds.size())
                return null;
        }
        else {

        }

        if (infos.size() != objectIds.size())
            return null;

        return infos;
    }

    public ArrayList<Document> findPreserveOrderWitNull(String key, List<ObjectId> objectIds) {

        ArrayList<Document> infos = new ArrayList<>();
        for(int i = 0; i < objectIds.size(); i++)
            infos.add(null);

        FindIterable<Document> cursor =
                documentMongoCollection.find(in(key, objectIds));

        int idx, counter = 0;
        for (Document doc : cursor) {
            idx = objectIds.indexOf(doc.getObjectId(key));
            if(idx != -1) {
                infos.set(idx, doc);
                counter++;
            }
        }
        if(counter != objectIds.size()) {
            idx = 0;
            for(Document doc : infos) {
                if(doc == null)
                    infos.set(idx, new Document(key, objectIds.get(idx))
                            .append("_id", new ObjectId())
                    );
                idx++;
            }
        }

        return infos;
    }

    public void bulkWrite(List<WriteModel<Document>> writes) {
        documentMongoCollection.bulkWrite(writes);
    }

    public void cleanRemove(Document doc) {
    }

    public void cleanReject(Document doc) {
    }

    public JSONArray distinctTags(String key) {
        DistinctIterable<String> cursor = documentMongoCollection.distinct(key, String.class);

        JSONArray jsonArray = new JSONArray();
        for (String itr : cursor)
            jsonArray.put(itr);

        return jsonArray;
    }
}
