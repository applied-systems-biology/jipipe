package org.hkijena.jipipe.api.servers;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SharedInstanceRegistration {
    private String instanceId;
    private long jipipePid;

    public SharedInstanceRegistration() {
    }

    public SharedInstanceRegistration(SharedInstanceRegistration other) {
        this.instanceId = other.instanceId;
        this.jipipePid = other.jipipePid;
    }

    @JsonGetter("instance-id")
    public String getInstanceId() {
        return instanceId;
    }

    @JsonSetter("instance-id")
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    @JsonGetter("jipipe-pid")
    public long getJipipePid() {
        return jipipePid;
    }

    @JsonSetter("jipipe-pid")
    public void setJipipePid(long jipipePid) {
        this.jipipePid = jipipePid;
    }
}
