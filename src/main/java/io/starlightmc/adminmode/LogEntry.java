package io.starlightmc.adminmode;

import lombok.Value;

import java.util.Date;

@Value
public class LogEntry {
    private final Date timestamp;
    private final String playerName;
    private final String type;
    private final String text;

    @Override
    public String toString() {
        return timestamp.toString() + "," + playerName + "," + type.toUpperCase() + "," + text;
    }
}
