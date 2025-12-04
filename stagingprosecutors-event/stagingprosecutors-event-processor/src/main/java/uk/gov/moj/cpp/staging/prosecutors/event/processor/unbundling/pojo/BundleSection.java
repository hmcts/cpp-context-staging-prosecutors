package uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pojo;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "seqNum",
        "bundleSectionCode",
        "bundleSectionName",
        "splitBundleSubSection",
        "targetSectionCode",
        "validFrom",
        "validTo"
})
public class BundleSection {

    @JsonProperty("seqNum")
    private Integer seqNum;
    @JsonProperty("bundleSectionCode")
    private String bundleSectionCode;
    @JsonProperty("bundleSectionName")
    private String bundleSectionName;
    @JsonProperty("splitBundleSubSection")
    private Boolean splitBundleSubSection;
    @JsonProperty("targetSectionCode")
    private String targetSectionCode;
    @JsonProperty("validFrom")
    private String validFrom;
    @JsonProperty("validTo")
    private String validTo;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    public BundleSection() {
        //do nothing
    }

    @JsonCreator
    public BundleSection(final Integer seqNum, final String bundleSectionCode, final String bundleSectionName, final Boolean splitBundleSubSection, final String targetSectionCode, final String validFrom, final String validTo) {
        this.seqNum = seqNum;
        this.bundleSectionCode = bundleSectionCode;
        this.bundleSectionName = bundleSectionName;
        this.splitBundleSubSection = splitBundleSubSection;
        this.targetSectionCode = targetSectionCode;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    @JsonProperty("seqNum")
    public Integer getSeqNum() {
        return seqNum;
    }

    @JsonProperty("seqNum")
    public void setSeqNum(Integer seqNum) {
        this.seqNum = seqNum;
    }

    @JsonProperty("bundleSectionCode")
    public String getBundleSectionCode() {
        return bundleSectionCode;
    }

    @JsonProperty("bundleSectionCode")
    public void setBundleSectionCode(String bundleSectionCode) {
        this.bundleSectionCode = bundleSectionCode;
    }

    @JsonProperty("bundleSectionName")
    public String getBundleSectionName() {
        return bundleSectionName;
    }

    @JsonProperty("bundleSectionName")
    public void setBundleSectionName(String bundleSectionName) {
        this.bundleSectionName = bundleSectionName;
    }

    @JsonProperty("splitBundleSubSection")
    public Boolean getSplitBundleSubSection() {
        return splitBundleSubSection;
    }

    @JsonProperty("splitBundleSubSection")
    public void setSplitBundleSubSection(Boolean splitBundleSubSection) {
        this.splitBundleSubSection = splitBundleSubSection;
    }

    @JsonProperty("targetSectionCode")
    public String getTargetSectionCode() {
        return targetSectionCode;
    }

    @JsonProperty("targetSectionCode")
    public void setTargetSectionCode(String targetSectionCode) {
        this.targetSectionCode = targetSectionCode;
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

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public static Builder bundleSection() {
        return new BundleSection.Builder();
    }

    public static class Builder {
        private Integer seqNum;
        private String bundleSectionCode;
        private String bundleSectionName;
        private Boolean splitBundleSubSection;
        private String targetSectionCode;
        private String validFrom;
        private String validTo;

        public Builder withSeqNum(final Integer seqNum) {
            this.seqNum = seqNum;
            return this;
        }

        public Builder withBbundleSectionCode(final String bundleSectionCode) {
            this.bundleSectionCode = bundleSectionCode;
            return this;
        }

        public Builder withBundleSectionName(final String bundleSectionName) {
            this.bundleSectionName = bundleSectionName;
            return this;
        }

        public Builder withSplitBundleSubSection(final Boolean splitBundleSubSection) {
            this.splitBundleSubSection = splitBundleSubSection;
            return this;
        }

        public Builder withTargetSectionCode(final String targetSectionCode) {
            this.targetSectionCode = targetSectionCode;
            return this;
        }

        public Builder withvValidFrom(final String validFrom) {
            this.validFrom = validFrom;
            return this;
        }

        public Builder withvValidTo(final String validTo) {
            this.validTo = validTo;
            return this;
        }

        public BundleSection build() {
            return new BundleSection(seqNum, bundleSectionCode, bundleSectionName, splitBundleSubSection,
                    targetSectionCode, validFrom, validTo);
        }

    }
}
