package dev.kiki.openapi.ktor

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.servers.Server
import org.openapitools.codegen.CodegenConstants
import org.openapitools.codegen.CodegenOperation
import org.openapitools.codegen.CodegenType
import org.openapitools.codegen.config.GlobalSettings
import org.openapitools.codegen.languages.AbstractKotlinCodegen

class Ktor3Codegen : AbstractKotlinCodegen() {
    init {
        artifactId = "kotlin-ktor-client"
        packageName = "dev.kiki.openapi.generated"
        apiPackage = "$packageName.api"
        modelPackage = "$packageName.model"
        embeddedTemplateDir = "kotlin-ktor3"

        typeMapping["array"] = "kotlin.collections.List"
        typeMapping["number"] = "kotlin.Double"
        typeMapping["integer"] = "kotlin.Int"
        typeMapping["int64"] = "kotlin.Long"
        typeMapping["date"] = "kotlin.String"
        typeMapping["date-time"] = "kotlin.String"
        typeMapping["object"] = "JsonElement"
        typeMapping["AnyType"] = "JsonElement"
        importMapping["JsonElement"] = "kotlinx.serialization.json.JsonElement"

        modelTemplateFiles["model.mustache"] = ".kt"
        apiTemplateFiles["api.mustache"] = ".kt"
        GlobalSettings.setProperty(CodegenConstants.SKIP_FORM_MODEL, "false")
    }

    override fun getTag(): CodegenType = CodegenType.CLIENT

    override fun getName(): String = "peterqin-kotlin-ktor"

    override fun getHelp(): String = "Generates Kotlin serializable models and Ktor suspend APIs."

    override fun processOpts() {
        super.processOpts()
        additionalProperties["useHilt"] = additionalProperties["useHilt"]?.toString()?.toBooleanStrictOrNull() ?: true
    }

    override fun preprocessOpenAPI(openAPI: OpenAPI) {
        super.preprocessOpenAPI(openAPI)
        val tags = (additionalProperties["openApiKtorIncludeTags"] as? String)
            ?.split(',')?.filter(String::isNotBlank)?.toSet().orEmpty()
        if (tags.isNotEmpty()) filterPaths(openAPI.paths, tags)
    }

    override fun fromOperation(
        path: String,
        httpMethod: String,
        operation: Operation?,
        servers: List<Server>?,
    ): CodegenOperation = super.fromOperation(path, httpMethod, operation, servers).also { generated ->
        generated.httpMethod = generated.httpMethod.lowercase().replaceFirstChar { it.uppercase() }
        generated.path = generated.path.removePrefix("/").replace(Regex("\\{([^}]+)}")) { match ->
            "\$${match.groupValues[1]}"
        }
        generated.responses.firstOrNull { it.is2xx }
            ?.also { generated.vendorExtensions["x-successResponse"] = it }
    }

    private fun filterPaths(paths: Paths?, tags: Set<String>) {
        fun keep(operation: Operation?): Boolean =
            operation?.tags.orEmpty().any(tags::contains)

        paths?.forEach { (_, item) ->
            if (!keep(item.get)) item.get = null
            if (!keep(item.post)) item.post = null
            if (!keep(item.put)) item.put = null
            if (!keep(item.delete)) item.delete = null
            if (!keep(item.patch)) item.patch = null
        }
    }
}
