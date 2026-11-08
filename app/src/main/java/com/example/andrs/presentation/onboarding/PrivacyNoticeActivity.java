package com.example.andrs.presentation.onboarding;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.andrs.App;
import com.example.andrs.R;
import com.example.andrs.databinding.ActivityPrivacyBinding;
import com.example.andrs.di.AppContainer;
import com.example.andrs.presentation.common.SecureWindow;
import com.example.andrs.presentation.main.MainActivity;

/**
 * Aviso de privacidad ineludible: se muestra en el primer inicio y no se puede saltar. Solo se
 * puede aceptar tras desplazarse hasta el final y marcar la casilla; "atrás" cierra la app.
 * La aceptación se guarda en SharedPreferences cifradas.
 */
public class PrivacyNoticeActivity extends AppCompatActivity {
    private static final String EXTRA_READ_ONLY = "read_only";

    private ActivityPrivacyBinding binding;
    private boolean readOnly;
    private boolean reachedEnd;

    public static Intent readOnlyIntent(Context context) {
        return new Intent(context, PrivacyNoticeActivity.class).putExtra(EXTRA_READ_ONLY, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityPrivacyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int pad = getResources().getDimensionPixelSize(R.dimen.screen_padding);
            v.setPadding(bars.left + pad, bars.top + pad, bars.right + pad, bars.bottom + pad);
            return insets;
        });

        readOnly = getIntent().getBooleanExtra(EXTRA_READ_ONLY, false);
        binding.body.setText(Html.fromHtml(getString(R.string.privacy_body), Html.FROM_HTML_MODE_COMPACT));

        if (readOnly) {
            binding.subtitle.setVisibility(View.GONE);
            binding.scrollHint.setVisibility(View.GONE);
            binding.checkbox.setVisibility(View.GONE);
            binding.btnAccept.setVisibility(View.GONE);
            binding.btnExit.setText(R.string.close);
            binding.btnExit.setOnClickListener(v -> finish());
            return;
        }

        binding.scroll.setOnScrollChangeListener(
                (androidx.core.widget.NestedScrollView.OnScrollChangeListener) (v, x, y, ox, oy) -> checkEnd());
        binding.scroll.post(this::checkEnd);
        binding.checkbox.setOnCheckedChangeListener((b, checked) -> updateAccept());
        binding.btnAccept.setOnClickListener(v -> accept());
        binding.btnExit.setOnClickListener(v -> finishAffinity());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        SecureWindow.apply(this);
    }

    private void checkEnd() {
        if (reachedEnd) return;
        View child = binding.scroll.getChildAt(0);
        if (child == null) return;
        int bottom = child.getBottom() - (binding.scroll.getHeight() + binding.scroll.getScrollY());
        if (bottom <= 24) {
            reachedEnd = true;
            binding.scrollHint.setVisibility(View.INVISIBLE);
            binding.checkbox.setEnabled(true);
            updateAccept();
        }
    }

    private void updateAccept() {
        binding.btnAccept.setEnabled(reachedEnd && binding.checkbox.isChecked());
    }

    private void accept() {
        AppContainer c = App.container(this);
        binding.btnAccept.setEnabled(false);
        c.executors.diskIO().execute(() -> {
            c.settings.acceptPrivacyNotice();
            c.reminders.ensureCycleAlerts();
            c.executors.mainThread().execute(() -> {
                App.lockManager(this).unlock();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });
        });
    }
}
