package com.merg.quoteapp.utils;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.merg.quoteapp.R;

public final class ReportBottomSheetHelper {

    public interface ReportSubmitListener {
        void onSubmit(String reason, String description);
    }

    private ReportBottomSheetHelper() {
    }

    /**
     * Shows the quote report bottom sheet.
     *
     * @param context Android context
     * @param listener selected report values callback
     */
    public static void show(Context context, ReportSubmitListener listener) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(context, 20);
        container.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(context);
        title.setText(R.string.report_quote_title);
        title.setTextColor(ContextCompat.getColor(context, R.color.quote_text_primary));
        title.setTextSize(20);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        container.addView(title);

        TextView subtitle = new TextView(context);
        subtitle.setText(R.string.report_quote_subtitle);
        subtitle.setTextColor(ContextCompat.getColor(context, R.color.quote_text_secondary));
        subtitle.setTextSize(14);
        subtitle.setPadding(0, dp(context, 6), 0, dp(context, 12));
        container.addView(subtitle);

        RadioGroup reasonGroup = new RadioGroup(context);
        reasonGroup.setOrientation(RadioGroup.VERTICAL);
        String[] reasons = context.getResources().getStringArray(R.array.report_reasons);
        for (int index = 0; index < reasons.length; index++) {
            MaterialRadioButton button = new MaterialRadioButton(context);
            button.setId(View.generateViewId());
            button.setText(reasons[index]);
            button.setTag(reasons[index]);
            button.setTextColor(ContextCompat.getColor(context, R.color.quote_text_primary));
            reasonGroup.addView(button);
            if (index == 0) {
                button.setChecked(true);
            }
        }
        container.addView(reasonGroup);

        TextInputLayout descriptionLayout = new TextInputLayout(context);
        descriptionLayout.setHint(context.getString(R.string.report_description_hint));
        descriptionLayout.setVisibility(View.GONE);
        TextInputEditText descriptionInput = new TextInputEditText(context);
        descriptionInput.setMinLines(2);
        descriptionInput.setMaxLines(4);
        descriptionLayout.addView(descriptionInput);
        container.addView(descriptionLayout);

        MaterialButton submitButton = new MaterialButton(context);
        submitButton.setText(R.string.submit_report);
        LinearLayout.LayoutParams submitParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 52));
        submitParams.setMargins(0, dp(context, 14), 0, 0);
        submitButton.setLayoutParams(submitParams);
        container.addView(submitButton);

        reasonGroup.setOnCheckedChangeListener((group, checkedId) -> {
            View checked = group.findViewById(checkedId);
            String selected = checked == null ? "" : String.valueOf(checked.getTag());
            descriptionLayout.setVisibility(
                    selected.equals(context.getString(R.string.report_reason_other))
                            ? View.VISIBLE : View.GONE);
        });

        submitButton.setOnClickListener(view -> {
            View checked = reasonGroup.findViewById(reasonGroup.getCheckedRadioButtonId());
            String reason = checked == null ? "" : String.valueOf(checked.getTag());
            String description = descriptionInput.getText() == null
                    ? "" : descriptionInput.getText().toString();
            listener.onSubmit(reason, description);
            dialog.dismiss();
        });

        dialog.setContentView(container);
        dialog.show();
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
