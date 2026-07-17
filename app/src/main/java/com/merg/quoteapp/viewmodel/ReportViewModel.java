package com.merg.quoteapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.merg.quoteapp.model.Quote;
import com.merg.quoteapp.repository.ReportRepository;

public class ReportViewModel extends ViewModel {

    private final ReportRepository repository = ReportRepository.getInstance();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> success = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> alreadyReported = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> dailyLimitReached = new MutableLiveData<>(false);

    /**
     * Returns loading state for report submission.
     *
     * @return loading LiveData
     */
    public LiveData<Boolean> getLoading() {
        return loading;
    }

    /**
     * Returns success state for report submission.
     *
     * @return success LiveData
     */
    public LiveData<Boolean> getSuccess() {
        return success;
    }

    /**
     * Returns error message state for report submission.
     *
     * @return error LiveData
     */
    public LiveData<String> getError() {
        return error;
    }

    /**
     * Returns duplicate report state.
     *
     * @return already reported LiveData
     */
    public LiveData<Boolean> getAlreadyReported() {
        return alreadyReported;
    }

    /**
     * Returns daily report limit state.
     *
     * @return daily limit reached LiveData
     */
    public LiveData<Boolean> getDailyLimitReached() {
        return dailyLimitReached;
    }

    /**
     * Submits a report for the provided quote.
     *
     * @param quote quote to report
     * @param reason selected report reason
     * @param description optional report description
     */
    public void submitReport(Quote quote, String reason, String description) {
        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }
        if (quote == null) {
            error.setValue("Raporlanacak alıntı bulunamadı.");
            return;
        }
        resetResultStates();
        loading.setValue(true);
        repository.submitReport(
                quote.getQuoteId(),
                reason,
                description,
                new ReportRepository.ReportCallback() {
                    @Override
                    public void onSuccess() {
                        loading.setValue(false);
                        success.setValue(true);
                    }

                    @Override
                    public void onAlreadyReported() {
                        loading.setValue(false);
                        alreadyReported.setValue(true);
                    }

                    @Override
                    public void onDailyLimitReached() {
                        loading.setValue(false);
                        dailyLimitReached.setValue(true);
                    }

                    @Override
                    public void onError(String message) {
                        loading.setValue(false);
                        error.setValue(message);
                    }
                });
    }

    /**
     * Clears one-time report result states after UI handles them.
     */
    public void clearResultStates() {
        resetResultStates();
    }

    private void resetResultStates() {
        success.setValue(false);
        alreadyReported.setValue(false);
        dailyLimitReached.setValue(false);
        error.setValue(null);
    }
}
