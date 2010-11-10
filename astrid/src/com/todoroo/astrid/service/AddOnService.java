package com.todoroo.astrid.service;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.widget.Button;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.data.AddOn;
import com.todoroo.astrid.utility.Constants;

/**
 * Astrid Service for managing add-ons
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class AddOnService {

    /** OEM preference key */
    private static final String PREF_OEM = "poem";

    /** Astrid Power Pack package */
    public static final String POWER_PACK_PACKAGE = "com.todoroo.astrid.ppack";

    /** Astrid Locale package */
    public static final String LOCALE_PACKAGE = "com.todoroo.astrid.locale";

    /** Astrid Power Pack label */
    public static final String POWER_PACK_LABEL = "Astrid Power Pack";

    /** Checks whether power pack should be enabled */
    public boolean hasPowerPack() {
        if (Preferences.getBoolean(PREF_OEM, false))
            return true;
        else if(isInstalled(POWER_PACK_PACKAGE, true))
            return true;
        return false;
    }

    /** Checks whether locale plugin should be enabled */
    public boolean hasLocalePlugin() {
        if (Preferences.getBoolean(PREF_OEM, false))
            return true;
        else if(isInstalled(LOCALE_PACKAGE, true))
            return true;
        return false;
    }

    /**
     * Takes users to the market
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public static class MarketClickListener implements DialogInterface.OnClickListener {
        private final Context context;
        private final String packageName;

        public MarketClickListener(Context activity, String packageName) {
            this.context = activity;
            this.packageName = packageName;
        }

        @Override
        public void onClick(DialogInterface arg0, int arg1) {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://search?q=pname:" + //$NON-NLS-1$
                            packageName)));
            if(context instanceof Activity)
                ((Activity)context).finish();
        }
    };

    public static void checkForUpgrades(final Activity activity) {
        if(DateUtilities.now() > Constants.UPGRADE.getTime()) {
            final AtomicInteger countdown = new AtomicInteger(10);
            final AlertDialog dialog = new AlertDialog.Builder(activity)
            .setTitle(R.string.DLG_information_title)
            .setMessage(R.string.DLG_please_update)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(R.string.DLG_to_market,
                    new MarketClickListener(activity, activity.getPackageName()))
            .setNegativeButton(countdown.toString(), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface myDialog, int which) {
                    // do nothing!
                }
            })
            .setCancelable(false)
            .show();
            dialog.setOwnerActivity(activity);
            dialog.getButton(Dialog.BUTTON_NEGATIVE).setEnabled(false);

            final Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    final int number = countdown.addAndGet(-1);

                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            Button negativeButton =
                                dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                            if(negativeButton == null)
                                return;
                            if(number == 0)
                                timer.cancel();

                            if(number == 0) {
                                dialog.setCancelable(true);
                                negativeButton.setText(
                                        android.R.string.ok);
                                negativeButton.setEnabled(true);
                            } else {
                                negativeButton.setEnabled(false);
                                negativeButton.setText(Integer.toString(number));
                            }
                        }
                    });
                }
            }, 0L, 1000L);
        }
    }

    /**
     * Record that a version was an OEM install
     */
    public static void recordOem() {
        Preferences.setBoolean(PREF_OEM, true);
    }

    /**
     * Check whether a given add-on is installed
     * @param addOn
     * @return
     */
    public boolean isInstalled(AddOn addOn) {
        // it isnt installed if it is null...
        if (addOn == null)
            return false;
        return isInstalled(addOn.getPackageName(), addOn.isInternal());
    }

    /**
     * Check whether an external add-on is installed
     * @param packageName
     * @return
     */
    public boolean isInstalled(String packageName) {
        return isInstalled(packageName, false);
    }

    /**
     * Check whether a given add-on is installed
     * @param addOn
     * @param internal whether to do api sig check
     * @return
     */
    private boolean isInstalled(String packageName, boolean internal) {
        if(Constants.PACKAGE.equals(packageName))
            return true;

        Context context = ContextManager.getContext();

        String packageSignature = AndroidUtilities.getSignature(context, packageName);
        if(packageSignature == null)
            return false;
        if(!internal)
            return true;

        String astridSignature = AndroidUtilities.getSignature(context, Constants.PACKAGE);
        return packageSignature.equals(astridSignature);
    }

    /**
     * Get one AddOn-descriptor by packageName and title.
     *
     * @param packageName could be Constants.PACKAGE or one of AddOnService-constants
     * @param title the descriptive title, as in "Producteev" or "Astrid Power Pack"
     * @return the addon-descriptor, if it is available (registered here in getAddOns), otherwise null
     */
    public AddOn getAddOn(String packageName, String title) {
        if (title == null || packageName == null)
            return null;

        AddOn addon = null;
        AddOn[] addons = getAddOns();
        for (int i = 0; i < addons.length ; i++) {
            if (packageName.equals(addons[i].getPackageName()) && title.equals(addons[i].getTitle())) {
                addon = addons[i];
            }
        }
        return addon;
    }

    /**
     * Get a list of add-ons
     *
     * @return available add-ons
     */
    public AddOn[] getAddOns() {
        Resources r = ContextManager.getContext().getResources();

        // temporary temporary
        AddOn[] list = new AddOn[3];
        list[0] = new AddOn(false, true, "Astrid Power Pack", null,
                "Support Astrid and get more productive with the Astrid Power Pack. Backup, widgets, no ads, and calendar integration. Power up today!",
                POWER_PACK_PACKAGE, "http://www.weloveastrid.com/store",
                ((BitmapDrawable)r.getDrawable(R.drawable.icon_pp)).getBitmap());

        list[1] = new AddOn(false, true, "Astrid Locale Plugin", null,
                "Allows Astrid to make use of the Locale application to send you notifications based on filter conditions. Requires Locale.",
                LOCALE_PACKAGE, "http://www.weloveastrid.com/store",
                ((BitmapDrawable)r.getDrawable(R.drawable.icon_locale)).getBitmap());

        list[2] = new AddOn(true, true, "Producteev", null,
                "Synchronize with Producteev service. Also changes Astrid's importance levels to stars.",
                Constants.PACKAGE, "http://www.producteev.com",
                ((BitmapDrawable)r.getDrawable(R.drawable.icon_producteev)).getBitmap());

        return list;
    }

}
