package com.trustt;

import com.intellij.openapi.components.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "TrusttSettings",
        storages = {@Storage("TrusttSettings.xml")}
)
public class SettingsState implements PersistentStateComponent<SettingsState> {
    public String openAiApiKey;

    public static SettingsState getInstance() {
        return ServiceManager.getService(SettingsState.class);
    }

    @Nullable
    @Override
    public SettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull SettingsState state) {
        this.openAiApiKey = state.openAiApiKey;
    }
}
