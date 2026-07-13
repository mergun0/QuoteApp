package com.merg.quoteapp.ui.quote;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.merg.quoteapp.R;
import com.merg.quoteapp.model.Quote;
import com.merg.quoteapp.model.QuoteState;
import com.merg.quoteapp.viewmodel.QuoteViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AddQuoteActivity extends AppCompatActivity {

    public static final String EXTRA_QUOTE_ID = "quoteId";
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_TEXT = "text";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_AUTHOR = "author";
    public static final String EXTRA_CHARACTER = "character";
    public static final String EXTRA_SEASON = "season";
    public static final String EXTRA_EPISODE = "episode";
    public static final String EXTRA_TAGS = "tags";
    public static final String EXTRA_SPOILER = "spoiler";

    private Spinner typeSpinner;
    private TextInputEditText quoteTextInput;
    private TextInputEditText titleInput;
    private TextInputEditText authorInput;
    private TextInputEditText characterInput;
    private TextInputEditText seasonInput;
    private TextInputEditText episodeInput;
    private TextInputEditText tagsInput;
    private TextInputLayout titleLayout;
    private TextInputLayout authorLayout;
    private TextInputLayout characterLayout;
    private MaterialCheckBox spoilerCheck;
    private MaterialButton saveButton;
    private TextView statusText;
    private LinearLayout seriesDetails;
    private boolean editing;
    private String quoteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_quote);
        bindViews();
        setupTypeSpinner();
        readEditData();

        QuoteViewModel viewModel = new ViewModelProvider(this).get(QuoteViewModel.class);
        viewModel.getOperationState().observe(this, this::renderState);
        saveButton.setOnClickListener(view -> viewModel.saveQuote(readQuote(), editing));
    }

    private void bindViews() {
        typeSpinner = findViewById(R.id.spinnerQuoteType);
        quoteTextInput = findViewById(R.id.editQuoteText);
        titleInput = findViewById(R.id.editQuoteTitle);
        authorInput = findViewById(R.id.editQuoteAuthor);
        characterInput = findViewById(R.id.editQuoteCharacter);
        titleLayout = findViewById(R.id.layoutQuoteTitle);
        authorLayout = findViewById(R.id.layoutQuoteAuthor);
        characterLayout = findViewById(R.id.layoutQuoteCharacter);
        seasonInput = findViewById(R.id.editQuoteSeason);
        episodeInput = findViewById(R.id.editQuoteEpisode);
        tagsInput = findViewById(R.id.editQuoteTags);
        spoilerCheck = findViewById(R.id.checkQuoteSpoiler);
        saveButton = findViewById(R.id.buttonSaveQuote);
        statusText = findViewById(R.id.textQuoteFormStatus);
        seriesDetails = findViewById(R.id.layoutSeriesDetails);
    }

    private void setupTypeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.quote_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);
        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = parent.getItemAtPosition(position).toString();
                boolean isSeries = "Dizi".equals(selectedType);
                seriesDetails.setVisibility(isSeries ? View.VISIBLE : View.GONE);
                updateTypeLabels(selectedType);
                if (!isSeries) {
                    seasonInput.setText("");
                    episodeInput.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                seriesDetails.setVisibility(View.GONE);
                updateTypeLabels("Film");
            }
        });
    }

    private void updateTypeLabels(String type) {
        if ("Kitap".equals(type)) {
            titleLayout.setHint(getString(R.string.book_title_label));
            authorLayout.setHint(getString(R.string.book_author_label));
            characterLayout.setHint(getString(R.string.character_optional));
        } else if ("Dizi".equals(type)) {
            titleLayout.setHint(getString(R.string.series_title_label));
            authorLayout.setHint(getString(R.string.author_director));
            characterLayout.setHint(getString(R.string.character_optional));
        } else {
            titleLayout.setHint(getString(R.string.movie_title_label));
            authorLayout.setHint(getString(R.string.movie_author_label));
            characterLayout.setHint(getString(R.string.character_optional));
        }
    }

    private void readEditData() {
        quoteId = getIntent().getStringExtra(EXTRA_QUOTE_ID);
        editing = quoteId != null && !quoteId.isEmpty();
        if (!editing) {
            return;
        }

        ((TextView) findViewById(R.id.textQuoteFormTitle)).setText(R.string.edit_quote);
        saveButton.setText(R.string.update_quote);
        selectType(getIntent().getStringExtra(EXTRA_TYPE));
        quoteTextInput.setText(getIntent().getStringExtra(EXTRA_TEXT));
        titleInput.setText(getIntent().getStringExtra(EXTRA_TITLE));
        authorInput.setText(getIntent().getStringExtra(EXTRA_AUTHOR));
        characterInput.setText(getIntent().getStringExtra(EXTRA_CHARACTER));
        seasonInput.setText(getIntent().getStringExtra(EXTRA_SEASON));
        episodeInput.setText(getIntent().getStringExtra(EXTRA_EPISODE));
        tagsInput.setText(getIntent().getStringExtra(EXTRA_TAGS));
        spoilerCheck.setChecked(getIntent().getBooleanExtra(EXTRA_SPOILER, false));
    }

    private void selectType(String type) {
        if (type == null) {
            return;
        }
        for (int index = 0; index < typeSpinner.getCount(); index++) {
            if (type.equals(typeSpinner.getItemAtPosition(index).toString())) {
                typeSpinner.setSelection(index);
                return;
            }
        }
    }

    private Quote readQuote() {
        Quote quote = new Quote();
        quote.setQuoteId(quoteId);
        quote.setType(typeSpinner.getSelectedItem().toString());
        quote.setText(textOf(quoteTextInput).trim());
        quote.setTitle(textOf(titleInput).trim());
        quote.setAuthor(textOf(authorInput).trim());
        quote.setCharacterName(textOf(characterInput).trim());
        quote.setSeason(textOf(seasonInput).trim());
        quote.setEpisode(textOf(episodeInput).trim());
        quote.setTags(parseTags(textOf(tagsInput)));
        quote.setSpoiler(spoilerCheck.isChecked());
        return quote;
    }

    private List<String> parseTags(String rawTags) {
        if (rawTags == null || rawTags.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(rawTags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private void renderState(QuoteState state) {
        boolean loading = state.getStatus() == QuoteState.Status.LOADING;
        saveButton.setEnabled(!loading);

        if (loading) {
            hideStatus();
        } else if (state.getStatus() == QuoteState.Status.ERROR) {
            showStatus(state.getMessage(), true);
        } else {
            showStatus(getString(editing ? R.string.quote_updated : R.string.quote_saved), false);
            statusText.postDelayed(this::finish, 900L);
        }
    }

    private void showStatus(String message, boolean error) {
        statusText.setText(message);
        statusText.setTextColor(getColor(error
                ? R.color.quote_status_error : R.color.quote_status_success));
        statusText.setVisibility(View.VISIBLE);
    }

    private void hideStatus() {
        statusText.setText("");
        statusText.setVisibility(View.GONE);
    }

    private String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
