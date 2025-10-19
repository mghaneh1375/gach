package irysc.gachesefid.Dto.content;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MissedDto {
    private List<MissedChunkDto> missedChunks;
    private List<MissedAttachDto> missedAttaches;
}
