package com.gbti.snapshotsforai.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Validates configuration files for the Snapshots for AI plugin.
 */
public class ConfigValidator {

    /**
     * Result of configuration validation.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public String getErrorMessage() {
            if (errors.isEmpty()) {
                return "";
            }
            return String.join("\n", errors);
        }

        public String getWarningMessage() {
            if (warnings.isEmpty()) {
                return "";
            }
            return String.join("\n", warnings);
        }
    }

    /**
     * Validates JSON configuration content.
     *
     * @param jsonContent Raw JSON content as string
     * @return ValidationResult with validity status and any errors/warnings
     */
    public ValidationResult validate(String jsonContent) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Check JSON syntax
        JSONObject config;
        try {
            config = new JSONObject(jsonContent);
        } catch (JSONException e) {
            errors.add("Invalid JSON syntax: " + e.getMessage());
            return new ValidationResult(false, errors, warnings);
        }

        // Check required keys
        validateRequiredKeys(config, errors);

        // Validate patterns
        if (config.has("excluded_patterns")) {
            validatePatterns(config.optJSONArray("excluded_patterns"), "excluded_patterns", warnings);
        }

        if (config.has("included_patterns")) {
            validatePatterns(config.optJSONArray("included_patterns"), "included_patterns", warnings);
        }

        // Validate default config
        if (config.has("default")) {
            validateDefaultConfig(config.optJSONObject("default"), warnings);
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    /**
     * Validates a JSONObject config directly.
     *
     * @param config JSONObject configuration
     * @return ValidationResult with validity status and any errors/warnings
     */
    public ValidationResult validate(JSONObject config) {
        return validate(config.toString());
    }

    private void validateRequiredKeys(JSONObject config, List<String> errors) {
        String[] requiredKeys = {"excluded_patterns", "included_patterns", "default"};

        for (String key : requiredKeys) {
            if (!config.has(key)) {
                errors.add("Missing required key: '" + key + "'");
            }
        }
    }

    private void validatePatterns(JSONArray patterns, String fieldName, List<String> warnings) {
        if (patterns == null) {
            return;
        }

        for (int i = 0; i < patterns.length(); i++) {
            try {
                String patternStr = patterns.getString(i);

                // Check if pattern contains glob wildcards and try to compile as regex
                if (patternStr.contains("*")) {
                    String regex = patternStr.replace(".", "\\.").replace("*", ".*");
                    try {
                        Pattern.compile(regex);
                    } catch (PatternSyntaxException e) {
                        warnings.add(String.format(
                            "Pattern in %s[%d] '%s' may not work correctly: %s",
                            fieldName, i, patternStr, e.getDescription()
                        ));
                    }
                }

                // Check for common mistakes
                if (patternStr.startsWith("/") || patternStr.startsWith("\\")) {
                    warnings.add(String.format(
                        "Pattern in %s[%d] '%s' starts with path separator - patterns should be relative",
                        fieldName, i, patternStr
                    ));
                }

            } catch (JSONException e) {
                warnings.add(String.format(
                    "Invalid pattern at %s[%d]: not a string",
                    fieldName, i
                ));
            }
        }
    }

    private void validateDefaultConfig(JSONObject defaultConfig, List<String> warnings) {
        if (defaultConfig == null) {
            return;
        }

        // Check for expected keys in default config
        String[] expectedKeys = {
            "default_prompt",
            "default_include_entire_project_structure",
            "default_include_all_files"
        };

        for (String key : expectedKeys) {
            if (!defaultConfig.has(key)) {
                warnings.add("Missing expected key in 'default': '" + key + "'");
            }
        }

        // Validate boolean types
        if (defaultConfig.has("default_include_entire_project_structure")) {
            Object value = defaultConfig.get("default_include_entire_project_structure");
            if (!(value instanceof Boolean)) {
                warnings.add("'default_include_entire_project_structure' should be a boolean");
            }
        }

        if (defaultConfig.has("default_include_all_files")) {
            Object value = defaultConfig.get("default_include_all_files");
            if (!(value instanceof Boolean)) {
                warnings.add("'default_include_all_files' should be a boolean");
            }
        }
    }
}
