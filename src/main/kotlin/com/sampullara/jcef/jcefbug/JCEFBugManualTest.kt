package com.sampullara.jcef.jcefbug

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class JCEFBugManualTestAction : AnAction("Test Auto-Close Dialog") {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        Messages.showInfoMessage(
            project,
            "This will open a non-modal dialog that should automatically close\n" +
            "when it receives the echo response from JavaScript.\n\n" +
            "Watch the console for debug messages.",
            "Manual Auto-Close Test"
        )
        
        val dialog = JCEFBugDialog(project, autoCloseOnEcho = true)
        dialog.show()
        
        println("Manual test dialog opened - should auto-close when JS sends echo message")
    }
}
