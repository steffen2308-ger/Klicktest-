package de.steffen.klicktest;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

/** Einstellungsseite: Dienst einschalten, Zeiger ein-/ausblenden, Sichtbarkeit und Tempo. */
public class MainActivity extends Activity {

    private TextView status;
    private Button toggle;
    private float density;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, dp(28), pad, pad);

        TextView title = text("Klicktest", 26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);
        root.addView(text("Ein Mauszeiger, der über anderen Apps klicken kann.", 15));

        status = text("", 16);
        status.setTypeface(Typeface.DEFAULT_BOLD);
        status.setPadding(0, dp(18), 0, dp(6));
        root.addView(status);

        Button open = button("Bedienungshilfe einschalten");
        open.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(open);

        toggle = button("Zeiger ausblenden");
        toggle.setOnClickListener(v -> {
            ClickService s = ClickService.instance;
            if (s != null) s.toggleOverlay();
            refresh();
        });
        root.addView(toggle);

        root.addView(section("Sichtbarkeit Mauszeiger"));
        root.addView(slider(Prefs.CURSOR_ALPHA, Prefs.MIN_ALPHA, 100, Prefs.cursorAlpha(this), "%", 1));
        root.addView(section("Sichtbarkeit Touchpad"));
        root.addView(slider(Prefs.PAD_ALPHA, Prefs.MIN_ALPHA, 100, Prefs.padAlpha(this), "%", 1));
        root.addView(section("Zeiger-Geschwindigkeit"));
        root.addView(slider(Prefs.SPEED, 5, 40, Prefs.speedTenths(this), "×", 10));

        root.addView(section("So geht's"));
        root.addView(text(
                "1. Oben „Bedienungshilfe einschalten“ antippen, „Klicktest-Mauszeiger“ wählen und einschalten.\n"
                        + "2. Zeiger und Touchpad erscheinen über allen Apps.\n"
                        + "3. Auf dem Touchpad wischen bewegt den Zeiger, Tippen klickt an der Zeigerspitze, "
                        + "Halten macht einen langen Klick, mit zwei Fingern wischen scrollt.\n"
                        + "4. Die blaue Leiste oben am Touchpad ziehen, um es zu verschieben. ✕ blendet alles aus; "
                        + "wieder einblenden hier oder über die Bedienungshilfe-Taste von Android.", 14));

        root.addView(section("Ausgegraut oder „Eingeschränkte Einstellung“?"));
        root.addView(text(
                "Android 13 und neuer sperrt Bedienungshilfen von Apps, die nicht aus dem Play Store kommen. "
                        + "Dann: unten „App-Info öffnen“, oben rechts ⋮ antippen, „Eingeschränkte Einstellungen zulassen“ "
                        + "wählen und danach die Bedienungshilfe erneut einschalten.", 14));
        Button info = button("App-Info öffnen");
        info.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null))));
        root.addView(info);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(245, 249, 250));
        scroll.addView(root);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        ClickService s = ClickService.instance;
        if (s == null) {
            status.setText("● Bedienungshilfe ist aus");
            status.setTextColor(Color.rgb(180, 60, 40));
            toggle.setEnabled(false);
            toggle.setText("Zeiger einblenden");
        } else {
            status.setText("● Bedienungshilfe ist an");
            status.setTextColor(Color.rgb(20, 130, 90));
            toggle.setEnabled(true);
            toggle.setText(s.isShown() ? "Zeiger ausblenden" : "Zeiger einblenden");
        }
    }

    private View slider(String key, int min, int max, int value, String unit, int divisor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        SeekBar bar = new SeekBar(this);
        bar.setMax(max - min);
        bar.setProgress(value - min);
        TextView label = text(format(value, unit, divisor), 14);
        label.setMinWidth(dp(56));
        label.setGravity(Gravity.END);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int v = progress + min;
                label.setText(format(v, unit, divisor));
                Prefs.putInt(MainActivity.this, key, v);
                ClickService s = ClickService.instance;
                if (s != null) s.applyPrefs();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        row.addView(bar, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(label);
        return row;
    }

    private static String format(int v, String unit, int divisor) {
        if (divisor == 1) return v + " " + unit;
        return String.format(java.util.Locale.GERMANY, "%.1f %s", v / (float) divisor, unit);
    }

    private TextView section(String s) {
        TextView t = text(s, 16);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(22), 0, dp(4));
        return t;
    }

    private TextView text(String s, int sp) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(Color.rgb(20, 40, 50));
        t.setLineSpacing(0, 1.15f);
        return t;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        return b;
    }

    private int dp(int v) {
        return Math.round(v * density);
    }
}
