package com.example.it_robota.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/**
 * Provides a single application-wide check for active internet connectivity.
 */
public class NetworkStateChecker {

    private final ConnectivityManager connectivityManager;

    /**
     * Creates a checker backed by the system connectivity service.
     *
     * @param context application or activity context
     */
    public NetworkStateChecker(Context context) {
        Context applicationContext = context.getApplicationContext();
        connectivityManager = (ConnectivityManager) applicationContext.getSystemService(
                Context.CONNECTIVITY_SERVICE
        );
    }

    /**
     * Reports whether the device has a validated connection to the internet.
     *
     * @return true when an active network has validated internet access
     */
    public boolean isNetworkAvailable() {
        if (connectivityManager == null) {
            return false;
        }

        Network activeNetwork = connectivityManager.getActiveNetwork();

        if (activeNetwork == null) {
            return false;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);

        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
