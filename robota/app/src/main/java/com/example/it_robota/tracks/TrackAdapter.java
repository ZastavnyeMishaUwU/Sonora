package com.example.it_robota.tracks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.it_robota.R;
import com.example.it_robota.models.Track;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays a simple list of tracks with track and artist names.
 */
public class TrackAdapter
        extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {

    /**
     * Receives track selection events from the list.
     */
    public interface OnTrackClickListener {

        /**
         * Called when a track item is selected.
         *
         * @param track selected track
         */
        void onTrackClick(Track track);
    }

    private final List<Track> tracks = new ArrayList<>();
    private final OnTrackClickListener onTrackClickListener;

    /**
     * Creates a track adapter.
     *
     * @param tracks initial list of tracks
     * @param onTrackClickListener listener for track selection
     */
    public TrackAdapter(
            List<Track> tracks,
            OnTrackClickListener onTrackClickListener
    ) {
        if (tracks != null) {
            this.tracks.addAll(tracks);
        }

        this.onTrackClickListener = onTrackClickListener;
    }

    /**
     * Creates a view holder for a track row.
     *
     * @param parent parent view group
     * @param viewType item view type
     * @return created track view holder
     */
    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(
                        R.layout.item_track,
                        parent,
                        false
                );

        return new TrackViewHolder(view);
    }

    /**
     * Binds track data to a list row.
     *
     * @param holder track view holder
     * @param position item position
     */
    @Override
    public void onBindViewHolder(
            @NonNull TrackViewHolder holder,
            int position
    ) {
        holder.bind(tracks.get(position));
    }

    /**
     * Returns the number of tracks displayed by the adapter.
     *
     * @return number of tracks
     */
    @Override
    public int getItemCount() {
        return tracks.size();
    }

    /**
     * Replaces the currently displayed tracks.
     *
     * @param newTracks new track list
     */
    public void setTracks(List<Track> newTracks) {
        tracks.clear();

        if (newTracks != null) {
            tracks.addAll(newTracks);
        }

        notifyDataSetChanged();
    }

    /**
     * Holds and binds the views of one track item.
     */
    class TrackViewHolder extends RecyclerView.ViewHolder {

        private final TextView trackNameTextView;
        private final TextView artistNameTextView;

        /**
         * Creates a track view holder.
         *
         * @param itemView track item root view
         */
        TrackViewHolder(@NonNull View itemView) {
            super(itemView);

            trackNameTextView =
                    itemView.findViewById(R.id.tvTrackName);

            artistNameTextView =
                    itemView.findViewById(R.id.tvArtistName);
        }

        /**
         * Displays track information and configures item selection.
         *
         * @param track track displayed by this row
         */
        void bind(Track track) {
            trackNameTextView.setText(
                    valueOrFallback(
                            track.getName(),
                            itemView.getContext().getString(
                                    R.string.search_unknown_track
                            )
                    )
            );

            artistNameTextView.setText(
                    valueOrFallback(
                            track.getArtistName(),
                            itemView.getContext().getString(
                                    R.string.search_unknown_artist
                            )
                    )
            );

            itemView.setOnClickListener(view -> {
                if (onTrackClickListener != null) {
                    onTrackClickListener.onTrackClick(track);
                }
            });
        }

        /**
         * Returns a safe text value for optional track fields.
         *
         * @param value original value
         * @param fallback fallback value
         * @return original value or fallback
         */
        private String valueOrFallback(
                String value,
                String fallback
        ) {
            if (value == null || value.trim().isEmpty()) {
                return fallback;
            }

            return value;
        }
    }
}