package com.sampullara.jcef.jcefbug

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel

class JCEFBugToolWindowFactory : ToolWindowFactory {
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = JCEFBugToolWindowContent(project)
        val content = ContentFactory.getInstance().createContent(toolWindowContent.contentPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class JCEFBugToolWindowContent(private val project: Project) {
    
    val contentPanel = JPanel(BorderLayout())
    
    init {
        val openButton = JButton("Open JCEF Dialog")
        openButton.addActionListener {
            val dialog = JCEFBugDialog(project)
            dialog.show()
        }

        val stressTestButton = JButton("Start Stress Test")
        stressTestButton.addActionListener {
            val stressTestAction = JCEFBugStressTestAction()
            val actionEvent = com.intellij.openapi.actionSystem.AnActionEvent.createFromDataContext(
                "ToolWindow",
                null,
                com.intellij.openapi.actionSystem.impl.SimpleDataContext.getProjectContext(project)
            )
            stressTestAction.actionPerformed(actionEvent)
        }

        val panel = JPanel()
        panel.add(openButton)
        panel.add(stressTestButton)

        contentPanel.add(panel, BorderLayout.NORTH)
    }
}
