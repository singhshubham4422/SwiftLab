package com.example.invoiceapp.dto;

import com.example.invoiceapp.model.enums.DataMode;
import jakarta.validation.constraints.NotNull;

public class ModeSwitchRequest {
    @NotNull(message = "Target data mode is required")
    private DataMode targetMode;

    private boolean confirm = false;

    public ModeSwitchRequest() {}

    public ModeSwitchRequest(DataMode targetMode, boolean confirm) {
        this.targetMode = targetMode;
        this.confirm = confirm;
    }

    public DataMode getTargetMode() { return targetMode; }
    public void setTargetMode(DataMode targetMode) { this.targetMode = targetMode; }
    public boolean isConfirm() { return confirm; }
    public void setConfirm(boolean confirm) { this.confirm = confirm; }
}
