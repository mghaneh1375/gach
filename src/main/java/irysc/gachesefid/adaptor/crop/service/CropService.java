package irysc.gachesefid.adaptor.crop.service;

import irysc.gachesefid.adaptor.crop.model.CropAdaptorResponse;
import irysc.gachesefid.adaptor.crop.model.CropResponse;
import org.springframework.web.multipart.MultipartFile;


public interface CropService {
    CropAdaptorResponse<CropResponse> crop(MultipartFile file);
}
