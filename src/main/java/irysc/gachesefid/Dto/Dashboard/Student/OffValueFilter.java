package irysc.gachesefid.Dto.Dashboard.Student;

public class OffValueFilter {
    @Override
    public boolean equals(Object obj) {
        if(obj == null) return true;
        SuggestedContentDto.Off off = (SuggestedContentDto.Off) obj;
        if(off.getStart() == null) return true;
        long curr = System.currentTimeMillis();
        return off.getStart() > curr || off.getExpiration() < curr;
    }
}
