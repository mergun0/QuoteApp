package com.merg.quoteapp.utils;

import com.google.firebase.firestore.DocumentSnapshot;
import com.merg.quoteapp.model.Quote;

public final class QuoteVisibilityUtils {

    public static final String HIDDEN_QUOTE_MESSAGE = "Bu alıntı artık görüntülenemiyor.";

    private QuoteVisibilityUtils() {
    }

    public static boolean isHidden(DocumentSnapshot document) {
        if (document == null) {
            return false;
        }
        return Boolean.TRUE.equals(document.getBoolean("isHidden"));
    }

    public static boolean isVisible(DocumentSnapshot document) {
        return !isHidden(document);
    }

    public static boolean isVisible(Quote quote) {
        return quote != null && !quote.isHidden();
    }
}
