/*
 * Copyright (c) 2024 Olivier Patry
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

package net.opatry.google.tasks.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
/**
 * Information about the surface (Docs, Chat Spaces) where this task was assigned from.
 */
abstract class SurfaceInfo {
//    @SerialName("driveResourceInfo")
    @Serializable
    /**
     * Information about the [Drive resource](https://developers.google.com/tasks/reference/rest/v1/tasks#driveresourceinfo) where a task was assigned from (the document, sheet, etc.).
     *
     * @property driveFileId Output only. Identifier of the file in the Drive API.
     * @property resourceKey Output only. Resource key required to access files shared via a shared link. Not required for all files. See also developers.google.com/drive/api/guides/resource-keys.
     */
    data class DriveResourceInfo(
        @SerialName("driveFileId")
        val driveFileId: String,
        @SerialName("resourceKey")
        val resourceKey: String,
    ) : SurfaceInfo()

//    @SerialName("spaceInfo")
    @Serializable
    /**
     * Information about the [Chat Space](https://developers.google.com/tasks/reference/rest/v1/tasks#spaceinfo) where a task was assigned from.
     *
     * @property space Output only. The Chat space where this task originates from. The format is "spaces/{space}".
     */
    data class SpaceInfo(
        @SerialName("space")
        val space: String,
    ) : SurfaceInfo()
}
