package com.thisaay.domainer

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import java.io.OutputStream


@OptIn(KspExperimental::class)
class DomainModelProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {

    operator fun OutputStream.plusAssign(str: String) {
        this.write(str.toByteArray())
    }

    inner class Visitor(val file: OutputStream) : KSVisitorVoid() {

        @OptIn(KotlinPoetKspPreview::class)
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            if (classDeclaration.classKind != ClassKind.CLASS && classDeclaration.classKind != ClassKind.ENUM_CLASS) {
                logger.error("Only Classes can be annotated with @DomainModel", classDeclaration)
                return
            }

            val annotation: KSAnnotation = classDeclaration.annotations.first {
                it.shortName.asString() == "DomainModel"
            }

            val classArgument: KSValueArgument = annotation.arguments
                .first { arg -> arg.name?.asString() == "clazz" }

            val domainType = classArgument.value as KSType



            if (classDeclaration.classKind == ClassKind.ENUM_CLASS) {
                file += "fun ${classDeclaration.qualifiedName?.asString()}.toModel(): ${domainType.declaration.qualifiedName?.asString()} "
                file += " = when(this) {\n"
                val domainEnums =
                    (domainType.declaration as KSClassDeclaration).declarations.filter { it !is KSFunctionDeclaration }

                 classDeclaration.declarations.forEach {
                    if(it !is KSFunctionDeclaration){
                        file += "    ${it.qualifiedName?.asString()} -> ${getEnumDomainName(it,domainEnums.toList())}\n"
                    }
                }
                file += "}\n\n"

                file += "fun ${domainType.declaration.qualifiedName?.asString()}.toData(): ${classDeclaration.qualifiedName?.asString()} "
                file += " = when(this) {\n"

                classDeclaration.declarations.forEach {
                    if(it !is KSFunctionDeclaration){
                        file += "    ${getEnumDomainName(it,domainEnums.toList())} -> ${it.qualifiedName?.asString()} \n"
                    }
                }

                file += "}\n\n"

            } else {
                val dataProps = classDeclaration.getAllProperties()
                file += "fun ${classDeclaration.qualifiedName?.asString()}.toModel(): ${domainType.declaration.qualifiedName?.asString()} = "
                file += "${domainType.declaration.qualifiedName?.asString()}(\n"
                dataProps.forEach {
                    when {
                        it.isEnumOrdinal() ->it.writeAsEnum(file)
                        it.hasDomainModel() -> it.writeWithToModel(file)
                        else -> it.writeAsItIs(file)
                    }
                }
                file += ")\n\n"

                file += "fun ${domainType.declaration.qualifiedName?.asString()}.toData(): ${classDeclaration.qualifiedName?.asString()}\n = "
                file += "${classDeclaration.qualifiedName?.asString()}(\n"
                dataProps.forEach {
                    when {
                        it.isEnumOrdinal() ->it.writeAsEnum(file,true)
                        it.hasDomainModel() -> it.writeWithToModel(file,true)
                        else -> it.writeAsItIs(file,true)
                    }
                }
                file += ")\n\n"
            }


        }


    }

    private fun getEnumDomainName(it: KSDeclaration,domainConstants: List<KSDeclaration>): String? {
        val name = it.getDomainName()?: it.simpleName.asString()

        return domainConstants.firstOrNull {
            it.simpleName.asString() == name
        }?.qualifiedName?.asString()
    }

    private fun KSPropertyDeclaration.hasDomainModel(): Boolean {
        return type.resolve().declaration.isAnnotationPresent(DomainModel::class)
    }

    private fun KSPropertyDeclaration.isEnumOrdinal(): Boolean {
        return isAnnotationPresent(EnumOrdinal::class)
    }

    private fun KSPropertyDeclaration.writeWithToModel(file: OutputStream, isData: Boolean = false) {
        val propName = simpleName.asString()
        val domainName = getDomainName()
        if(isData){
            file += "    $propName = ${domainName ?: propName}.toData(),\n"
        }else{
            file += "    ${domainName ?: propName} = ${propName}.toModel(),\n"
        }
    }

    private fun KSPropertyDeclaration.writeAsItIs(file: OutputStream,isData: Boolean = false) {
        val propName = simpleName.asString()
        val domainName = getDomainName()
        if(isData){
            file += "    $propName = ${domainName ?: propName},\n"

        }else{
            file += "    ${domainName ?: propName} = ${propName},\n"
        }
    }

    private fun KSPropertyDeclaration.writeAsEnum(file: OutputStream,isData: Boolean = false) {
        val enumClass = annotations.first { it.shortName.asString() == "EnumOrdinal" }
            .arguments.first { it.name?.asString() == "clazz" }.value as KSType
        val propName = simpleName.asString()
        val domainName = getDomainName()

        if(isData){
            file += "    $propName = ${domainName ?: propName}.toData().ordinal,\n"
        }else{
            file += "    ${domainName ?: propName} = ${enumClass.declaration.qualifiedName?.asString()}.values()[$propName].toModel(),\n"
        }

    }

    private fun KSDeclaration.getDomainName(): String? {
        return annotations
            .firstOrNull { it.shortName.asString() == "DomainName" }
            ?.arguments?.first { it.name?.asString() == "name" }?.value as? String
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation("com.thisaay.domainer.DomainModel")
            .filterIsInstance<KSClassDeclaration>()

        if (!symbols.iterator().hasNext()) return emptyList()

        val file: OutputStream = codeGenerator.createNewFile(
            dependencies = Dependencies(false, *resolver.getAllFiles().toList().toTypedArray()),
            packageName = "com.thisaay.domainer",
            fileName = "GeneratedDomainMappers"
        )
        file += "package com.thisaay.domainer\n\n"

        symbols.forEach { it.accept(Visitor(file), Unit) }

        file.close()
        val unableToProcess = symbols.filterNot { it.validate() }.toList()
        return unableToProcess
    }

}


//@AutoService(Processor::class) // For registering the service
//@SupportedSourceVersion(SourceVersion.RELEASE_8) // to support Java 8
//@SupportedOptions(DomainModelProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
//class DomainModelProcessor : AbstractProcessor() {
//
//    val generatedSourcesRoot: String =
//        processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME].orEmpty()
//
//    @OptIn(KotlinPoetMetadataPreview::class)
//    override fun process(p0: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
//        roundEnv?.getElementsAnnotatedWith(DomainModel::class.java)?.forEach { element ->
//            if (generatedSourcesRoot.isEmpty()) {
//                return false
//            }
//
//            if (element.kind != ElementKind.CLASS) {
//                return false
//            }
//
//            val typeMetadata = element.getAnnotation(Metadata::class.java)
//            val kmClass = typeMetadata.toImmutableKmClass()
//            val className = ClassInspectorUtil.createClassName(kmClass.name)
//
//            val domainClass = element.getAnnotation(DomainModel::class.java).clazz
//
//            val funcBuilder = FunSpec.builder("toModel")
//                .addModifiers(KModifier.PUBLIC)
//                .receiver(className)
//                .returns(domainClass)
//
//            val file = File("src/main/java/generated").apply { mkdir() }
//            FileSpec.builder(getPackage(element).toString(), "DomainModelMappersGenerated")
//                .addFunction(funcBuilder.build())
//                .build()
//                .writeTo(file)
//        }
//
//        return true
//    }
//
//    companion object {
//        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
//    }
//}
