# Plan: #1379 sub-project 1 - project lifecycle MCP operations

Status: DESIGN APPROVED (bounded, in chat, 2026-11-17)

## Approved design
Four MCP operations exposing the GUI project flows with non-interactive, parameter-driven decisions; interactive parity (windows front + focused); Ask permissions for flow ops.

- create_project: name, optional template-id (absent = default template), optional template-data mode (with-data | pipeline-only, default with-data), optional save-to (immediate first save, allowed roots). Opens front+focused.
- open_project: path, format (auto | jipx | legacy | crate, default auto), optional force-current-window. auto resolves the .jipx-sibling / crate prompts non-interactively (modern choice = the GUI default). Opens front+focused.
- save_project: optional project, optional save-to (save-as when given), format (auto | jipx | legacy, default auto; auto = the isLegacyCompatible gate). Non-prompting; WAITS for the async JIPX save (bounded) and returns the real outcome.
- get_project_metadata: optional project. name, path, save path, modified flag. Read-only.

Key requirement: NO behavior fork. The decision logic inside the GUI dialogs (sibling choice, format prompt, isLegacyCompatible gate) is extracted into parameter-driven helpers that BOTH the dialogs and the ops call; the GUI keeps prompting exactly as today.

User rulings: windows come to front and focus (interactive parity; the user waits for the AI and wants to see what it does). Small sub-projects first, interaction toolkit later.

## Branch
1379-project-lifecycle (from 1369-mcp-server tip 96932227d6; rebase onto master after !198 merges).

## Verified facts
- ProjectOperations.java exists; ops are nested static classes, requiresEdt, "handled by the server directly because they require access to the desktop window system" (class doc).
- JIPipeDesktopProjectWindow: newProject :405 (getDefaultTemplateProject), newProjectFromTemplate(Template) :432 (PROMPTS load-with-data vs pipeline-only for zipped templates with data), openProject(Path, forceCurrentWindow) :520 (PROMPTS: crate.zip import, jipx sibling), openProjectInThisOrNewWindow :1140, saveProjectAs :750 (format prompt unless isLegacyCompatible forces jipx), saveProjectAsJipx :831 (NON-MODAL, per-project JIPipeQueuedRunnableExecutor saveQueue :98 / getOrCreateSaveQueue :1060), projectSavePath field, saveProjectAndCacheToDirectory :1651.
- JIPipeProjectSaveTarget: desktop/app/project/JIPipeProjectSaveTarget.java (isLegacyCompatible, shouldPrompt).
- Allowed roots: AutomationOperationUtils.allowedRoots (export_data precedent).
- Window resolution precedent: UiStateOperations.findProjectWindowByKey / resolveDefaultProjectWindow (T2).

## Task 1 (single implementer): the four operations + helper extraction
Brief: .superpowers/sdd/2026-11-17-wi1379-lifecycle/task-lifecycle-brief.md
