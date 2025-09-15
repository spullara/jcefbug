package com.sampullara.jcef.jcefbug

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class JCEFBugAction : AnAction("Open JCEF Dialog") {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val dialog = JCEFBugDialog(project)
        dialog.show()
    }
}
