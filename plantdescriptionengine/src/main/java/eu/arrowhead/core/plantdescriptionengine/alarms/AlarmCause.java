package eu.arrowhead.core.plantdescriptionengine.alarms;

public enum AlarmCause {
    systemInactive("appears to be inactive."),
    systemNotRegistered("cannot be found in the Service Registry."),
    systemNotInDescription("is not present in the active Plant Description.");

    private String description;

    private AlarmCause(String description) {
        this.description = description;
    }

    public String getDescription(String systemIdentifier) {
        return systemIdentifier + " " + this.description;
    }
}