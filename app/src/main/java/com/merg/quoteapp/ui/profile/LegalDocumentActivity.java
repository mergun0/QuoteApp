package com.merg.quoteapp.ui.profile;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.RawRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.merg.quoteapp.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class LegalDocumentActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_RAW_RES_ID = "extra_raw_res_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legal_document);

        MaterialToolbar toolbar = findViewById(R.id.toolbarLegalDocument);
        TextView contentText = findViewById(R.id.textLegalDocumentContent);

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        int rawResId = getIntent().getIntExtra(EXTRA_RAW_RES_ID, 0);

        toolbar.setTitle(title == null || title.trim().isEmpty()
                ? getString(R.string.legal_document_title) : title);
        toolbar.setNavigationOnClickListener(view -> finish());

        if (rawResId == 0) {
            contentText.setText(R.string.legal_content_missing);
        } else {
            contentText.setText(readRawText(rawResId));
        }
    }

    private String readRawText(@RawRes int rawResId) {
        StringBuilder builder = new StringBuilder();
        try (InputStream stream = getResources().openRawResource(rawResId);
             BufferedReader reader = new BufferedReader(new InputStreamReader(
                     stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        } catch (IOException | RuntimeException error) {
            return getString(R.string.legal_content_missing);
        }
        return builder.toString().trim();
    }
}
