package net.poe.entitylootdrops.gui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.poe.entitylootdrops.LootConfig;

/**
 * Custom configuration screen for the EntityLootDrops mod.
 * This provides a GUI for editing the mod's configuration.
 */
@OnlyIn(Dist.CLIENT)
public class ConfigScreen extends Screen {
    private final Screen parentScreen;
    private Button reloadButton;
    
    // File browser state
    private String currentDirectory = "config/EntityLootDrops";
    private List<String> fileList = new ArrayList<>();
    private int fileScrollOffset = 0;
    private static final int FILES_PER_PAGE = 8;
    
    // UI state
    private enum UIState {
        MAIN_MENU,
        FILE_BROWSER,
        FILE_EDITOR,
        CREATE_FILE
    }
    
    private UIState currentState = UIState.MAIN_MENU;
    private String currentFile = null;
    private MultilineEditBox fileContentBox;
    private EditBox fileNameBox;
    private String errorMessage = null;
    
    // Constructor should be here, right after the fields
    private static final ResourceLocation LOGO = new ResourceLocation("entitylootdrops", "textures/logo.png");
    public ConfigScreen(Screen parentScreen) {
        super(Component.literal("Entity Loot Drops Configuration"));
        this.parentScreen = parentScreen;
    }
    
    /**
     * A custom EditBox that better handles multi-line text editing.
     */
    private class MultilineEditBox extends EditBox {
        private int scrollOffset = 0;
        private final int lineHeight = 12;
        private List<String> lines = new ArrayList<>();
        private String fullText = "";
        private boolean editable = true;
        private net.minecraft.client.gui.Font fontRenderer;
        private int maxLineWidth;
        
        // Add cursor position tracking
        private int cursorPos = 0;
        private int cursorLine = 0;
        private int cursorColumn = 0;
        private long lastCursorBlink = 0;
        private boolean cursorVisible = true;
        
        public MultilineEditBox(net.minecraft.client.gui.Font font, int x, int y, int width, int height, Component message) {
            super(font, x, y, width, height, message);
            this.fontRenderer = font;
            this.maxLineWidth = width - 10; // Leave some padding
            this.lastCursorBlink = System.currentTimeMillis();
        }
        
        @Override
        public void setValue(String text) {
            super.setValue(text);
            this.fullText = text;
            this.cursorPos = text.length(); // Set cursor at the end
            updateLines();
            updateCursorPosition();
        }
        
        @Override
        public String getValue() {
            return this.fullText;
        }
        
        @Override
        public void setEditable(boolean editable) {
            super.setEditable(editable);
            this.editable = editable;
        }
        
        private boolean isEditableCustom() {
            return this.editable;
        }
        
        private void updateLines() {
            // First split by newlines
            String[] rawLines = this.fullText.split("\n", -1);
            lines = new ArrayList<>();
            
            // Then wrap each line to fit the width
            for (String rawLine : rawLines) {
                if (rawLine.isEmpty()) {
                    lines.add("");
                    continue;
                }
                
                // Handle indentation for JSON
                String indentation = "";
                int i = 0;
                while (i < rawLine.length() && Character.isWhitespace(rawLine.charAt(i))) {
                    indentation += rawLine.charAt(i);
                    i++;
                }
                
                // Wrap the line
                String remaining = rawLine;
                while (!remaining.isEmpty()) {
                    int endIndex = findBreakPoint(remaining, maxLineWidth);
                    if (endIndex == 0) endIndex = Math.min(remaining.length(), 1); // Ensure progress
                    
                    String part = remaining.substring(0, endIndex);
                    lines.add(part);
                    
                    remaining = remaining.substring(endIndex);
                    // Add indentation to continuation lines
                    if (!remaining.isEmpty() && !indentation.isEmpty()) {
                        remaining = indentation + "  " + remaining; // Extra indent for continuation
                    }
                }
            }
            
            // Make sure cursor is in a valid position
            updateCursorPosition();
        }
        
        private void updateCursorPosition() {
            // Find which line and column the cursor is at
            int pos = 0;
            cursorLine = 0;
            cursorColumn = 0;
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (pos + line.length() >= cursorPos) {
                    cursorLine = i;
                    cursorColumn = cursorPos - pos;
                    break;
                }
                pos += line.length();
                
                // Add 1 for the newline character if not the last line
                if (i < lines.size() - 1) {
                    pos += 1;
                }
            }
            
            // Ensure cursor is visible by scrolling if needed
            ensureCursorVisible();
        }
        
        private void ensureCursorVisible() {
            int visibleLines = this.height / lineHeight;
            
            // If cursor is above visible area, scroll up
            if (cursorLine < scrollOffset) {
                scrollOffset = cursorLine;
            }
            // If cursor is below visible area, scroll down
            else if (cursorLine >= scrollOffset + visibleLines) {
                scrollOffset = cursorLine - visibleLines + 1;
            }
        }
        
        private int findBreakPoint(String text, int maxWidth) {
            // If the text fits, return its length
            if (fontRenderer.width(text) <= maxWidth) {
                return text.length();
            }
            
            // Find the last space before exceeding maxWidth
            int width = 0;
            int lastSpace = -1;
            
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                int charWidth = fontRenderer.width(String.valueOf(c));
                
                if (c == ' ' || c == ',' || c == ':' || c == ';') {
                    lastSpace = i;
                }
                
                width += charWidth;
                
                if (width > maxWidth) {
                    // If we found a space, break there
                    if (lastSpace != -1) {
                        return lastSpace + 1; // Include the space in the current line
                    }
                    // Otherwise break at the current position
                    return i;
                }
            }
            
            // If we get here, the entire text fits
            return text.length();
        }
        
        @Override
        public boolean charTyped(char c, int modifiers) {
            if (this.isVisible() && this.isFocused() && this.isEditableCustom()) {
                if (c == '\n' || c == '\r') {
                    insertText("\n");
                } else {
                    insertText(String.valueOf(c));
                }
                return true;
            }
            return false;
        }
        
        @Override
        public void insertText(String text) {
        if (cursorPos < 0) cursorPos = 0;
        if (cursorPos > fullText.length()) cursorPos = fullText.length();
    
        // Insert the text at the cursor position
        this.fullText = this.fullText.substring(0, cursorPos) + text + this.fullText.substring(cursorPos);
        cursorPos += text.length();
        updateLines();
        }
        
        private void deleteText(int count) {
            if (cursorPos <= 0 || fullText.isEmpty()) return;
            
            int start = Math.max(0, cursorPos - count);
            this.fullText = this.fullText.substring(0, start) + this.fullText.substring(cursorPos);
            cursorPos = start;
            updateLines();
        }
        
        @Override
        public void moveCursor(int amount) {
        cursorPos = Math.max(0, Math.min(fullText.length(), cursorPos + amount));
        updateCursorPosition();
        lastCursorBlink = System.currentTimeMillis();
        cursorVisible = true;
        }
        
        private void moveCursorToLineStart() {
            // Find the start of the current line
            int pos = 0;
            for (int i = 0; i < cursorLine; i++) {
                pos += lines.get(i).length();
                if (i < lines.size() - 1) {
                    pos += 1; // Add 1 for newline
                }
            }
            cursorPos = pos;
            cursorColumn = 0;
            lastCursorBlink = System.currentTimeMillis();
            cursorVisible = true;
        }
        
        private void moveCursorToLineEnd() {
            // Find the end of the current line
            int pos = 0;
            for (int i = 0; i <= cursorLine; i++) {
                pos += lines.get(i).length();
                if (i < cursorLine) {
                    pos += 1; // Add 1 for newline
                }
            }
            cursorPos = pos;
            cursorColumn = lines.get(cursorLine).length();
            lastCursorBlink = System.currentTimeMillis();
            cursorVisible = true;
        }
        
        private void moveCursorVertical(int lineDelta) {
            // Move cursor up or down by the specified number of lines
            int targetLine = Math.max(0, Math.min(lines.size() - 1, cursorLine + lineDelta));
            
            if (targetLine != cursorLine) {
                // Calculate position at the start of the current line
                int startOfCurrentLine = 0;
                for (int i = 0; i < cursorLine; i++) {
                    startOfCurrentLine += lines.get(i).length();
                    if (i < lines.size() - 1) {
                        startOfCurrentLine += 1; // Add 1 for newline
                    }
                }
                
                // Calculate position at the start of the target line
                int startOfTargetLine = 0;
                for (int i = 0; i < targetLine; i++) {
                    startOfTargetLine += lines.get(i).length();
                    if (i < lines.size() - 1) {
                        startOfTargetLine += 1; // Add 1 for newline
                    }
                }
                
                // Try to maintain the same column position
                int targetColumn = Math.min(cursorColumn, lines.get(targetLine).length());
                cursorPos = startOfTargetLine + targetColumn;
                cursorLine = targetLine;
                cursorColumn = targetColumn;
                
                ensureCursorVisible();
                lastCursorBlink = System.currentTimeMillis();
                cursorVisible = true;
            }
        }
        
        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (this.isVisible() && this.isFocused()) {
                // Handle ESC key specially - don't consume it, let the screen handle it
                if (keyCode == 256) { // ESC key
                    return false;
                }
                
                if (this.isEditableCustom()) {
                    // Handle Enter key for adding new lines
                    if (keyCode == 257 || keyCode == 335) { // Enter or numpad Enter
                        insertText("\n");
                        return true;
                    }
                    
                    // Handle backspace for deleting characters
                    if (keyCode == 259) { // Backspace
                        deleteText(1);
                        return true;
                    }
                    
                    // Handle delete key
                    if (keyCode == 261) { // Delete key
                        if (cursorPos < fullText.length()) {
                            this.fullText = this.fullText.substring(0, cursorPos) + 
                                            this.fullText.substring(cursorPos + 1);
                            updateLines();
                        }
                        return true;
                    }
                    
                    // Handle tab key for indentation
                    if (keyCode == 258) { // Tab key
                        insertText("    "); // 4 spaces for tab
                        return true;
                    }
                    
                    // Handle left/right arrow keys for cursor movement
                    if (keyCode == 263) { // Left arrow
                        moveCursor(-1);
                        return true;
                    }
                    if (keyCode == 262) { // Right arrow
                        moveCursor(1);
                        return true;
                    }
                    
                    // Handle home/end keys
                    if (keyCode == 268) { // Home key
                        moveCursorToLineStart();
                        return true;
                    }
                    if (keyCode == 269) { // End key
                        moveCursorToLineEnd();
                        return true;
                    }
                }
                
                // Handle up/down arrow keys for cursor movement
                if (keyCode == 265) { // Up arrow
                    moveCursorVertical(-1);
                    return true;
                }
                if (keyCode == 264) { // Down arrow
                    moveCursorVertical(1);
                    return true;
                }
                
                // Handle page up/down for scrolling
                if (keyCode == 266) { // Page up
                    int visibleLines = this.height / lineHeight;
                    scrollOffset = Math.max(0, scrollOffset - visibleLines);
                    moveCursorVertical(-visibleLines);
                    return true;
                }
                if (keyCode == 267) { // Page down
                    int visibleLines = this.height / lineHeight;
                    scrollOffset = Math.min(Math.max(0, lines.size() - visibleLines), scrollOffset + visibleLines);
                    moveCursorVertical(visibleLines);
                    return true;
                }
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            boolean result = super.mouseClicked(mouseX, mouseY, button);
            
            // Make sure we get focus when clicked
            if (isMouseOver(mouseX, mouseY)) {
                setFocused(true);
                
                // Set cursor position based on click location
                if (this.isEditableCustom()) {
                    int clickedLine = scrollOffset + (int)((mouseY - this.getY()) / lineHeight);
                    if (clickedLine >= 0 && clickedLine < lines.size()) {
                        // Calculate position at the start of the clicked line
                        int startOfLine = 0;
                        for (int i = 0; i < clickedLine; i++) {
                            startOfLine += lines.get(i).length();
                            if (i < lines.size() - 1) {
                                startOfLine += 1; // Add 1 for newline
                            }
                        }
                        
                        // Find the closest character to the click position
                        String line = lines.get(clickedLine);
                        int bestPos = 0;
                        int bestDist = Integer.MAX_VALUE;
                        
                        for (int i = 0; i <= line.length(); i++) {
                            int charX = this.getX() + 4 + fontRenderer.width(line.substring(0, i));
                            int dist = (int)Math.abs(mouseX - charX);
                            
                            if (dist < bestDist) {
                                bestDist = dist;
                                bestPos = i;
                            }
                        }
                        
                        cursorPos = startOfLine + bestPos;
                        updateCursorPosition();
                        lastCursorBlink = System.currentTimeMillis();
                        cursorVisible = true;
                    }
                }
                
                return true;
            }
            return result;
        }
        
        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (isMouseOver(mouseX, mouseY)) {
                if (delta > 0) {
                    scrollOffset = Math.max(0, scrollOffset - 3);
                } else {
                    scrollOffset = Math.min(scrollOffset + 3, Math.max(0, lines.size() - (getHeight() / lineHeight)));
                }
                return true;
            }
            return false;
        }
        
        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            if (this.isVisible()) {
                // Draw background
                guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.width + 1, this.getY() + this.height + 1, -6250336);
                guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, -16777216);
                
                // Check if JSON is valid
                boolean isValidJson = true;
                String jsonError = null;
                try {
                    JsonParser.parseString(this.fullText);
                } catch (Exception e) {
                    isValidJson = false;
                    jsonError = e.getMessage();
                    if (jsonError != null && jsonError.length() > 50) {
                        jsonError = jsonError.substring(0, 47) + "...";
                    }
                }
                
                // Draw JSON validation status
                if (this.fullText.trim().startsWith("{") || this.fullText.trim().startsWith("[")) {
                    String validationText = isValidJson ? "✓ Valid JSON" : "✗ Invalid JSON";
                    int validationColor = isValidJson ? 0x55FF55 : 0xFF5555;
                    guiGraphics.drawString(this.fontRenderer, validationText, 
                        this.getX() + this.width - this.fontRenderer.width(validationText) - 10, 
                        this.getY() + 5, validationColor);
                    
                    // Draw error message if JSON is invalid
                    if (!isValidJson && jsonError != null) {
                        guiGraphics.drawString(this.fontRenderer, jsonError, 
                            this.getX() + 5, this.getY() + this.height - 15, 0xFF5555);
                    }
                }
                
                // Draw text content
                int visibleLines = this.height / lineHeight;
                int endLine = Math.min(lines.size(), scrollOffset + visibleLines);
                
                for (int i = scrollOffset; i < endLine; i++) {
                    String line = lines.get(i);
                    int yPos = this.getY() + (i - scrollOffset) * lineHeight + 5;
                    
                    // Enhanced JSON syntax highlighting
                    if (line.trim().isEmpty()) {
                        // Empty line
                        continue;
                    } else if (line.contains(":")) {
                        // Key-value pair
                        int colonIndex = line.indexOf(":");
                        String key = line.substring(0, colonIndex + 1);
                        String value = colonIndex + 1 < line.length() ? line.substring(colonIndex + 1) : "";
                        
                        // Draw key in cyan
                        guiGraphics.drawString(this.fontRenderer, key, this.getX() + 4, yPos, 0x55FFFF);
                        
                        // Draw value with different colors based on type
                        if (value.trim().startsWith("\"")) {
                            // String value - green
                            guiGraphics.drawString(this.fontRenderer, value, 
                                this.getX() + 4 + this.fontRenderer.width(key), yPos, 0x55FF55);
                        } else if (value.trim().matches("\\s*\\d+(\\.\\d+)?\\s*,?")) {
                            // Number value - yellow
                            guiGraphics.drawString(this.fontRenderer, value, 
                                this.getX() + 4 + this.fontRenderer.width(key), yPos, 0xFFFF55);
                        } else if (value.trim().matches("\\s*(true|false)\\s*,?")) {
                            // Boolean value - purple
                            guiGraphics.drawString(this.fontRenderer, value, 
                                this.getX() + 4 + this.fontRenderer.width(key), yPos, 0xFF55FF);
                        } else if (value.trim().matches("\\s*(null)\\s*,?")) {
                            // Null value - gray
                            guiGraphics.drawString(this.fontRenderer, value, 
                                this.getX() + 4 + this.fontRenderer.width(key), yPos, 0xAAAAAA);
                        } else {
                            // Other values - white
                            guiGraphics.drawString(this.fontRenderer, value, 
                                this.getX() + 4 + this.fontRenderer.width(key), yPos, 0xFFFFFF);
                        }
                    } else {
                        // Handle special characters
                        String trimmed = line.trim();
                        if (trimmed.matches("[{}]")) {
                            // Braces - orange
                            guiGraphics.drawString(this.fontRenderer, line, this.getX() + 4, yPos, 0xFFAA00);
                        } else if (trimmed.matches("[\\[\\]]")) {
                            // Brackets - light blue
                            guiGraphics.drawString(this.fontRenderer, line, this.getX() + 4, yPos, 0x00AAFF);
                        } else if (trimmed.equals(",")) {
                            // Comma - gray
                            guiGraphics.drawString(this.fontRenderer, line, this.getX() + 4, yPos, 0xAAAAAA);
                        } else if (trimmed.startsWith("\"") && trimmed.endsWith("\",")) {
                            // String array item - green
                            guiGraphics.drawString(this.fontRenderer, line, this.getX() + 4, yPos, 0x55FF55);
                        } else if (trimmed.matches("\\d+(\\.\\d+)?,?")) {
                            // Number array item - yellow
                            guiGraphics.drawString(this.fontRenderer, line, this.getX() + 4, yPos, 0xFFFF55);
                        } else if (trimmed.matches("(true|false),?")) {
                            // Boolean array item - purple
                            guiGraphics.drawString(this.fontRenderer, line, this.getX() + 4, yPos, 0xFF55FF);
                        } else {
                            // Default color - white
                            guiGraphics.drawString(this.fontRenderer, line, this.getX() + 4, yPos, 0xFFFFFF);
                        }
                    }
                }
                
                // Draw scroll indicator if needed
                if (lines.size() > visibleLines) {
                    int scrollBarHeight = Math.max(20, visibleLines * this.height / lines.size());
                    int scrollBarY = this.getY() + (scrollOffset * (this.height - scrollBarHeight) / Math.max(1, lines.size() - visibleLines));
                    
                    guiGraphics.fill(this.getX() + this.width - 6, this.getY(), this.getX() + this.width, this.getY() + this.height, -7303024);
                    guiGraphics.fill(this.getX() + this.width - 5, scrollBarY, this.getX() + this.width - 1, scrollBarY + scrollBarHeight, -3092272);
                }
                
                // Draw cursor if focused
                if (this.isFocused() && this.isEditableCustom()) {
                    // Update cursor blink state
                    if (System.currentTimeMillis() - lastCursorBlink > 500) {
                        cursorVisible = !cursorVisible;
                        lastCursorBlink = System.currentTimeMillis();
                    }
                    
                    // Only draw cursor if it's in the visible state and the line is visible
                    if (cursorVisible && cursorLine >= scrollOffset && cursorLine < endLine) {
                        String lineBeforeCursor = lines.get(cursorLine).substring(0, cursorColumn);
                        int cursorX = this.getX() + 4 + this.fontRenderer.width(lineBeforeCursor);
                        int cursorY = this.getY() + (cursorLine - scrollOffset) * lineHeight + 5;
                        
                        guiGraphics.fill(cursorX, cursorY, cursorX + 1, cursorY + 10, -3092272);
                    }
                }
            }
        }
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Clear existing widgets before adding new ones
        clearWidgets();
        
        // If user doesn't have edit permission and tries to access edit screens, redirect to file browser
        if (!hasEditPermission() && (currentState == UIState.CREATE_FILE)) {
            currentState = UIState.FILE_BROWSER;
            errorMessage = "You don't have permission to create files. Required permission level: 2";
        }
        
        switch (currentState) {
            case MAIN_MENU:
                initMainMenu();
                break;
            case FILE_BROWSER:
                initFileBrowser();
                break;
            case FILE_EDITOR:
                initFileEditor();
                break;
            case CREATE_FILE:
                initCreateFile();
                break;
        }
    }
    
    /**
     * Checks if the current player has permission to edit configurations.
     * @return true if in single-player or if the player has permission level 2 or higher
     */
    private boolean hasEditPermission() {
        Minecraft minecraft = Minecraft.getInstance();
        
        // Always allow editing in single-player
        if (minecraft.isSingleplayer()) {
            return true;
        }
        
        // In multiplayer, require permission level 2
        return minecraft.player != null && minecraft.player.hasPermissions(2);
    }

    private void initMainMenu() {
        int y = 50;
        
        // Add title for events section
        y += 10;
        
        boolean canEdit = hasEditPermission();
        
        // Add file browser button
        Button fileBrowserButton = Button.builder(
            Component.literal("Browse Configuration Files"),
            (btn) -> {
                // Open file browser
                currentState = UIState.FILE_BROWSER;
                loadFileList();
                this.init(); // Reinitialize the screen
            }
        ).pos(this.width / 2 - 100, y).size(200, 20).build();
        
        this.addRenderableWidget(fileBrowserButton);
        y += 24;
        
        // Add reload button
        reloadButton = Button.builder(
            Component.literal("Reload Configuration"),
            (btn) -> {
                // Reload the configuration
                LootConfig.loadConfig();
                // Close and reopen the screen to refresh
                Minecraft.getInstance().setScreen(new ConfigScreen(parentScreen));
            }
        ).pos(this.width / 2 - 100, y).size(200, 20).build();
        
        this.addRenderableWidget(reloadButton);
        y += 24;
        
        // Add done button
        Button doneButton = Button.builder(
            Component.literal("Done"),
            (btn) -> {
                // Close the screen
                Minecraft.getInstance().setScreen(parentScreen);
            }
        ).pos(this.width / 2 - 100, y).size(200, 20).build();
        
        this.addRenderableWidget(doneButton);
        
        // Add a message if user doesn't have edit permission
        if (!canEdit) {
            errorMessage = "View-only mode. You need permission level 2 to make changes.";
        }
    }
    
    private void initFileBrowser() {
        // Add title and current directory
        
        // Add back button
        Button backButton = Button.builder(
            Component.literal("Back to Main Menu"),
            (btn) -> {
                currentState = UIState.MAIN_MENU;
                this.init();
            }
        ).pos(10, 10).size(120, 20).build();
        
        this.addRenderableWidget(backButton);
        
        // Add parent directory button if not in root
        if (!currentDirectory.equals("config/EntityLootDrops")) {
            Button parentDirButton = Button.builder(
                Component.literal("↑ Parent Directory"),
                (btn) -> {
                    // Go to parent directory
                    Path current = Paths.get(currentDirectory);
                    Path parent = current.getParent();
                    if (parent != null && parent.toString().contains("EntityLootDrops")) {
                        currentDirectory = parent.toString();
                        loadFileList();
                        this.init();
                    }
                }
            ).pos(140, 10).size(120, 20).build();
            
            this.addRenderableWidget(parentDirButton);
        }
        
        // Add create file button only if user has edit permission
        if (hasEditPermission()) {
            Button createFileButton = Button.builder(
                Component.literal("Create New File"),
                (btn) -> {
                    currentState = UIState.CREATE_FILE;
                    this.init();
                }
            ).pos(this.width - 130, 10).size(120, 20).build();
            
            this.addRenderableWidget(createFileButton);
        }
        
        // Add file list with scroll buttons
        int y = 50;
        
        // Add scroll up button if needed
        if (fileScrollOffset > 0) {
            Button scrollUpButton = Button.builder(
                Component.literal("↑"),
                (btn) -> {
                    fileScrollOffset = Math.max(0, fileScrollOffset - FILES_PER_PAGE);
                    this.init();
                }
            ).pos(this.width / 2 - 100, y).size(200, 20).build();
            
            this.addRenderableWidget(scrollUpButton);
            y += 24;
        }
        
        // Add file buttons
        int endIndex = Math.min(fileList.size(), fileScrollOffset + FILES_PER_PAGE);
        for (int i = fileScrollOffset; i < endIndex; i++) {
            final String fileName = fileList.get(i);
            final Path filePath = Paths.get(currentDirectory, fileName);
            
            Button fileButton;
            if (Files.isDirectory(filePath)) {
                // Directory button
                fileButton = Button.builder(
                    Component.literal("📁 " + fileName),
                    (btn) -> {
                        // Navigate to directory
                        currentDirectory = filePath.toString();
                        loadFileList();
                        this.init();
                    }
                ).pos(this.width / 2 - 150, y).size(300, 20).build();
            } else {
                // File button with edit and delete options
                fileButton = Button.builder(
                    Component.literal("📄 " + fileName),
                    (btn) -> {
                        // Open file editor or view-only mode based on permissions
                        currentFile = filePath.toString();
                        currentState = UIState.FILE_EDITOR;
                        this.init();
                    }
                ).pos(this.width / 2 - 150, y).size(240, 20).build();
                
                // Add delete button only if user has edit permission
                if (hasEditPermission()) {
                    Button deleteButton = Button.builder(
                        Component.literal("❌"),
                        (btn) -> {
                            // Delete file
                            try {
                                Files.delete(filePath);
                                loadFileList();
                                this.init();
                            } catch (IOException e) {
                                errorMessage = "Failed to delete file: " + e.getMessage();
                            }
                        }
                    ).pos(this.width / 2 + 100, y).size(50, 20).build();
                    deleteButton.setTooltip(Tooltip.create(Component.literal("Delete file")));
                    
                    this.addRenderableWidget(deleteButton);
                }
            }
            
            this.addRenderableWidget(fileButton);
            y += 24;
        }
        
        // Add scroll down button if needed
        if (endIndex < fileList.size()) {
            Button scrollDownButton = Button.builder(
                Component.literal("↓"),
                (btn) -> {
                    fileScrollOffset += FILES_PER_PAGE;
                    this.init();
                }
            ).pos(this.width / 2 - 100, y).size(200, 20).build();
            
            this.addRenderableWidget(scrollDownButton);
        }
    }
    
    private void initFileEditor() {
        // Add title
        
        // Add back button
        Button backButton = Button.builder(
            Component.literal("Back to File Browser"),
            (btn) -> {
                currentState = UIState.FILE_BROWSER;
                this.init();
            }
        ).pos(10, 10).size(150, 20).build();
        
        this.addRenderableWidget(backButton);
        
        // Add save button only if user has edit permission
        if (hasEditPermission()) {
            Button saveButton = Button.builder(
                Component.literal("Save File"),
                (btn) -> {
                    // Save file
                    try {
                        String content = fileContentBox.getValue();
                        
                        // Try to format JSON
                        try {
                            JsonElement jsonElement = JsonParser.parseString(content);
                            Gson gson = new GsonBuilder().setPrettyPrinting().create();
                            content = gson.toJson(jsonElement);
                            fileContentBox.setValue(content);
                        } catch (Exception e) {
                            errorMessage = "Warning: Invalid JSON format! File saved as-is.";
                        }
                        
                        Files.write(Paths.get(currentFile), content.getBytes());
                        errorMessage = "File saved successfully!";
                    } catch (IOException e) {
                        errorMessage = "Failed to save file: " + e.getMessage();
                    }
                }
            ).pos(this.width - 160, 10).size(150, 20).build();
            
            this.addRenderableWidget(saveButton);
            
            // Add format JSON button
            Button formatButton = Button.builder(
                Component.literal("Format JSON"),
                (btn) -> {
                    // Format JSON
                    try {
                        String content = fileContentBox.getValue();
                        JsonElement jsonElement = JsonParser.parseString(content);
                        Gson gson = new GsonBuilder().setPrettyPrinting().create();
                        content = gson.toJson(jsonElement);
                        fileContentBox.setValue(content);
                        errorMessage = "JSON formatted successfully!";
                    } catch (Exception e) {
                        errorMessage = "Invalid JSON format: " + e.getMessage();
                    }
                }
            ).pos(this.width - 320, 10).size(150, 20).build();
            
            this.addRenderableWidget(formatButton);
        }
        
        // Create a better text editor for file content
fileContentBox = new MultilineEditBox(this.font, this.width / 2 - 200, 50, 400, this.height - 100, Component.literal("File Content"));
fileContentBox.setMaxLength(100000);
fileContentBox.setEditable(hasEditPermission()); // Only editable if user has permission

// Load file content
try {
    String content = new String(Files.readAllBytes(Paths.get(currentFile)));
    
    // Try to format JSON
    try {
        JsonElement jsonElement = JsonParser.parseString(content);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        content = gson.toJson(jsonElement);
    } catch (Exception e) {
        // Not valid JSON or other error, use as is
    }
    
    fileContentBox.setValue(content);
} catch (IOException e) {
    fileContentBox.setValue("Error loading file: " + e.getMessage());
}

this.addRenderableWidget(fileContentBox);

// Add a message if user doesn't have edit permission
if (!hasEditPermission()) {
    errorMessage = "View-only mode. You need permission level 2 to edit files.";
}
}

private void initCreateFile() {
// Add title

// Add back button
Button backButton = Button.builder(
    Component.literal("Back to File Browser"),
    (btn) -> {
        currentState = UIState.FILE_BROWSER;
        this.init();
    }
).pos(10, 10).size(150, 20).build();

this.addRenderableWidget(backButton);

// Add file name edit box
fileNameBox = new EditBox(this.font, this.width / 2 - 100, 50, 200, 20, Component.literal("File Name"));
fileNameBox.setMaxLength(100);
fileNameBox.setEditable(true);
fileNameBox.setValue("new_file.json");

this.addRenderableWidget(fileNameBox);

// Add file content edit box - use MultilineEditBox here too
fileContentBox = new MultilineEditBox(this.font, this.width / 2 - 200, 100, 400, this.height - 150, Component.literal("File Content"));
fileContentBox.setMaxLength(100000);
fileContentBox.setEditable(true);
fileContentBox.setValue("{\n  \"_comment\": \"New configuration file\",\n  \"itemId\": \"minecraft:diamond\",\n  \"dropChance\": 10.0,\n  \"minAmount\": 1,\n  \"maxAmount\": 3\n}");

this.addRenderableWidget(fileContentBox);

// Add create button
Button createButton = Button.builder(
    Component.literal("Create File"),
    (btn) -> {
        // Create file
        try {
            String fileName = fileNameBox.getValue();
            if (!fileName.endsWith(".json")) {
                fileName += ".json";
            }
            
            Path filePath = Paths.get(currentDirectory, fileName);
            
            // Check if file already exists
            if (Files.exists(filePath)) {
                errorMessage = "File already exists!";
                return;
            }
            
            String content = fileContentBox.getValue();
            
            // Try to format JSON
            try {
                JsonElement jsonElement = JsonParser.parseString(content);
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                content = gson.toJson(jsonElement);
            } catch (Exception e) {
                errorMessage = "Invalid JSON format!";
                return;
            }
            
            Files.write(filePath, content.getBytes());
            
            // Return to file browser
            currentState = UIState.FILE_BROWSER;
            loadFileList();
            this.init();
        } catch (IOException e) {
            errorMessage = "Failed to create file: " + e.getMessage();
        }
    }
).pos(this.width / 2 - 100, this.height - 40).size(200, 20).build();

this.addRenderableWidget(createButton);
}

private void loadFileList() {
fileList.clear();
File dir = new File(currentDirectory);

if (dir.exists() && dir.isDirectory()) {
    // Add directories first
    Arrays.stream(dir.listFiles())
        .filter(File::isDirectory)
        .map(File::getName)
        .sorted()
        .forEach(fileList::add);
    
    // Then add files
    Arrays.stream(dir.listFiles())
        .filter(file -> !file.isDirectory() && file.getName().endsWith(".json"))
        .map(File::getName)
        .sorted()
        .forEach(fileList::add);
}
}

@Override
public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
this.renderBackground(guiGraphics);

// Render title
guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

// Render current directory if in file browser
if (currentState == UIState.FILE_BROWSER) {
    guiGraphics.drawString(this.font, "Current Directory: " + currentDirectory, this.width / 2 - 200, 35, 0xCCCCCC);
}

// Render current file if in file editor
if (currentState == UIState.FILE_EDITOR && currentFile != null) {
    guiGraphics.drawString(this.font, "Editing: " + currentFile, this.width / 2 - 200, 35, 0xCCCCCC);
    
    // Show read-only indicator if user doesn't have edit permission
    if (!hasEditPermission()) {
        guiGraphics.drawString(this.font, "[READ ONLY]", this.width / 2 + 100, 35, 0xFF5555);
    }
}

// Render error message if any
if (errorMessage != null) {
    guiGraphics.drawCenteredString(this.font, errorMessage, this.width / 2, this.height - 20, 0xFF5555);
}

super.render(guiGraphics, mouseX, mouseY, partialTick);
}

@Override
public boolean shouldCloseOnEsc() {
// If the text editor has focus, don't close or change screens
if ((fileContentBox != null && fileContentBox.isFocused()) || 
    (fileNameBox != null && fileNameBox.isFocused())) {
    return false;
}

if (currentState != UIState.MAIN_MENU) {
    // Go back to previous screen instead of closing
    currentState = currentState == UIState.FILE_BROWSER ? UIState.MAIN_MENU : UIState.FILE_BROWSER;
    // Instead of calling init directly, schedule a refresh
    this.minecraft.setScreen(this);
    return false;
}
return true;
}

@Override
public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
// Handle ESC key specially
if (keyCode == 256) { // ESC key
    if (fileContentBox != null && fileContentBox.isFocused()) {
        fileContentBox.setFocused(false);
        return true;
    }
    if (fileNameBox != null && fileNameBox.isFocused()) {
        fileNameBox.setFocused(false);
        return true;
    }
}

// Let the focused widget handle the key press first
if (this.getFocused() != null && this.getFocused().keyPressed(keyCode, scanCode, modifiers)) {
    return true;
}

// If no widget handled it, handle it at the screen level
return super.keyPressed(keyCode, scanCode, modifiers);
}

/**
* Registers this screen as the config screen for the mod.
* This should be called during mod initialization.
*/
public static void register() {
ModLoadingContext.get().registerExtensionPoint(
    ConfigScreenHandler.ConfigScreenFactory.class,
    () -> new ConfigScreenHandler.ConfigScreenFactory(
        (minecraft, screen) -> new ConfigScreen(screen)
    )
);
} 
}
// End of ConfigScreen.java
