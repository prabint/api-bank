package api.bank.unit.tests

import api.bank.multitab.SettingsTab
import api.bank.settings.ApiBankSettingsPersistentStateComponent
import api.bank.settings.ApiBankSettingsState
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsTabTest {
    private val gson = Gson()
    private val project: Project = mock()
    private val logger: Logger = mock()
    private val persistentStateComponent: ApiBankSettingsPersistentStateComponent = mock()
    private lateinit var settingsTab: SettingsTab

    @BeforeEach
    fun setup() {
        whenever(persistentStateComponent.state)
            .thenReturn(ApiBankSettingsState("/path", "path2"))

        settingsTab = SettingsTab(
            project = project,
            gson = gson,
            logger = logger,
            settings = persistentStateComponent,
            onApply = {}
        )
    }

    @Test
    fun `isValidEnvVarFile returns success for empty JSON`() {
        val validJson = "[]"
        val result = settingsTab.isValidEnvVarFile(validJson)
        assertTrue(result.success)
        assertEquals("", result.message)
    }

    @Test
    fun `isValidEnvVarFile returns success for valid JSON`() {
        val validJson = """
            [
              {
                "id": "76be8f81-5a18-4e00-9b27-3a45167d4b7a",
                "name": "Staging",
                "variableItems": [
                  [
                    "base_url",
                    "https://jsonplaceholder.typicode.com"
                  ]
                ],
                "isActive": true
              },
              {
                "id": "4dad5c07-f062-408f-8879-a0c9331cd08d",
                "name": "Production",
                "variableItems": [
                  [
                    "base_url",
                    "https://jsonplaceholder.typicode.com"
                  ]
                ],
                "isActive": false
              }
            ]
        """.trimIndent()

        val result = settingsTab.isValidEnvVarFile(validJson)
        assertTrue(result.success)
        assertEquals("", result.message)
    }

    @Test
    fun `isValidEnvVarFile returns failure for missing id field`() {
        val invalidJson = """
            [
              {
                "name": "Staging",
                "variableItems": [
                  [
                    "base_url",
                    "https://jsonplaceholder.typicode.com"
                  ]
                ],
                "isActive": true
              },
              {
                "id": "4dad5c07-f062-408f-8879-a0c9331cd08d",
                "name": "Production",
                "variableItems": [
                  [
                    "base_url",
                    "https://jsonplaceholder.typicode.com"
                  ]
                ],
                "isActive": false
              }
            ]
        """.trimIndent()

        val result = settingsTab.isValidEnvVarFile(invalidJson)
        assertFalse(result.success)
        assertTrue(result.message.contains("missing id"))
    }

    @Test
    fun `isValidEnvVarFile returns failure for missing name field`() {
        val invalidJson = """
            [
              {
                "id": "76be8f81-5a18-4e00-9b27-3a45167d4b7a",
                "name": "Staging",
                "variableItems": [
                  [
                    "base_url",
                    "https://jsonplaceholder.typicode.com"
                  ]
                ],
                "isActive": true
              },
              {
                "id": "4dad5c07-f062-408f-8879-a0c9331cd08d",
                "variableItems": [
                  [
                    "base_url",
                    "https://jsonplaceholder.typicode.com"
                  ]
                ],
                "isActive": false
              }
            ]
        """.trimIndent()

        val result = settingsTab.isValidEnvVarFile(invalidJson)
        assertFalse(result.success)
    }

    @Test
    fun `isValidEnvVarFile returns failure for missing variableItems field`() {
        val invalidJson = """
            [
              {
                "id": "76be8f81-5a18-4e00-9b27-3a45167d4b7a",
                "name": "Staging",
                "isActive": true
              },
              {
                "id": "4dad5c07-f062-408f-8879-a0c9331cd08d",
                "name": "Production",
                "variableItems": [
                  [
                    "base_url",
                    "https://jsonplaceholder.typicode.com"
                  ]
                ],
                "isActive": false
              }
            ]
        """.trimIndent()

        val result = settingsTab.isValidEnvVarFile(invalidJson)
        assertFalse(result.success)
    }

    @Test
    fun `isValidEnvVarFile returns failure for missing isActive field`() {
        val invalidJson = """
            [
              {
                "id": "76be8f81-5a18-4e00-9b27-3a45167d4b7a",
                "name": "Staging",
                "variableItems": [
                  [
                    "base_url",
                    "https://jsonplaceholder.typicode.com"
                  ]
                ]
              },
              {
                "id": "4dad5c07-f062-408f-8879-a0c9331cd08d",
                "name": "Production",
                "variableItems": [
                  [
                    "base_url",
                    "https://jsonplaceholder.typicode.com"
                  ]
                ],
                "isActive": false
              }
            ]
        """.trimIndent()

        val result = settingsTab.isValidEnvVarFile(invalidJson)
        assertFalse(result.success)
    }

    @Test
    fun `isValidEnvVarFile returns failure for parsing fails`() {
        val invalidJson = """
            [
              {
                "id": "76be8f81-5a18-4e00-9b27-3a45167d4b7a",
                "name": "Staging",
                "variableItems": [
                  [
                    "base_url",
                    "https://jsonplaceholder.typicode.com"
                  ]
                ],
                "isActive": true
              },
              {
                "id": "4dad5c07-f062-408f-8879-a0c9331cd08d",
                "name": "Production",
                "variableItems": [
                  [
                    "base_url",
                    "https://jsonplaceholder.typicode.com"
                  ]
                ],
                "isActive": false
              }
            ] }
        """.trimIndent()

        val result = settingsTab.isValidEnvVarFile(invalidJson)
        assertFalse(result.success)
    }

    @Test
    fun `isValidRequestGroupFile returns success for empty JSON`() {
        val validJson = "[]"
        val result = settingsTab.isValidRequestGroupFile(validJson)
        assertTrue(result.success)
        assertEquals("", result.message)
    }

    @Test
    fun `isValidRequestGroupFile returns success for valid JSON`() {
        val validJson = """
                [
                  {
                    "groupName": "Group 1",
                    "requests": [
                      {
                        "id": "610ae987-e8d9-4091-9242-d3c4b540f89e",
                        "name": "Get Post",
                        "url": "${'$'}{{base_url}}/todos/1",
                        "method": "GET",
                        "header": [
                          [
                            "Content-Type",
                            "application/json"
                          ]
                        ],
                        "body": ""
                      }
                    ]
                  },
                  {
                    "groupName": "Group 2",
                    "requests": [
                      {
                        "id": "d5a40903-34e2-47ab-bc4e-2622fe9858f3",
                        "name": "Create Post",
                        "url": "${'$'}{{base_url}}/posts",
                        "method": "POST",
                        "header": [
                          [
                            "Content-Type",
                            "application/json"
                          ]
                        ],
                        "body": "{\n  \"userId\": 1,\n  \"id\": 1,\n  \"title\": \"delectus aut autem\",\n  \"completed\": false\n}"
                      },
                      {
                        "id": "9b485002-74d4-4617-949d-37f69adba5ef",
                        "name": "Remove",
                        "url": "${'$'}{{base_url}}/posts/1",
                        "method": "DELETE",
                        "header": [
                          [
                            "Content-Type",
                            "application/json"
                          ]
                        ],
                        "body": ""
                      }
                    ]
                  }
                ]
        """.trimIndent()

        val result = settingsTab.isValidRequestGroupFile(validJson)
        assertTrue(result.success)
        assertEquals("", result.message)
    }

    @Test
    fun `isValidRequestGroupFile returns failure for missing groupName field`() {
        val invalidJson = """
                [
                  {
                    "requests": [
                      {
                        "id": "610ae987-e8d9-4091-9242-d3c4b540f89e",
                        "name": "Get Post",
                        "url": "${'$'}{{base_url}}/todos/1",
                        "method": "GET",
                        "header": [
                          [
                            "Content-Type",
                            "application/json"
                          ]
                        ],
                        "body": ""
                      }
                    ]
                  },
                  {
                    "groupName": "Group 2",
                    "requests": [
                      {
                        "id": "d5a40903-34e2-47ab-bc4e-2622fe9858f3",
                        "name": "Create Post",
                        "url": "${'$'}{{base_url}}/posts",
                        "method": "POST",
                        "header": [
                          [
                            "Content-Type",
                            "application/json"
                          ]
                        ],
                        "body": "{\n  \"userId\": 1,\n  \"id\": 1,\n  \"title\": \"delectus aut autem\",\n  \"completed\": false\n}"
                      },
                      {
                        "id": "9b485002-74d4-4617-949d-37f69adba5ef",
                        "name": "Remove",
                        "url": "${'$'}{{base_url}}/posts/1",
                        "method": "DELETE",
                        "header": [
                          [
                            "Content-Type",
                            "application/json"
                          ]
                        ],
                        "body": ""
                      }
                    ]
                  }
                ]
        """.trimIndent()

        val result = settingsTab.isValidRequestGroupFile(invalidJson)
        assertFalse(result.success)
    }

    @Test
    fun `isValidRequestGroupFile returns failure for missing requests field`() {
        val invalidJson = """
                [
                  {
                    "groupName": "Group 1"
                  },
                  {
                    "groupName": "Group 2",
                    "requests": [
                      {
                        "id": "d5a40903-34e2-47ab-bc4e-2622fe9858f3",
                        "name": "Create Post",
                        "url": "${'$'}{{base_url}}/posts",
                        "method": "POST",
                        "header": [
                          [
                            "Content-Type",
                            "application/json"
                          ]
                        ],
                        "body": "{\n  \"userId\": 1,\n  \"id\": 1,\n  \"title\": \"delectus aut autem\",\n  \"completed\": false\n}"
                      },
                      {
                        "id": "9b485002-74d4-4617-949d-37f69adba5ef",
                        "name": "Remove",
                        "url": "${'$'}{{base_url}}/posts/1",
                        "method": "DELETE",
                        "header": [
                          [
                            "Content-Type",
                            "application/json"
                          ]
                        ],
                        "body": ""
                      }
                    ]
                  }
                ]
        """.trimIndent()

        val result = settingsTab.isValidRequestGroupFile(invalidJson)
        assertFalse(result.success)
    }

    @Test
    fun `isValidRequestGroupFile returns failure for parsing fails`() {
        val invalidJson = """
                [
                    "groupName": "Group 1"
                  },
                  {
                    "groupName": "Group 2",
                    "requests": [
                      {
                        "id": "d5a40903-34e2-47ab-bc4e-2622fe9858f3",
                        "name": "Create Post",
                        "url": "${'$'}{{base_url}}/posts",
                        "method": "POST",
                        "header": [
                          [
                            "Content-Type",
                            "application/json"
                          ]
                        ],
                        "body": "{\n  \"userId\": 1,\n  \"id\": 1,\n  \"title\": \"delectus aut autem\",\n  \"completed\": false\n}"
                      },
                      {
                        "id": "9b485002-74d4-4617-949d-37f69adba5ef",
                        "name": "Remove",
                        "url": "${'$'}{{base_url}}/posts/1",
                        "method": "DELETE",
                        "header": [
                          [
                            "Content-Type",
                            "application/json"
                          ]
                        ],
                        "body": ""
                      }
                    ]
                  }
                ]
        """.trimIndent()

        val result = settingsTab.isValidRequestGroupFile(invalidJson)
        assertFalse(result.success)
    }
}