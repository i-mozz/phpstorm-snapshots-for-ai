package com.gbti.snapshotsforai.util;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class PatternMatcherTest {

    private PatternMatcher matcher;
    private static final String BASE_PATH = "/project/root";

    @Before
    public void setUp() {
        matcher = new PatternMatcher();
    }

    // ========== Binary Image Tests ==========

    @Test
    public void testIsBinaryImageFile_jpg() {
        assertTrue(matcher.isBinaryImageFile("image.jpg"));
        assertTrue(matcher.isBinaryImageFile("path/to/image.jpg"));
        assertTrue(matcher.isBinaryImageFile("/absolute/path/image.JPG"));
    }

    @Test
    public void testIsBinaryImageFile_jpeg() {
        assertTrue(matcher.isBinaryImageFile("photo.jpeg"));
        assertTrue(matcher.isBinaryImageFile("photo.JPEG"));
    }

    @Test
    public void testIsBinaryImageFile_png() {
        assertTrue(matcher.isBinaryImageFile("screenshot.png"));
        assertTrue(matcher.isBinaryImageFile("icon.PNG"));
    }

    @Test
    public void testIsBinaryImageFile_gif() {
        assertTrue(matcher.isBinaryImageFile("animation.gif"));
    }

    @Test
    public void testIsBinaryImageFile_bmp() {
        assertTrue(matcher.isBinaryImageFile("bitmap.bmp"));
    }

    @Test
    public void testIsBinaryImageFile_tiff() {
        assertTrue(matcher.isBinaryImageFile("scan.tiff"));
    }

    @Test
    public void testIsBinaryImageFile_webp() {
        assertTrue(matcher.isBinaryImageFile("modern.webp"));
    }

    @Test
    public void testIsBinaryImageFile_ico() {
        assertTrue(matcher.isBinaryImageFile("favicon.ico"));
    }

    @Test
    public void testIsBinaryImageFile_notImage() {
        assertFalse(matcher.isBinaryImageFile("code.java"));
        assertFalse(matcher.isBinaryImageFile("style.css"));
        assertFalse(matcher.isBinaryImageFile("data.json"));
        assertFalse(matcher.isBinaryImageFile("readme.md"));
    }

    @Test
    public void testIsBinaryImageFile_svg() {
        // SVG should NOT be considered binary image
        assertFalse(matcher.isBinaryImageFile("icon.svg"));
        assertFalse(matcher.isBinaryImageFile("icon.SVG"));
    }

    // ========== SVG Tests ==========

    @Test
    public void testIsSvgFile() {
        assertTrue(matcher.isSvgFile("icon.svg"));
        assertTrue(matcher.isSvgFile("path/to/icon.svg"));
        assertTrue(matcher.isSvgFile("icon.SVG"));
        assertTrue(matcher.isSvgFile("ICON.Svg"));
    }

    @Test
    public void testIsSvgFile_notSvg() {
        assertFalse(matcher.isSvgFile("icon.png"));
        assertFalse(matcher.isSvgFile("icon.jpg"));
        assertFalse(matcher.isSvgFile("svgfile.txt"));
    }

    // ========== Filter Binary Images Tests ==========

    @Test
    public void testFilterOutBinaryImages() {
        List<String> files = Arrays.asList(
            "code.java",
            "image.png",
            "icon.svg",
            "photo.jpg",
            "style.css"
        );

        List<String> filtered = matcher.filterOutBinaryImages(files);

        assertEquals(3, filtered.size());
        assertTrue(filtered.contains("code.java"));
        assertTrue(filtered.contains("icon.svg"));
        assertTrue(filtered.contains("style.css"));
        assertFalse(filtered.contains("image.png"));
        assertFalse(filtered.contains("photo.jpg"));
    }

    @Test
    public void testFilterOutBinaryImages_keepsSvg() {
        List<String> files = Arrays.asList("logo.svg", "logo.png");
        List<String> filtered = matcher.filterOutBinaryImages(files);

        assertEquals(1, filtered.size());
        assertTrue(filtered.contains("logo.svg"));
    }

    // ========== Exclude Pattern Tests ==========

    @Test
    public void testCompileExcludePatterns() {
        JSONArray patterns = new JSONArray();
        patterns.put(".git");
        patterns.put("node_modules");

        List<Pattern> compiled = matcher.compileExcludePatterns(patterns, BASE_PATH);

        assertEquals(2, compiled.size());
    }

    @Test
    public void testCompileExcludePatterns_null() {
        List<Pattern> compiled = matcher.compileExcludePatterns(null, BASE_PATH);
        assertTrue(compiled.isEmpty());
    }

    @Test
    public void testIsExcluded_gitDirectory() {
        JSONArray patterns = new JSONArray();
        patterns.put(".git");

        List<Pattern> compiled = matcher.compileExcludePatterns(patterns, BASE_PATH);

        assertTrue(matcher.isExcluded(BASE_PATH + "/.git/config", compiled));
        assertTrue(matcher.isExcluded(BASE_PATH + "/.git/HEAD", compiled));
    }

    @Test
    public void testIsExcluded_nodeModules() {
        JSONArray patterns = new JSONArray();
        patterns.put("node_modules");

        List<Pattern> compiled = matcher.compileExcludePatterns(patterns, BASE_PATH);

        assertTrue(matcher.isExcluded(BASE_PATH + "/node_modules/package/index.js", compiled));
    }

    @Test
    public void testIsExcluded_wildcardPattern() {
        JSONArray patterns = new JSONArray();
        patterns.put("*.log");

        List<Pattern> compiled = matcher.compileExcludePatterns(patterns, BASE_PATH);

        assertTrue(matcher.isExcluded(BASE_PATH + "/app.log", compiled));
        assertTrue(matcher.isExcluded(BASE_PATH + "/logs/debug.log", compiled));
    }

    // ========== Include Pattern Tests ==========

    @Test
    public void testCompileIncludePatterns() {
        JSONArray patterns = new JSONArray();
        patterns.put("package.json");
        patterns.put("build.gradle");

        List<Pattern> compiled = matcher.compileIncludePatterns(patterns);

        assertEquals(2, compiled.size());
    }

    @Test
    public void testIsIncluded_emptyPatterns() {
        List<Pattern> empty = matcher.compileIncludePatterns(null);
        assertTrue(matcher.isIncluded("/any/file.txt", empty));
    }

    @Test
    public void testIsIncluded_packageJson() {
        JSONArray patterns = new JSONArray();
        patterns.put("package.json");

        List<Pattern> compiled = matcher.compileIncludePatterns(patterns);

        assertTrue(matcher.isIncluded("/project/package.json", compiled));
        assertTrue(matcher.isIncluded("/project/subdir/package.json", compiled));
    }

    // ========== Excluded Directory Tests ==========

    @Test
    public void testIsInExcludedDirectory() {
        JSONArray patterns = new JSONArray();
        patterns.put("node_modules");

        List<Pattern> compiled = matcher.compileExcludePatterns(patterns, BASE_PATH);

        assertTrue(matcher.isInExcludedDirectory(
            BASE_PATH + "/node_modules/lodash/index.js", compiled));
    }

    @Test
    public void testIsInExcludedDirectory_notExcluded() {
        JSONArray patterns = new JSONArray();
        patterns.put("node_modules");

        List<Pattern> compiled = matcher.compileExcludePatterns(patterns, BASE_PATH);

        assertFalse(matcher.isInExcludedDirectory(
            BASE_PATH + "/src/main.js", compiled));
    }

    // ========== Should Include File Tests ==========

    @Test
    public void testShouldIncludeFile_normalFile() {
        JSONArray excludePatterns = new JSONArray();
        excludePatterns.put(".git");

        JSONArray includePatterns = new JSONArray();

        List<Pattern> exclude = matcher.compileExcludePatterns(excludePatterns, BASE_PATH);
        List<Pattern> include = matcher.compileIncludePatterns(includePatterns);

        assertTrue(matcher.shouldIncludeFile(BASE_PATH + "/src/Main.java", exclude, include));
    }

    @Test
    public void testShouldIncludeFile_excludedFile() {
        JSONArray excludePatterns = new JSONArray();
        excludePatterns.put(".git");

        List<Pattern> exclude = matcher.compileExcludePatterns(excludePatterns, BASE_PATH);
        List<Pattern> include = matcher.compileIncludePatterns(null);

        assertFalse(matcher.shouldIncludeFile(BASE_PATH + "/.git/config", exclude, include));
    }

    @Test
    public void testShouldIncludeFile_fileInExcludedDirectory() {
        JSONArray excludePatterns = new JSONArray();
        excludePatterns.put("node_modules");

        List<Pattern> exclude = matcher.compileExcludePatterns(excludePatterns, BASE_PATH);
        List<Pattern> include = matcher.compileIncludePatterns(null);

        assertFalse(matcher.shouldIncludeFile(
            BASE_PATH + "/node_modules/package/index.js", exclude, include));
    }
}
