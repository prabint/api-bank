package api.bank.unit.tests

import api.bank.utils.FileManager
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.io.path.createTempFile
import kotlin.test.assertEquals

class MigrationTest {
    private val json = Json

    private val fileManager = FileManager(
        json = json,
        project = mockk(relaxed = true),
        logger = mockk(relaxed = true)
    )

    @Test
    fun testRequestFileMigration() {
        val v0 = readTestResource("migration/api_detail_persistent_v0.json")
        val expected = readTestResource("migration/api_detail_persistent_final.json")
        val file = createTempFile().toFile().apply { writeText(v0) }
        fileManager.migrate(file)
        val expectedElement = json.parseToJsonElement(expected)
        val actualElement = json.parseToJsonElement(file.readText())
        assertEquals(expectedElement, actualElement)
    }

    @Test
    fun testVariableFileMigration() {
        val v0 = readTestResource("migration/variable_collection_persistent_v0.json")
        val expected = readTestResource("migration/variable_collection_persistent_final.json")
        val file = createTempFile().toFile().apply { writeText(v0) }
        fileManager.migrate(file)
        val expectedElement = json.parseToJsonElement(expected)
        val actualElement = json.parseToJsonElement(file.readText())
        assertEquals(expectedElement, actualElement)
    }

    private fun readTestResource(path: String): String =
        Thread.currentThread()
            .contextClassLoader
            .getResourceAsStream(path)
            ?.bufferedReader()
            ?.readText()
            ?: error("Resource not found: $path")
}