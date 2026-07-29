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

    override fun preprocessOpenAPI(openAPI: OpenAPI) {
        super.preprocessOpenAPI(openAPI)
        val tags = (additionalProperties["includeTags"] as? String)
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
        paths?.forEach { (_, item) ->
            if (item.get?.tags?.none(tags::contains) != false) item.get = null
            if (item.post?.tags?.none(tags::contains) != false) item.post = null
            if (item.put?.tags?.none(tags::contains) != false) item.put = null
            if (item.delete?.tags?.none(tags::contains) != false) item.delete = null
            if (item.patch?.tags?.none(tags::contains) != false) item.patch = null
        }
    }
}
