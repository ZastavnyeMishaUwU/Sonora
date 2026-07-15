package com.example.it_robota.api;

import com.example.it_robota.models.Track;

import java.util.ArrayList;
import java.util.List;

public class JamendoResponse {

    private JamendoHeader header;
    private List<JamendoTrackDto> results;

    public JamendoResponse() {
    }

    public JamendoResponse(JamendoHeader header, List<JamendoTrackDto> results) {
        this.header = header;
        this.results = results;
    }

    /*
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

    public JamendoHeader getHeader() {
        return header;
    }

    public void setHeader(JamendoHeader header) {
        this.header = header;
    }

    public List<JamendoTrackDto> getResults() {
        return results;
    }

    public void setResults(List<JamendoTrackDto> results) {
        this.results = results;
    }
}