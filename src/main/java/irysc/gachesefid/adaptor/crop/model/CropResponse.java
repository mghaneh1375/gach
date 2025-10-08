package irysc.gachesefid.adaptor.crop.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CropResponse {
    private List<String> filenames;
}
