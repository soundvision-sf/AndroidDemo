package com.soundvision.demo.dfu.hawkbit.model;

public class TargetConfig {

    public class Polling{
        public String sleep;
    }

    public class Config{
        public Polling polling;
    }

    public class DeploymentBase{
        public String href;
    }

    public class ConfigData{
        public String href;
    }

    public class Links{
        public DeploymentBase deploymentBase;
        public ConfigData configData;
    }

    public Config config;
    public Links _links;
}
