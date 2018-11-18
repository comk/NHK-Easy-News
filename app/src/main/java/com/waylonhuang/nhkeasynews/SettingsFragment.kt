package com.waylonhuang.nhkeasynews


import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.CheckBoxPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener
import java.util.*


class SettingsFragment : PreferenceFragmentCompat(), RewardedVideoAdListener {

    private var mAd: RewardedVideoAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {}

    private fun loadRewardedVideoAd() {
        mAd!!.loadAd(AD_UNIT_ID, AdRequest.Builder().build())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val preferences = activity.getSharedPreferences(PREFS_FILE, 0)

        (activity as AppCompatActivity).supportActionBar!!.title = "Settings"

        mAd = MobileAds.getRewardedVideoAdInstance(activity)
        mAd!!.rewardedVideoAdListener = this
        loadRewardedVideoAd()

        val appPackageName = activity.packageName

        var fontDefault = preferences.getInt("font", 18)
        val fontPref = findPreference("font") as Preference
        fontPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            showDialog(1,
                    56,
                    fontDefault - 16,
                    "Select Font Size",
                    "$fontDefault",
                    { progress: Int ->
                        fontDefault = progress + 16

                        // Return the font value.
                        "$fontDefault"
                    },
                    { d: DialogInterface, i: Int ->
                        val editor = preferences.edit()
                        editor.putInt("font", fontDefault)
                        editor.apply()
                    })
            true
        }

        var playbackDefault = preferences.getFloat("playback", 1.0f)
        val playbackPref = findPreference("playback") as Preference
        playbackPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            showDialog(1,
                    19,
                    (playbackDefault * 10 - 1).toInt(),
                    "Select Audio Playback Speed",
                    "${playbackDefault}x",
                    { progress: Int ->
                        playbackDefault = (progress + 1) / 10.0f

                        // Return the font value.
                        "${playbackDefault}x"
                    },
                    { d: DialogInterface, i: Int ->
                        val editor = preferences.edit()
                        editor.putFloat("playback", playbackDefault)
                        editor.apply()
                    })
            true
        }

        val furiganaPref = findPreference("furigana") as CheckBoxPreference
        furiganaPref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            if (newValue is Boolean) {
                val editor = preferences.edit()
                editor.putBoolean("furigana", newValue)
                editor.apply()
            }
            true
        }
        furiganaPref.isChecked = preferences.getBoolean("furigana", true)

        val videoPref = findPreference("video") as Preference
        videoPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (mAd!!.isLoaded) {
                mAd!!.show()
            }
            true
        }

        val playStorePref = findPreference("rate") as Preference
        playStorePref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)))
            true
        }

        val resetPref = findPreference("reset") as Preference
        resetPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            clearPreferences()
            true
        }

        val mailPref = findPreference("contact") as Preference
        mailPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("nhkeasyapp@gmail.com"))
            intent.putExtra(Intent.EXTRA_SUBJECT, "NHK Easy News Feedback")
            intent.putExtra(Intent.EXTRA_TEXT, "Feedback:\n")

            startActivity(Intent.createChooser(intent, "Send Email"))
            true
        }

        val buildPref = findPreference("build") as Preference
        val buildDate = Date(BuildConfig.TIMESTAMP)
        buildPref.summary = buildDate.toString()

        val sourcePref = findPreference("source") as Preference
        val packageManager = context.packageManager
        var sourceStr: String? = packageManager.getInstallerPackageName(appPackageName)
        if (sourceStr == null) {
            sourceStr = "Side Loaded"
        } else {
            sourceStr = "Google Play"
        }
        sourcePref.summary = sourceStr

        val versionPref = findPreference("version") as Preference
        versionPref.summary = BuildConfig.VERSION_NAME

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun clearPreferences() {
        val builder = AlertDialog.Builder(activity)

        builder.setMessage("Clear all preferences set by the user")
                .setTitle("Reset preferences")
        builder.setPositiveButton("Ok") { dialog, id ->
            val settings = activity.getSharedPreferences(PREFS_FILE, 0)
            val editor = settings.edit()
            editor.putFloat("playback", 1.0f)
            editor.putInt("font", 18)
            editor.putBoolean("furigana", true)
            editor.apply()

            Toast.makeText(activity, "Preferences reset", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("Cancel") { dialog, id ->
            // User cancelled the dialog
        }

        // Create the AlertDialog
        val dialog = builder.create()
        dialog.show()
    }

    // Required to reward the user.
    override fun onRewarded(reward: RewardItem) {
        Toast.makeText(activity, "Video complete! currency: " + reward.type + "  amount: " + reward.amount, Toast.LENGTH_SHORT).show()
        // Reward the user.
    }

    override fun onRewardedVideoAdClosed() {
        Log.wtf(TAG, "onRewardedVideoAdClosed")
    }

    override fun onRewardedVideoAdLoaded() {
        Log.wtf(TAG, "onRewardedVideoAdLoaded")
    }

    override fun onRewardedVideoAdFailedToLoad(i: Int) {
        Log.wtf(TAG, "onRewardedVideoAdFailedToLoad")
    }

    override fun onRewardedVideoAdLeftApplication() {
        Log.wtf(TAG, "onRewardedVideoAdLeftApplication")
    }

    override fun onRewardedVideoAdOpened() {
        Log.wtf(TAG, "onRewardedVideoAdOpened")
    }

    override fun onRewardedVideoStarted() {
        Log.wtf(TAG, "onRewardedVideoStarted")
    }

    override fun onResume() {
        mAd!!.resume(activity)
        super.onResume()
    }

    override fun onPause() {
        mAd!!.pause(activity)
        super.onPause()
    }

    override fun onDestroy() {
        mAd!!.destroy(activity)
        super.onDestroy()
    }

    private fun showDialog(incrementVal: Int,
                           maxVal: Int,
                           progressVal: Int,
                           titleStr: String,
                           initVal: String,
                           routine: (Int) -> String,
                           positiveRoutine: (DialogInterface, Int) -> Unit) {
        val builder = AlertDialog.Builder(activity)

        val dialogView = layoutInflater.inflate(R.layout.alert_dialog_fragment_detail, null);
        builder.setView(dialogView);

        val dialogTextView = dialogView.findViewById<TextView>(R.id.alert_dialog_tv)
        dialogTextView.text = initVal

        val seekBar = dialogView.findViewById<SeekBar>(R.id.alert_dialog_sb)
        seekBar.keyProgressIncrement = incrementVal
        seekBar.max = maxVal
        seekBar.progress = progressVal
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar1: SeekBar?, progress: Int, fromUser: Boolean) {
                val result = routine(progress)

                // Set the text of the text view associated with the seek bar.
                dialogTextView.text = result
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })

        builder.setTitle(titleStr)
        builder.setPositiveButton("Ok", { dialogInterface, someInt -> positiveRoutine(dialogInterface, someInt) })
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    companion object {
        public val PREFS_FILE = "PREFS_FILE"

        private val TAG = SettingsFragment::class.java.simpleName

        val APP_ID = "ca-app-pub-1189993122998448~5476081412"
        val AD_UNIT_ID = "ca-app-pub-1189993122998448/8699036115"

        fun newInstance(): SettingsFragment {
            val fragment = SettingsFragment()
            val args = Bundle()
            if (fragment.arguments == null) {
                fragment.arguments = args
            }
            return fragment
        }
    }
}
