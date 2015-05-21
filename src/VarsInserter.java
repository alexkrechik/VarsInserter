import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;

import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alexkrechik on 3/17/15.
 */
public class VarsInserter extends AnAction {

    public void actionPerformed(AnActionEvent e) {

        Project project = e.getData(PlatformDataKeys.PROJECT);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        PsiFile file = e.getData(PlatformDataKeys.PSI_FILE);


        if (editor == null ) {
            Messages.showMessageDialog(project, "Editor should be opened or active",
                    "Error", Messages.getInformationIcon());
            return;
        }

        String fileName = file.getName();
        String text = editor.getDocument().getText();

        if (!text.contains("module.exports") || !fileName.contains(".js")) {
            Messages.showMessageDialog(project, "Invalid file. Please Ensure, that file is correct .js locators file",
                    "Error", Messages.getErrorIcon());
            return;
        }

        String varsText = getVarsText(text);
        String returnText = getReturnText(text);
        String finalText = varsText.concat(returnText);
        editor.getDocument().setText(finalText);

        List<String> duplicatedVars = getDuplicates(text);
        if (duplicatedVars.size() > 0) {
            String duplicates = "";
            for (String var : duplicatedVars) {
                duplicates = duplicates.concat(var + "\n");
            }
            Messages.showMessageDialog(project, "File contains duplicated variables:\n" + duplicates,
                    "Error", Messages.getErrorIcon());
        }

    }

    private String getVarsText (String text) {
        String varsText = text.split("\\s*return.*\\{",2)[0];
        varsText = varsText.concat("\n\n");
        return varsText;
    }

    private List<String> getReturnVars (String text) {

        String varsText = getVarsText(text);
        varsText = varsText.split("\\s*module.exports.*\\{",2)[1];
        List<String> allVariables = new ArrayList<String>();

        for (String line : varsText.split("\n")) {
            if (line.contains("var") && !line.matches("^\\s*\\/\\/.*")) {
                String[] words = line.split("\\s+|=");
                for (int i=0; i<words.length-1; i++) {
                    if (words[i].equals("var")) {
                        String variable = words[i + 1].replaceAll(";","");
                        allVariables.add(variable);
                    }
                }
            }
            if (line.isEmpty()) {
                allVariables.add("");
            }
        }

        return allVariables;
    }

    private String getReturnText (String text) {

        String returnText = "\treturn {\n";
        List<String> allVariablesList = getReturnVars(text);

        for (String variable : allVariablesList) {
            if (variable.isEmpty()) {
                returnText = returnText.concat("\n");
            } else {
                returnText = returnText.concat("\t\t" + variable + " : " + variable + ",\n");
            }
        }

        returnText = returnText.replaceAll(",\\s*$","");
        returnText = returnText.concat("\n\n\t}\n}");

        return returnText;
    }

    private List<String> getDuplicates (String text) {

        List<String> allVariablesArray = getReturnVars(text);
        Set<String> allVariablesSet = new HashSet<String>();
        List<String> duplicatedVars = new ArrayList<String>();

        for (String variable : allVariablesArray) {
            if (!variable.isEmpty()) {
                if (allVariablesSet.contains(variable)) {
                    duplicatedVars.add(variable);
                } else {
                    allVariablesSet.add(variable);
                }
            }
        }

        return duplicatedVars;
    }

}
