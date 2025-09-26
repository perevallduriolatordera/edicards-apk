package net.ifeu.edicards.Services;

import java.util.ArrayList;

public class ImportResult {
    private boolean success;
    private ArrayList<String> errorMessages;

    public ImportResult() {
        this.success = true;
        this.errorMessages = new ArrayList<>();
    }

    public ImportResult(boolean success) {
        this.success = success;
        this.errorMessages = new ArrayList<>();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ArrayList<String> getErrorMessages() {
        return errorMessages;
    }

    public void addErrorMessage(String message) {
        this.errorMessages.add(message);
        this.success = false;
    }

    public void addErrorMessages(ArrayList<String> messages) {
        this.errorMessages.addAll(messages);
        if (!messages.isEmpty()) {
            this.success = false;
        }
    }

    public boolean hasErrors() {
        return !errorMessages.isEmpty();
    }

    public int getErrorCount() {
        return errorMessages.size();
    }
}