package com.example.it_robota.api;

/**
 * Model class representing the header section of a Jamendo API response.
 * It contains response metadata, status codes, and error details.
 */
public class JamendoHeader {

    private String status;
    private int code;
    private String errorMessage;
    private int resultsCount;

    /**
     * Default empty constructor required for serialization/deserialization
     */
    public JamendoHeader() {
    }

    /**
     * Constructor with all fields to easily create header instances
     */
    public JamendoHeader(String status, int code, String errorMessage, int resultsCount) {
        this.status = status;
        this.code = code;
        this.errorMessage = errorMessage;
        this.resultsCount = resultsCount;
    }

    /**
     * Gets the response status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the response status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the response code
     */
    public int getCode() {
        return code;
    }

    /**
     * Sets the response code
     */
    public void setCode(int code) {
        this.code = code;
    }

    /**
     * Gets the error message
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Gets the results count
     */
    public int getResultsCount() {
        return resultsCount;
    }

    /**
     * Sets the results count
     */
    public void setResultsCount(int resultsCount) {
        this.resultsCount = resultsCount;
    }
}