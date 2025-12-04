package uk.gov.moj.cpp.staging.prosecutors.common;

@SuppressWarnings("squid:S00115")
public enum Title {
    Lord("Lord"),
    Rev("Rev"),
    Mr("Mr"),
    Dame("Dame"),
    Ms("Ms"),
    Sir("Sir"),
    Dr("Dr"),
    Lady("Lady"),
    Mrs("Mrs");

    private final String value;

    Title(String value) {
        this.value = value;
    }
}
