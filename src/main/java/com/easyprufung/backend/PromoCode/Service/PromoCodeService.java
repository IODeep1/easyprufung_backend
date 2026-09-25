package com.easyprufung.backend.PromoCode.Service;

import com.easyprufung.backend.PromoCode.DTO.PromoCodeDTO;
import com.easyprufung.backend.PromoCode.PromoCode;
import com.easyprufung.backend.PromoCode.Repository.PromoCodeRepository;
import com.easyprufung.backend.User.Repository.UsersRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@Service
public class PromoCodeService {
    private static final Logger logger = LoggerFactory.getLogger(PromoCodeService.class);

    final String lifetime = "lifetime";
    final String autopilot = "autopilot";
    final String usedStatus = "used";
    final String unusedStatus = "unused";


    @Autowired
    UsersRepository usersRepository;
    @Autowired
    PromoCodeRepository promoCodeRepository;

    //Methods
    public PromoCode createAppSumoPromoCode(String code) {
        ModelMapper modelMapper = new ModelMapper();
        Date date = new Date();
        PromoCodeDTO promoCodeDTO = new PromoCodeDTO();
        promoCodeDTO.setCode(code);
        promoCodeDTO.setPlan(autopilot);
        promoCodeDTO.setType(lifetime);
        promoCodeDTO.setIsActive(false);
        promoCodeDTO.setStatus(unusedStatus);
        promoCodeDTO.setCreatedDate(new Timestamp(date.getTime()));
        promoCodeDTO.setUpdatedDate(new Timestamp(date.getTime()));
        PromoCode promoCode = modelMapper.map(promoCodeDTO, PromoCode.class);
        return promoCodeRepository.save(promoCode);
    }


    public PromoCode activateAppSumoPromoCode(String code) {
        try {
            PromoCode promoCode= promoCodeRepository.findByCode(code);
            if(promoCode != null) {
                if(promoCode.getIsActive()){
                    return null;
                }
                Date date = new Date();
                promoCode.setIsActive(true);
                promoCode.setStatus(usedStatus);
                promoCode.setUpdatedDate(new Timestamp(date.getTime()));
                return promoCodeRepository.save(promoCode);
            }
        }
        catch (Exception ex){
            logger.error(ex.getMessage());
        }
        return null;
    }

    public List<PromoCode> findAll() {
        return promoCodeRepository.findAll();
    }
}
