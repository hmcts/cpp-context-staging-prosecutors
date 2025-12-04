package uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PDFBundleDetails {

    @JsonProperty("id")
    private String id;
    @JsonProperty("seqNum")
    private Integer seqNum;
    @JsonProperty("cpsBundleCode")
    private String cpsBundleCode;
    @JsonProperty("parentBundleCode")
    private String parentBundleCode;
    @JsonProperty("parentBundleDescription")
    private String parentBundleDescription;
    @JsonProperty("targetSectionCode")
    private String targetSectionCode;
    @JsonProperty("unbundleFlag")
    private Boolean unbundleFlag;
    @JsonProperty("bundleAcceptanceFlag")
    private Boolean bundleAcceptanceFlag;
    @JsonProperty("validFrom")
    private String validFrom;
    @JsonProperty("validTo")
    private String validTo;
    @JsonProperty("bundleSections")
    private List<BundleSection> bundleSections = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("seqNum")
    public Integer getSeqNum() {
        return seqNum;
    }

    @JsonProperty("seqNum")
    public void setSeqNum(Integer seqNum) {
        this.seqNum = seqNum;
    }

    @JsonProperty("cpsBundleCode")
    public String getCpsBundleCode() {
        return cpsBundleCode;
    }

    @JsonProperty("cpsBundleCode")
    public void setCpsBundleCode(String cpsBundleCode) {
        this.cpsBundleCode = cpsBundleCode;
    }

    @JsonProperty("parentBundleCode")
    public String getParentBundleCode() {
        return parentBundleCode;
    }

    @JsonProperty("parentBundleCode")
    public void setParentBundleCode(String parentBundleCode) {
        this.parentBundleCode = parentBundleCode;
    }

    @JsonProperty("parentBundleDescription")
    public String getParentBundleDescription() {
        return parentBundleDescription;
    }

    @JsonProperty("parentBundleDescription")
    public void setParentBundleDescription(String parentBundleDescription) {
        this.parentBundleDescription = parentBundleDescription;
    }

    @JsonProperty("targetSectionCode")
    public String getTargetSectionCode() {
        return targetSectionCode;
    }

    @JsonProperty("targetSectionCode")
    public void setTargetSectionCode(String targetSectionCode) {
        this.targetSectionCode = targetSectionCode;
    }

    @JsonProperty("unbundleFlag")
    public Boolean getUnbundleFlag() {
        return unbundleFlag;
    }

    @JsonProperty("unbundleFlag")
    public void setUnbundleFlag(Boolean unbundleFlag) {
        this.unbundleFlag = unbundleFlag;
    }

    @JsonProperty("bundleAcceptanceFlag")
    public Boolean getBundleAcceptanceFlag() {
        return bundleAcceptanceFlag;
    }

    @JsonProperty("bundleAcceptanceFlag")
    public void setBundleAcceptanceFlag(Boolean bundleAcceptanceFlag) {
        this.bundleAcceptanceFlag = bundleAcceptanceFlag;
    }

    @JsonProperty("validFrom")
    public String getValidFrom() {
        return validFrom;
    }

    @JsonProperty("validFrom")
    public void setValidFrom(String validFrom) {
        this.validFrom = validFrom;
    }

    @JsonProperty("validTo")
    public String getValidTo() {
        return validTo;
    }

    @JsonProperty("validTo")
    public void setValidTo(String validTo) {
        this.validTo = validTo;
    }

    @JsonProperty("bundleSections")
    public List<BundleSection> getBundleSections() {
        return new ArrayList<>(bundleSections);
    }

    @JsonProperty("bundleSections")
    public void setBundleSections(List<BundleSection> bundleSections) {
        this.bundleSections = new ArrayList<>(bundleSections);
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}


