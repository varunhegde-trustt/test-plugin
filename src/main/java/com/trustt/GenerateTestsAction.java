package com.trustt;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GenerateTestsAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            Messages.showErrorDialog("No open project found", "Error");
            return;
        }

        SettingsState settings = SettingsState.getInstance();
        String apiKey = settings.openAiApiKey;

        if (apiKey == null || apiKey.isBlank()) {
            apiKey = Messages.showInputDialog(
                    project,
                    "Enter your OpenAI API Key:",
                    "OpenAI API Key Required",
                    Messages.getQuestionIcon()
            );

            if (apiKey == null || apiKey.isBlank()) {
                Messages.showErrorDialog("API Key is required to proceed.", "Error");
                return;
            }

            settings.openAiApiKey = apiKey;
        }

        VirtualFile selectedFolder = FileChooser.chooseFile(
                FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                project, null
        );

        if (selectedFolder == null) {
            Messages.showErrorDialog("No folder selected", "Error");
            return;
        }

        List<VirtualFile> javaFiles = new ArrayList<>();
        VfsUtil.visitChildrenRecursively(selectedFolder, new VirtualFileVisitor<>() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                if (!file.isDirectory() && file.getName().endsWith(".java")) {
                    javaFiles.add(file);
                }
                return true;
            }
        });

        if (javaFiles.isEmpty()) {
            Messages.showErrorDialog("No Java files found", "Error");
            return;
        }

        for (VirtualFile file : javaFiles) {
            try {
                processJavaFile(project, file, apiKey);
            } catch (Exception ex) {
                Messages.showErrorDialog("❌ Failed: " + ex.getMessage(), "Error");
                ex.printStackTrace();
            }
        }

        Messages.showInfoMessage("✅ Test generation completed!", "Success");
    }


    private void processJavaFile(Project project, VirtualFile file, String apiKey) throws Exception {
        String content = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
        String testPath = TestGeneratorUtils.getTestPath(file, project);
        File testFile = new File(testPath);

        if (testFile.exists()) {
            System.out.println("⏭️ Skipped existing test: " + testFile.getAbsolutePath());
            return;
        }

        String testCode = generateTestWithOpenAI(content, apiKey);
        testFile.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write(testCode);
        }

        System.out.println("✅ Generated test: " + testFile.getAbsolutePath());
    }


    private String generateTestWithOpenAI(String javaCode, String apiKey) throws Exception {
        String prompt = "Generate JUnit 5 test cases with full coverage for the following class:\n\n" +
                "Requirements:\n" +
                "- Ensure 100% test coverage for all methods, including private, protected, and public methods.\n" +
                "- Mock all dependencies, including @Autowired services, using @Mock and @InjectMocks.\n" +
                "- Exception Handling:\n" +
                "  - If a method throws an exception, use thenAnswer(invocation -> { throw new Exception(\"message\"); }) instead of thenThrow().\n" +
                "  - Ensure correct exception messages are passed while throwing exceptions.\n" +
                "  - Use assertThrows(ExpectedException.class, () -> methodCall()) to verify exceptions.\n" +
                "- Stub Only Necessary Methods:\n" +
                "  - Avoid unnecessary stubbings that are not used in assertions.\n" +
                "  - Use lenient() for mocks that might not always be invoked to prevent strict stubbing errors.\n" +
                "- Ensure Mocked Methods Match Actual Calls:\n" +
                "  - Ensure stubbed method parameters match the actual arguments used in method calls to avoid argument mismatch errors.\n" +
                "- Fix Too Many Actual Invocations Error:\n" +
                "  - Verify mock method calls using verify(mock, times(n)) to match expected invocations.\n" +
                "- Ensure Proper Coverage for All Edge Cases:\n" +
                "  - Cover scenarios with valid, invalid, null, and empty values.\n" +
                "  - Test handling of exceptions thrown from dependent services.\n" +
                "- Check for Proper Exception Handling in Edge Cases:\n" +
                "  - If an exception is expected but a NullPointerException occurs instead, ensure all required mock values are set properly.\n\n" +
                "Class under test:\n\n" +
                javaCode;

        URL url = new URL("https://api.openai.com/v1/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");

        JSONObject body = new JSONObject();
        body.put("model", "gpt-4");

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", "You are an expert Java test engineer."));
        messages.put(new JSONObject().put("role", "user").put("content", prompt));

        body.put("messages", messages);
        body.put("temperature", 0.7);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        InputStream is = conn.getResponseCode() < 400 ? conn.getInputStream() : conn.getErrorStream();
        String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        JSONObject responseJson = new JSONObject(response);
        JSONArray choices = responseJson.getJSONArray("choices");
        String content = choices.getJSONObject(0).getJSONObject("message").getString("content");

        return TestGeneratorUtils.extractJavaCodeBlock(content);
    }
}
