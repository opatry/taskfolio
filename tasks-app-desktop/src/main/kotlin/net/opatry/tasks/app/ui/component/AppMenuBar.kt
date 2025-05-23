/*
 * Copyright (c) 2025 Olivier Patry
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.opatry.tasks.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import net.opatry.tasks.app.resources.Res
import net.opatry.tasks.app.resources.app_menu_edit
import net.opatry.tasks.app.resources.app_menu_edit_new_task
import net.opatry.tasks.app.resources.app_menu_edit_new_task_list
import net.opatry.tasks.app.resources.app_menu_tools
import net.opatry.tasks.app.resources.app_menu_tools_show_network_logs
import net.opatry.tasks.app.util.OS
import net.opatry.tasks.app.util.currentOS
import org.jetbrains.compose.resources.stringResource

@Composable
fun FrameWindowScope.AppMenuBar(
    showDevelopmentTools: Boolean,
    onNewTaskListClick: () -> Unit,
    canCreateTask: Boolean,
    onNewTaskClick: () -> Unit,
    onNetworkLogClick: () -> Unit,
) {
    MenuBar {
        Menu(stringResource(Res.string.app_menu_edit), mnemonic = 'E') {
            Item(
                text = stringResource(Res.string.app_menu_edit_new_task_list),
                shortcut = when (currentOS) {
                    OS.Mac -> KeyShortcut(key = Key.N, meta = true, shift = true)
                    OS.Linux,
                    OS.Windows -> KeyShortcut(key = Key.N, ctrl = true, shift = true)
                },
                onClick = onNewTaskListClick
            )
            Item(
                text = stringResource(Res.string.app_menu_edit_new_task),
                shortcut = when (currentOS) {
                    OS.Mac -> KeyShortcut(key = Key.N, meta = true)
                    OS.Linux,
                    OS.Windows -> KeyShortcut(key = Key.N, ctrl = true)
                },
                enabled = canCreateTask,
                onClick = onNewTaskClick
            )
        }
        if (showDevelopmentTools) {
            Menu(stringResource(Res.string.app_menu_tools), mnemonic = 'T') {
                Item(
                    text = stringResource(Res.string.app_menu_tools_show_network_logs),
                    shortcut = when (currentOS) {
                        OS.Mac -> KeyShortcut(key = Key.L, meta = true, shift = true)
                        OS.Linux,
                        OS.Windows -> KeyShortcut(key = Key.L, ctrl = true, shift = true)
                    },
                    onClick = onNetworkLogClick
                )
            }
        }
    }
}