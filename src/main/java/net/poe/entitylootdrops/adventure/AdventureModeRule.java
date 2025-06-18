package net.poe.entitylootdrops.adventure;

import java.util.List;

public class AdventureModeRule {
    private boolean enabled;
    private String dimension;
    private List<String> allowedBlockBreakIDs;
    private boolean preventBlockPlacement;
    private List<String> allowedPlacementIDs;
    private boolean preventBlockModification;
    private List<String> allowedModificationIDs;
    private String comment;
    private String breakMessage;
    private String placeMessage;
    private String modifyMessage;

    // Getters and setters

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }

    public List<String> getAllowedBlockBreakIDs() { return allowedBlockBreakIDs; }
    public void setAllowedBlockBreakIDs(List<String> allowedBlockBreakIDs) { this.allowedBlockBreakIDs = allowedBlockBreakIDs; }

    public boolean isPreventBlockPlacement() { return preventBlockPlacement; }
    public void setPreventBlockPlacement(boolean preventBlockPlacement) { this.preventBlockPlacement = preventBlockPlacement; }

    public List<String> getAllowedPlacementIDs() { return allowedPlacementIDs; }
    public void setAllowedPlacementIDs(List<String> allowedPlacementIDs) { this.allowedPlacementIDs = allowedPlacementIDs; }

    public boolean isPreventBlockModification() { return preventBlockModification; }
    public void setPreventBlockModification(boolean preventBlockModification) { this.preventBlockModification = preventBlockModification; }

    public List<String> getAllowedModificationIDs() { return allowedModificationIDs; }
    public void setAllowedModificationIDs(List<String> allowedModificationIDs) { this.allowedModificationIDs = allowedModificationIDs; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getBreakMessage() { return breakMessage; }
    public void setBreakMessage(String breakMessage) { this.breakMessage = breakMessage; }

    public String getPlaceMessage() { return placeMessage; }
    public void setPlaceMessage(String placeMessage) { this.placeMessage = placeMessage; }

    public String getModifyMessage() { return modifyMessage; }
    public void setModifyMessage(String modifyMessage) { this.modifyMessage = modifyMessage; }
}