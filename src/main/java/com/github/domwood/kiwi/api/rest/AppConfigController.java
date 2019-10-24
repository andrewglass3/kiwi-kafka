package com.github.domwood.kiwi.api.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.domwood.kiwi.utilities.Constants.API_ENDPOINT;

@CrossOrigin("*")
@RestController
@RequestMapping(API_ENDPOINT)
@ConfigurationProperties("kafka")
public class AppConfigController {

    @Value("${app.version:dev}")
    private String appVersion;

    @Value("${spring.profiles.active}")
    private List<String> activeProfiles;

    private Map<String, List<String>> clusters = new HashMap<>();

    @GetMapping("/version")
    @ResponseBody
    public String version(){
        return this.appVersion;
    }

    @GetMapping("/profiles")
    @ResponseBody
    public List<String> profiles(){
        return activeProfiles;
    }

    @GetMapping("/clusters")
    public Map<String, List<String>> clusters(){
        return this.clusters;
    }

    public Map<String, List<String>> getClusters() {
        return clusters;
    }
}
