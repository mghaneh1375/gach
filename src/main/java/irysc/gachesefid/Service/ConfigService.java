package irysc.gachesefid.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import static irysc.gachesefid.Utility.StaticValues.*;

@Service
public class ConfigService {
    @Value("${custom.dev_mode}")
    private Boolean devMode;
    @Value("${custom.asanak_username}")
    private String asanakUsername;
    @Value("${custom.asanak_password}")
    private String asanakPassword;
    @Value("${custom.asanak_token}")
    private String asanakToken;
    @Value("${custom.asanak_sender}")
    private String asanakSender;
    @Value("${custom.asanak_template_url}")
    private String asanakTemplateUrl;
    @Value("${custom.asanak_sms_url}")
    private String asanakSmsUrl;
    @Value("${custom.kavenegar.token}")
    private String kavenegarToken;

    @PostConstruct
    public void init() {
        DEV_MODE = devMode;
        ASANAK_USERNAME = asanakUsername;
        ASANAK_SENDER = asanakSender;
        ASANAK_PASSWORD = asanakPassword;
        ASANAK_TOKEN = asanakToken;
        ASANAK_TEMPLATE_URL = asanakTemplateUrl;
        ASANAK_SMS_URL = asanakSmsUrl;
        KAVENEGAR_TOKEN = kavenegarToken;
    }


}
