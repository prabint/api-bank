package api.bank.unit.tests

import api.bank.models.Result
import api.bank.models.SchemaType
import api.bank.utils.FileManager
import io.mockk.mockk
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import kotlin.io.path.createTempFile
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileManagerTest {
    private val json = Json

    private val fileManager = FileManager(
        json = json,
        project = mockk(relaxed = true),
        logger = mockk(relaxed = true)
    )

    @Test
    fun testRequestFileMigration() {
        val v0 = readTestResource("migration/api_detail_persistent_v0.json")
        val expected = readTestResource("migration/api_detail_persistent_v1.json")
        val file = createTempFile().toFile().apply { writeText(v0) }
        fileManager.migrate(file)
        val expectedElement = json.parseToJsonElement(expected)
        val actualElement = json.parseToJsonElement(file.readText())
        assertEquals(expectedElement, actualElement)
    }

    @Test
    fun testVariableFileMigration() {
        val v0 = readTestResource("migration/variable_collection_persistent_v0.json")
        val expected = readTestResource("migration/variable_collection_persistent_v1.json")
        val file = createTempFile().toFile().apply { writeText(v0) }
        fileManager.migrate(file)
        val expectedElement = json.parseToJsonElement(expected)
        val actualElement = json.parseToJsonElement(file.readText())
        assertEquals(expectedElement, actualElement)
    }

    @Test
    fun testGetJsonSchemaVersionV0() {
        val array = buildJsonArray { }
        assertEquals(0, fileManager.getJsonSchemaVersion(array))
    }

    @Test
    fun testGetJsonSchemaVersionV1() {
        val obj = buildJsonObject {
            put(FileManager.JSON_KEY_SCHEMA_VERSION, 1)
        }
        assertEquals(1, fileManager.getJsonSchemaVersion(obj))
    }

    @Test
    fun testMigrate0to1Structure() {
        val v0Requests = buildJsonArray {
            add(buildJsonObject { put("key", "value") })
        }
        val result = fileManager.migrate0to1(v0Requests, SchemaType.REQUESTS)

        assertTrue(result is Result.Success)
        val migrated = result.data as JsonObject
        assertEquals(1, migrated[FileManager.JSON_KEY_SCHEMA_VERSION]?.jsonPrimitive?.int)
        assertEquals(SchemaType.REQUESTS.type, migrated[FileManager.JSON_KEY_SCHEMA_TYPE]?.jsonPrimitive?.content)
        assertEquals(v0Requests, migrated[FileManager.JSON_KEY_DATA])
    }

    @Test
    fun testDetectSchemaTypeV0Requests() {
        val v0Requests = buildJsonArray {
            add(buildJsonObject {
                put("groupName", "Test Group")
                put("requests", buildJsonArray { })
            })
        }
        assertEquals(SchemaType.REQUESTS, fileManager.getJsonSchemaType(v0Requests))
    }

    @Test
    fun testDetectSchemaTypeV0Variables() {
        val v0Variables = buildJsonArray {
            add(buildJsonObject {
                put("id", "1")
                put("name", "Test")
                put("variableItems", buildJsonArray { })
                put("isActive", true)
            })
        }
        assertEquals(SchemaType.VARIABLES, fileManager.getJsonSchemaType(v0Variables))
    }

    @Test
    fun testNewFileCreation() {
        val tempDir = System.getProperty("java.io.tmpdir")
        val path = "$tempDir/new_bank_file_${System.currentTimeMillis()}.json"
        val file = fileManager.getJsonFile(path)

        assertTrue(file.exists())
        val content = json.parseToJsonElement(file.readText()) as JsonObject
        assertEquals(
            FileManager.SUPPORTED_SCHEMA_VERSION,
            content[FileManager.JSON_KEY_SCHEMA_VERSION]?.jsonPrimitive?.int
        )

        // Cleanup
        file.delete()
    }

    private fun readTestResource(path: String): String =
        Thread.currentThread()
            .contextClassLoader
            .getResourceAsStream(path)
            ?.bufferedReader()
            ?.readText()
            ?: error("Resource not found: $path")
}