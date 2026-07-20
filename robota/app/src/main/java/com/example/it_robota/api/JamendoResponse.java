package com.example.it_robota.api;

import com.example.it_robota.models.Track;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class representing the full response structure from the Jamendo API.
 * Contains the response header and the list of track DTOs returned by the request.
 */
public class JamendoResponse {

    private JamendoHeader header;
    private List<JamendoTrackDto> results;

    /**
     * Default empty constructor required for serialization/deserialization
     */
    public JamendoResponse() {
    }

    /**
     * Constructor with all fields to easily create response instances
     *
     * @param header the response header metadata
     * @param results the list of track DTOs
     */
    public JamendoResponse(JamendoHeader header, List<JamendoTrackDto> results) {
        this.header = header;
        this.results = results;
    }

    /**
     * Converts API DTO models into the main domain Track models.
     * Maps each JamendoTrackDto inside results into a clean, local Track object.
     */
    public List<Track> toTrackList() {
        List<Track> tracks = new ArrayList<>();

        if (results == null) {
            return tracks;
        }

        for (JamendoTrackDto trackDto : results) {
            if (trackDto != null) {
                tracks.add(trackDto.toTrack());
            }
        }

        return tracks;
    }

    /**
     * Gets the response header
     */
    public JamendoHeader getHeader() {
        return header;
    }

    /**
     * Sets the response header
     */
    public void setHeader(JamendoHeader header) {
        this.header = header;
    }

    /**
     * Gets the list of track DTOs
     */
    public List<JamendoTrackDto> getResults() {
        return results;
    }

    /**
     * Sets the list of track DTOs
     */
    public void setResults(List<JamendoTrackDto> results) {
        this.results = results;
    }
}