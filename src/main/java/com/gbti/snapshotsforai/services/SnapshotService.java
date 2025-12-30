package com.gbti.snapshotsforai.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public final class SnapshotService {
    private static final Logger LOG = Logger.getInstance(SnapshotService.class);
    private final Project project;

    public SnapshotService(Project project) {
        this.project = project;
    }

    public void initializeSnapshotDirectory() throws IOException {
        LOG.info("Initializing snapshot directory");
        String basePath = project.getBasePath();
        if (basePath == null) {
            LOG.error("Project base path is null");
            throw new IOException("Project base path is null");
        }
        LOG.info("Project base path: " + basePath);

        Path snapshotsDir = Paths.get(basePath, ".snapshots");
        LOG.info("Snapshots directory path: " + snapshotsDir);

        if (!Files.exists(snapshotsDir)) {
            LOG.info("Creating snapshots directory");
            Files.createDirectory(snapshotsDir);
        } else {
            LOG.info("Snapshots directory already exists");
        }

        Path configFilePath = snapshotsDir.resolve("config.json");
        if (!Files.exists(configFilePath)) {
            LOG.info("Creating config file at: " + configFilePath);
            JSONObject config = new JSONObject();

            // Load patterns from resource files
            config.put("excluded_patterns", loadPatternsFromResource("/defaults/excluded_patterns.json"));
            config.put("included_patterns", loadPatternsFromResource("/defaults/included_patterns.json"));
            config.put("default", loadDefaultConfigFromResource());

            Files.write(configFilePath, config.toString(4).getBytes(StandardCharsets.UTF_8));
        } else {
            LOG.info("Config file already exists at: " + configFilePath);
        }

        Path readmeFilePath = snapshotsDir.resolve("readme.md");
        String readmeContent = "# Snapshots for AI\n\n" +
                "## Configuration\n\n" +
                "The `config.json` file allows you to customize the behavior of the Snapshots for AI plugin.\n\n" +
                "### Options\n\n" +
                "- `excluded_patterns`: A list of patterns to exclude from the project structure snapshot. Patterns include:\n" +
                "  - `.git`\n" +
                "  - `.gitignore`\n" +
                "  - `gradle`\n" +
                "  - `gradlew`\n" +
                "  - `gradlew.*`\n" +
                "  - `node_modules`\n" +
                "  - `vendor`\n" +
                "  - `.snapshots`\n" +
                "  - `.idea`\n" +
                "  - `.vscode`\n" +
                "  - `*.log`\n" +
                "  - `*.tmp`\n" +
                "  - `target`\n" +
                "  - `dist`\n" +
                "  - `build`\n" +
                "  - `.DS_Store`\n" +
                "  - `*.bak`\n" +
                "  - `*.swp`\n" +
                "  - `*.swo`\n" +
                "  - `*.lock`\n" +
                "  - `*.iml`\n" +
                "  - `coverage`\n" +
                "  - `*.min.js`\n" +
                "  - `*.min.css`\n" +
                "  - `netlify.toml`\n" +
                "  - `package-lock.json`\n" +
                "  - `__pycache__`\n" +
                "  - `LICENSE`\n" +
                "- `included_patterns`: A list of patterns to include in the project structure snapshot. Patterns include:\n" +
                "  - `build.gradle`\n" +
                "  - `settings.gradle`\n" +
                "  - `gradle.properties`\n" +
                "  - `pom.xml`\n" +
                "  - `Makefile`\n" +
                "  - `CMakeLists.txt`\n" +
                "  - `package.json`\n" +
                "  - `package-lock.json`\n" +
                "  - `yarn.lock`\n" +
                "  - `requirements.txt`\n" +
                "  - `Pipfile`\n" +
                "  - `Pipfile.lock`\n" +
                "  - `Gemfile`\n" +
                "  - `Gemfile.lock`\n" +
                "  - `composer.json`\n" +
                "  - `composer.lock`\n" +
                "  - `.editorconfig`\n" +
                "  - `.eslintrc.json`\n" +
                "  - `.eslintrc.js`\n" +
                "  - `.prettierrc`\n" +
                "  - `.babelrc`\n" +
                "  - `.env`\n" +
                "  - `.dockerignore`\n" +
                "  - `.gitattributes`\n" +
                "  - `.stylelintrc`\n" +
                "  - `.npmrc`\n\n" +
                "## Default Configuration\n\n" +
                "- `default_prompt`: The default prompt text that will be displayed in the snapshot dialog.\n" +
                "- `default_include_entire_project_structure`: Whether to include the entire project structure by default when creating a snapshot.\n" +
                "- `default_include_all_files`: Whether to include all project files by default when creating a snapshot.\n" +
                "- `max_file_size_kb`: Maximum size of a single file in KB (default: 1024 KB = 1 MB). Files exceeding this limit will be skipped.\n" +
                "- `max_total_size_mb`: Maximum total size of all files in MB (default: 10 MB). Once this limit is reached, remaining files will be skipped.\n" +
                "\n" +
                "## Usage\n\n" +
                "To create a snapshot, follow these steps:\n\n" +
                "### From the Tools Menu\n" +
                "1. Open the `Tools` menu in PHPStorm.\n" +
                "2. Select `Create Snapshot`.\n" +
                "3. Enter your prompt (if not using the default prompt).\n" +
                "4. Select the files to include in the snapshot.\n" +
                "5. Click `OK` to generate the snapshot.\n\n" +
                "### From the Main Toolbar\n" +
                "1. Click on the `Create Snapshot` icon in the main toolbar.\n" +
                "2. Follow the same steps as above to create a snapshot.\n\n" +
                "The snapshot will be saved in the `.snapshots` directory within your project.\n";

        Files.write(readmeFilePath, readmeContent.getBytes());

        VirtualFileManager.getInstance().asyncRefresh(() -> {
            LOG.info("Virtual file system refreshed");
        });
    }

    private JSONArray loadPatternsFromResource(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                LOG.warn("Resource not found: " + resourcePath + ", using empty array");
                return new JSONArray();
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(content);
            return json.getJSONArray("patterns");
        } catch (IOException e) {
            LOG.error("Error loading patterns from " + resourcePath, e);
            return new JSONArray();
        }
    }

    private JSONObject loadDefaultConfigFromResource() {
        try (InputStream is = getClass().getResourceAsStream("/defaults/default_config.json")) {
            if (is == null) {
                LOG.warn("Default config resource not found, using hardcoded defaults");
                JSONObject defaults = new JSONObject();
                defaults.put("default_prompt", "Enter your prompt here");
                defaults.put("default_include_entire_project_structure", true);
                defaults.put("default_include_all_files", false);
                return defaults;
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return new JSONObject(content);
        } catch (IOException e) {
            LOG.error("Error loading default config", e);
            JSONObject defaults = new JSONObject();
            defaults.put("default_prompt", "Enter your prompt here");
            defaults.put("default_include_entire_project_structure", true);
            defaults.put("default_include_all_files", false);
            return defaults;
        }
    }
}
