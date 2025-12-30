package com.gbti.snapshotsforai.util;

import org.json.JSONArray;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility class for file pattern matching operations used in snapshot generation.
 */
public class PatternMatcher {

    private static final Pattern BINARY_IMAGE_PATTERN =
        Pattern.compile(".*\\.(jpg|jpeg|png|gif|bmp|tiff|webp|ico)$", Pattern.CASE_INSENSITIVE);

    /**
     * Compiles exclude patterns from JSON array into regex patterns.
     *
     * @param excludedPatterns JSON array of pattern strings
     * @param basePath         Project base path for anchoring patterns
     * @return List of compiled regex patterns
     */
    public List<Pattern> compileExcludePatterns(JSONArray excludedPatterns, String basePath) {
        List<Pattern> patterns = new ArrayList<>();
        if (excludedPatterns == null) {
            return patterns;
        }

        String normalizedBasePath = basePath.replace("\\", "/");

        for (int i = 0; i < excludedPatterns.length(); i++) {
            String patternStr = excludedPatterns.getString(i);
            // Convert glob pattern to regex: escape special chars, then convert * to .*
            String quotedPattern = Pattern.quote(patternStr).replace("\\E.*\\Q", ".*").replace("\\Q\\E", "");
            // Also handle the case when * is alone: \Q*\E -> .*
            if (patternStr.contains("*")) {
                quotedPattern = patternStr.replace(".", "\\.").replace("*", ".*");
            }
            String regex = "^" + Pattern.quote(normalizedBasePath) + "(/.*)?/\\.?" + quotedPattern + ".*$";
            patterns.add(Pattern.compile(regex));
        }

        return patterns;
    }

    /**
     * Compiles include patterns from JSON array into regex patterns.
     *
     * @param includedPatterns JSON array of pattern strings
     * @return List of compiled regex patterns
     */
    public List<Pattern> compileIncludePatterns(JSONArray includedPatterns) {
        List<Pattern> patterns = new ArrayList<>();
        if (includedPatterns == null) {
            return patterns;
        }

        for (int i = 0; i < includedPatterns.length(); i++) {
            String patternStr = includedPatterns.getString(i);
            String regex = patternStr.replace(".", "\\.").replace("*", ".*");
            patterns.add(Pattern.compile(".*/" + regex + "$"));
        }

        return patterns;
    }

    /**
     * Checks if a file path matches any of the exclude patterns.
     *
     * @param filePath        Normalized file path (forward slashes)
     * @param excludePatterns List of compiled exclude patterns
     * @return true if the file matches any exclude pattern
     */
    public boolean isExcluded(String filePath, List<Pattern> excludePatterns) {
        for (Pattern pattern : excludePatterns) {
            if (pattern.matcher(filePath).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a file path matches any of the include patterns.
     * If no include patterns are defined, all files are considered included.
     *
     * @param filePath        Normalized file path (forward slashes)
     * @param includePatterns List of compiled include patterns
     * @return true if the file matches any include pattern or if no patterns defined
     */
    public boolean isIncluded(String filePath, List<Pattern> includePatterns) {
        if (includePatterns.isEmpty()) {
            return true;
        }
        for (Pattern pattern : includePatterns) {
            if (pattern.matcher(filePath).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a file is located within an excluded directory.
     *
     * @param filePath        Normalized file path (forward slashes)
     * @param excludePatterns List of compiled exclude patterns
     * @return true if any parent directory matches an exclude pattern
     */
    public boolean isInExcludedDirectory(String filePath, List<Pattern> excludePatterns) {
        Path path = Paths.get(filePath).getParent();
        while (path != null) {
            if (isExcluded(path.toString().replace('\\', '/'), excludePatterns)) {
                return true;
            }
            path = path.getParent();
        }
        return false;
    }

    /**
     * Checks if a file is a binary image file (non-SVG).
     *
     * @param filePath File path to check
     * @return true if the file is a binary image
     */
    public boolean isBinaryImageFile(String filePath) {
        return BINARY_IMAGE_PATTERN.matcher(filePath).matches();
    }

    /**
     * Checks if a file is an SVG file.
     *
     * @param filePath File path to check
     * @return true if the file is an SVG
     */
    public boolean isSvgFile(String filePath) {
        return filePath.toLowerCase().endsWith(".svg");
    }

    /**
     * Filters out binary image files from a list, keeping SVG files.
     *
     * @param filePaths List of file paths to filter
     * @return Filtered list excluding binary images but including SVGs
     */
    public List<String> filterOutBinaryImages(List<String> filePaths) {
        List<String> filtered = new ArrayList<>();
        for (String filePath : filePaths) {
            if (!isBinaryImageFile(filePath) || isSvgFile(filePath)) {
                filtered.add(filePath);
            }
        }
        return filtered;
    }

    /**
     * Determines if a file should be included in the snapshot based on patterns.
     *
     * @param filePath        Normalized file path
     * @param excludePatterns List of compiled exclude patterns
     * @param includePatterns List of compiled include patterns
     * @return true if the file should be included
     */
    public boolean shouldIncludeFile(String filePath, List<Pattern> excludePatterns, List<Pattern> includePatterns) {
        boolean inExcludedDir = isInExcludedDirectory(filePath, excludePatterns);
        if (inExcludedDir) {
            return false;
        }

        boolean included = isIncluded(filePath, includePatterns);
        boolean excluded = isExcluded(filePath, excludePatterns);

        // Include patterns take priority over exclude patterns
        if (included) {
            return true;
        }

        return !excluded;
    }
}
