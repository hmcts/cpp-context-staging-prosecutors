package uk.gov.moj.cpp.staging.prosecutors.event.processor.application;

public class CaseDefendantOrganisation {
    private final String asn;

    private final String name;

    private final String prosecutorDefendantId;

    public CaseDefendantOrganisation(final String asn, final String name, final String prosecutorDefendantId) {
        this.asn = asn;
        this.name = name;
        this.prosecutorDefendantId = prosecutorDefendantId;
    }

    public String getAsn() {
        return asn;
    }

    public String getName() {
        return name;
    }

    public String getProsecutorDefendantId() {
        return prosecutorDefendantId;
    }

    public static Builder caseDefendantOrganisation() {
        return new Builder();
    }


    public static class Builder {
        private String asn;

        private String name;

        private String prosecutorDefendantId;

        public Builder withAsn(final String asn) {
            this.asn = asn;
            return this;
        }

        public Builder withName(final String name) {

                this.name = name;
                        return this;

        }

        public Builder withProsecutorDefendantId(final String prosecutorDefendantId) {
            this.prosecutorDefendantId = prosecutorDefendantId;
            return this;
        }



        public Builder withValuesFrom(final CaseDefendantOrganisation caseDefendantOrganisation) {
            this.asn = caseDefendantOrganisation.getAsn();
            this.name = caseDefendantOrganisation.getName();
            this.prosecutorDefendantId = caseDefendantOrganisation.getProsecutorDefendantId();
            return this;
        }

        public CaseDefendantOrganisation build() {
            return new CaseDefendantOrganisation(asn, name, prosecutorDefendantId);
        }
    }
}
