package com.example.it_robota.api;

public class JamendoHeader {

    private String status;
    private int code;
    private String errorMessage;
    private int resultsCount;

    public JamendoHeader() {
    }

    public JamendoHeader(String status, int code, String errorMessage, int resultsCount) {
        this.status = status;
        this.code = code;
        this.errorMessage = errorMessage;
        this.resultsCount = resultsCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getResultsCount() {
        return resultsCount;
    }

    public void setResultsCount(int resultsCount) {
        this.resultsCount = resultsCount;
    }
}