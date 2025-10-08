package irysc.gachesefid.adaptor.crop.service;

import irysc.gachesefid.adaptor.crop.CropRestTemplate;
import irysc.gachesefid.adaptor.crop.model.CropAdaptorResponse;
import irysc.gachesefid.adaptor.crop.model.CropResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
public class CropServiceImpl implements CropService {

    @Autowired
    private CropRestTemplate restTemplate;

    @Override
    public CropAdaptorResponse<CropResponse> crop(MultipartFile file) throws IOException {
        ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };
        MultiValueMap<String, Object> body
                = new LinkedMultiValueMap<>();
        body.add("file", resource);

        return restTemplate.postForEntity(
                "cropPDF", body,
                new ParameterizedTypeReference<>() {}
        );
    }
}
