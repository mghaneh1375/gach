package irysc.gachesefid.Service.advice;

import irysc.gachesefid.Dto.ResponseDto;
import irysc.gachesefid.Dto.advice.AdvisorDigestInfoDto;
import irysc.gachesefid.Dto.advice.AdvisorGeneralInfoDto;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

import static irysc.gachesefid.Main.GachesefidApplication.userRepository;

@Service
public class AdvisorService {

    public ResponseEntity<ResponseDto<AdvisorGeneralInfoDto>> getGeneralInfo(ObjectId advisorId) {
        return new ResponseEntity<>(
                ResponseDto
                        .builder(AdvisorGeneralInfoDto.class)
                        .status("ok")
                        .data(userRepository.advisorGeneralInfo(advisorId))
                        .build(),
                HttpStatus.OK
        );
    }

    public ResponseEntity<ResponseDto<List<AdvisorDigestInfoDto>>> getAllAdvisorsDigest() {
        return new ResponseEntity<>(
                ResponseDto
                        .builderList(AdvisorDigestInfoDto.class)
                        .status("ok")
                        .data(userRepository.advisorsDigestInfo())
                        .build(),
                HttpStatus.OK
        );
    }

}
