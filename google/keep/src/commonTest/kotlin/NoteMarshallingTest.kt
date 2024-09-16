import kotlinx.serialization.json.Json
import net.opatry.google.keep.entity.Note
import kotlin.test.Test
import kotlin.test.assertEquals

class NoteMarshallingTest {
    @Test
    fun foo() {
        // unmarshall the following JSON string into a Note object with kotlinx-serialization

        val json =  """
            {
              "kind": "keep#note",
              "id": "note-id-123",
              "title": "Shopping List",
              "textContent": "Buy milk, eggs, and bread.",
              "createdTimestamp": "2023-09-12T10:00:00Z",
              "updatedTimestamp": "2023-09-12T11:00:00Z",
              "permissions": {
                "kind": "keep#permission",
                "role": "writer",
                "type": "user",
                "emailAddress": "example.user@gmail.com",
                "isDeleted": false
              }
            }
        """.trimIndent()

         val note = Json.decodeFromString<Note>(json)
        assertEquals("note-id-123", note.name)
        assertEquals("Shopping List", note.title)
        // TODO create instant from y, m, d, h, m
//        assertEquals(Instant., note.createTime)
//        assertEquals(note.permissions.)
    }
}