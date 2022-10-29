package irysc.gachesefid.Controllers.Content;

import irysc.gachesefid.Exception.InvalidFieldsException;
import org.bson.Document;
import org.bson.types.ObjectId;

import static irysc.gachesefid.Main.GachesefidApplication.contentRepository;

public class Utility {

    static Document returnIfNoRegistry(ObjectId id) throws InvalidFieldsException {

        Document doc = contentRepository.findById(id);
        if(doc == null)
            throw new InvalidFieldsException("id is not valid");

        if(doc.getList("users", Document.class).size() > 0)
            throw new InvalidFieldsException("بسته موردنظر خریده شده است و این امکان وجود ندارد.");

        return doc;

    }

}
