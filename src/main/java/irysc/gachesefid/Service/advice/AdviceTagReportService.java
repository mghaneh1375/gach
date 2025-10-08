package irysc.gachesefid.Service.advice;

import irysc.gachesefid.Dto.Dashboard.Advisor.ReportProblemDigestDto;
import irysc.gachesefid.Dto.ResponseDto;
import irysc.gachesefid.Dto.advice.CreateAdviceTagReportDto;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Models.TeachReportTagMode;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.EnumValidatorImp;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.JUST_ID;

@Service
public class AdviceTagReportService {

    private static final Integer PER_PAGE = 50;

    public ResponseEntity<String> createTag(CreateAdviceTagReportDto dto) {
        if (adviceTagReportRepository.exist(
                and(
                        exists("deleted_at", false),
                        eq("label", dto.getLabel())
                )
        ))
            throw new InvalidFieldsException("این تگ در سیستم موجود است");

        Document newDoc = new Document("label", dto.getLabel())
                .append("priority", dto.getPriority())
                .append("mode", dto.getMode())
                .append("visibility", dto.getVisibility())
                .append("created_at", System.currentTimeMillis());

        return new ResponseEntity<>(
                adviceTagReportRepository.insertOneWithReturn(newDoc),
                HttpStatus.OK
        );
    }

    public String removeTags(JSONArray jsonArray) {
        JSONArray doneIds = new JSONArray();
        JSONArray excepts = new JSONArray();

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                String id = jsonArray.getString(i);

                if (!ObjectId.isValid(id)) {
                    excepts.put(i + 1);
                    continue;
                }

                Document tmp = adviceTagReportRepository.findOneAndUpdate(
                        new ObjectId(id),
                        set("deleted_at", System.currentTimeMillis())
                );

                if (tmp == null) {
                    excepts.put(i + 1);
                    continue;
                }

                doneIds.put(id);
            } catch (Exception x) {
                excepts.put(i + 1);
            }
        }

        return Utility.returnRemoveResponse(excepts, doneIds);
    }

    public void editTag(ObjectId id, CreateAdviceTagReportDto dto) {
        Document doc = adviceTagReportRepository.findById(id);
        if (doc == null)
            throw new InvalidFieldsException("id is incorrect");

        if (!doc.getString("label").equals(dto.getLabel()) &&
                adviceTagReportRepository.exist(
                        and(
                                exists("deleted_at", false),
                                eq("label", dto.getLabel())
                        )
                ))
            throw new InvalidFieldsException("این تگ در سیستم موجود است");

        doc.put("label", dto.getLabel());
        doc.put("priority", dto.getPriority());
        doc.put("mode", dto.getMode());

        adviceTagReportRepository.replaceOneWithoutClearCache(id, doc);
    }

    public ResponseEntity<ResponseDto> getAllReportTags(String mode, boolean isAdmin) {
        if (mode != null) {
            if (!EnumValidatorImp.isValid(mode, TeachReportTagMode.class))
                throw new InvalidFieldsException("invalid mode");
        }

        return new ResponseEntity<>(
                ResponseDto
                        .builder(List.class)
                        .data(
                                adviceTagReportRepository.getList(
                                        mode, isAdmin
                                )
                        )
                        .status("ok")
                        .build(),
                HttpStatus.OK
        );
    }

    public List<ReportProblemDigestDto> getAdviceReports(
            Long from, Long to, Boolean showJustUnSeen,
            ObjectId advisorId, ObjectId studentId,
            Boolean justSendFromStudent, Boolean justSendFromTeacher,
            ObjectId adviceId, int pageIndex
    ) {
        List<Bson> filters = new ArrayList<>();

        if (adviceId != null)
            filters.add(eq("schedule_id", adviceId));

        if (showJustUnSeen != null && showJustUnSeen)
            filters.add(eq("seen", false));

        if (justSendFromStudent != null && justSendFromStudent)
            filters.add(eq("send_from", "student"));

        if (justSendFromTeacher != null && justSendFromTeacher)
            filters.add(eq("send_from", "teacher"));

        if (advisorId != null)
            filters.add(eq("advisor_id", advisorId));

        if (studentId != null)
            filters.add(eq("student_id", studentId));

        if (from != null)
            filters.add(gte("created_at", from));

        if (to != null)
            filters.add(lte("created_at", to));

        return adviceReportRepository.getReports(
                filters.isEmpty()
                        ? null
                        : and(filters),
                (pageIndex - 1) * PER_PAGE,
                PER_PAGE
        );
    }

    public void setReportAsSeen(ObjectId id) {
        adviceReportRepository.updateOne(
                id, set("seen", true)
        );
    }

    public void setAdviceScheduleReportProblemsByAdvisor(
            final ObjectId userId, final ObjectId studentId,
            final JSONArray tagIds, final String desc
    ) {
        List<Object> tagOIdsList = getTagIds(tagIds);
        Document myTeachReport = new Document("student_id", studentId)
                .append("send_from", "teacher")
                .append("seen", false)
                .append("advisor_id", userId)
                .append("created_at", System.currentTimeMillis());

        myTeachReport.put("tag_ids", tagOIdsList);
        if (desc != null)
            myTeachReport.put("desc", desc);

        adviceReportRepository.insertOne(myTeachReport);
    }


    public void setAdviceScheduleReportProblemsByStudent(
            final ObjectId userId, final ObjectId advisorId,
            final JSONArray tagIds, final String desc
    ) {
        List<Object> tagOIdsList = getTagIds(tagIds);
        Document myReport = new Document("student_id", userId)
                .append("send_from", "student")
                .append("seen", false)
                .append("advisor_id", advisorId)
                .append("created_at", System.currentTimeMillis());

        myReport.put("tag_ids", tagOIdsList);
        if (desc != null)
            myReport.put("desc", desc);

        adviceReportRepository.insertOne(myReport);
    }

    private List<Object> getTagIds(JSONArray tagIds) {
        List<Object> tagOIdsList = null;

        if (tagIds != null && !tagIds.isEmpty()) {
            Set<ObjectId> tagOIds = new HashSet<>();
            try {
                for (int i = 0; i < tagIds.length(); i++) {
                    if (!ObjectId.isValid(tagIds.getString(i)))
                        throw new InvalidFieldsException("no valid params");
                    tagOIds.add(new ObjectId(tagIds.getString(i)));
                }
            } catch (Exception ex) {
                throw new InvalidFieldsException("no valid params");
            }

            tagOIdsList = new ArrayList<>(tagOIds);
            if (teachTagReportRepository.findByIds(tagOIdsList, false, JUST_ID) == null)
                throw new InvalidFieldsException("no valid params");
        }

        if (tagOIdsList == null)
            tagOIdsList = new ArrayList<>();

        return tagOIdsList;
    }

    public void removeReport(
            final ObjectId userId, final ObjectId id
    ) {
        adviceReportRepository.deleteOne(and(
                eq("_id", id),
                eq("user_id", userId)
        ));
    }
}
