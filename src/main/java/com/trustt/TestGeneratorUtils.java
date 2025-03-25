package com.trustt;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestGeneratorUtils {

    public static String getTestPath(VirtualFile sourceFile, Project project) {
        String projectRoot = project.getBasePath();
        if (projectRoot == null) throw new RuntimeException("Project root not found");

        String sourcePath = sourceFile.getPath();
        String relativePath = sourcePath.replace(projectRoot, "")
                .replace("/src/main/java/", "/src/test/java/")
                .replace(".java", "Test.java");

        return projectRoot + relativePath;
    }

    public static String extractJavaCodeBlock(String text) {
        Pattern pattern = Pattern.compile("```java\\s+(.*?)```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) return matcher.group(1).trim();
        return text.trim(); // fallback
    }
}
