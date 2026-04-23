package com.etread.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
@Data
@Component
@ConfigurationProperties(prefix = "etread.gateway.blacklist")
public class GatewayBlacklistProperties {

    private boolean enabled = true;
    private List<String> ipList = new ArrayList<>();
    private List<String> tokenList = new ArrayList<>();
    private List<String> accountList = new ArrayList<>();


}
