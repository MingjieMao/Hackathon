package com.example.myapplication;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class LoginActivity extends AppCompatActivity {
    private EditText inputUsername;
    private EditText inputPassword;
    private MaterialButton buttonLoginMember;
    private MaterialButton buttonLoginAdmin;
    private boolean isAdminSelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        UiPreferences.applyAppearance(this);
        super.onCreate(savedInstanceState);

        if (UiPreferences.isLoggedIn(this)) {
            openMain();
            return;
        }

        EdgeToEdge.enable(this);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.page_background)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }

        setContentView(R.layout.activity_login);

        View root = findViewById(R.id.loginRoot);
        inputUsername = findViewById(R.id.inputLoginUsername);
        inputPassword = findViewById(R.id.inputLoginPassword);
        buttonLoginMember = findViewById(R.id.buttonLoginMember);
        buttonLoginAdmin = findViewById(R.id.buttonLoginAdmin);
        MaterialButton buttonLogin = findViewById(R.id.buttonLogin);

        applyInsets(root);
        updateRoleButtons();
        buttonLoginMember.setOnClickListener(v -> { isAdminSelected = false; updateRoleButtons(); });
        buttonLoginAdmin.setOnClickListener(v -> { isAdminSelected = true; updateRoleButtons(); });
        buttonLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String username = inputUsername.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        if (!"1234".equals(username) || !"1234".equals(password)) {
            Toast.makeText(this, R.string.toast_login_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        UiPreferences.setLoginSession(this, isAdminSelected);
        AppData.ensurePopulated();
        AppData.setAdminMode(isAdminSelected);
        openMain();
    }

    private void updateRoleButtons() {
        int selectedBg = ContextCompat.getColor(this, R.color.tab_bar_fill);
        int unselectedBg = ContextCompat.getColor(this, R.color.surface);
        int selectedText = ContextCompat.getColor(this, R.color.ink_primary);
        int unselectedText = ContextCompat.getColor(this, R.color.ink_secondary);

        buttonLoginMember.setBackgroundTintList(ColorStateList.valueOf(isAdminSelected ? unselectedBg : selectedBg));
        buttonLoginMember.setTextColor(isAdminSelected ? unselectedText : selectedText);

        buttonLoginAdmin.setBackgroundTintList(ColorStateList.valueOf(isAdminSelected ? selectedBg : unselectedBg));
        buttonLoginAdmin.setTextColor(isAdminSelected ? selectedText : unselectedText);
    }

    private void openMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void applyInsets(View target) {
        int start = target.getPaddingStart();
        int top = target.getPaddingTop();
        int end = target.getPaddingEnd();
        int bottom = target.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(target, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPaddingRelative(
                    start,
                    top + systemBars.top,
                    end,
                    bottom + systemBars.bottom
            );
            return insets;
        });
    }
}
