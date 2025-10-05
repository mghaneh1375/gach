package irysc.gachesefid.adaptor.crop.service;

import irysc.gachesefid.adaptor.crop.CropRestTemplate;
import irysc.gachesefid.adaptor.crop.model.CropAdaptorResponse;
import irysc.gachesefid.adaptor.crop.model.CropResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

@Component
public class CropServiceImpl implements CropService {

    @Autowired
    private CropRestTemplate restTemplate;

    @Override
    public CropAdaptorResponse<CropResponse> crop(MultipartFile file) {
        MultiValueMap<String, Object> body
                = new LinkedMultiValueMap<>();
        body.add("file", file);

        return restTemplate.postForEntity(
                "cropPDF", body, CropAdaptorResponse.class);
    }
}
