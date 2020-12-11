package com.soundvision.demo.dfu.hawkbit.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TargetDeployment {

    public class Hashes{
        public String sha1;
        public String md5;
        public String sha256;
    }

    public class DownloadHttp{
        public String href;
    }

    public class Md5sumHttp{
        public String href;
    }

    public class Links{
        @SerializedName("download-http")
        public DownloadHttp downloadHttp;
        @SerializedName("md5sum-http")
        public Md5sumHttp md5sumHttp;
    }

    public class Artifact{
        public String filename;
        public Hashes hashes;
        public int size;
        public Links _links;
    }

    public class Chunk{
        public String part;
        public String version;
        public String name;
        public List<Artifact> artifacts;
    }

    public class Deployment{
        public String download;
        public String update;
        public List<Chunk> chunks;
    }

   public String id;
   public Deployment deployment;

}
