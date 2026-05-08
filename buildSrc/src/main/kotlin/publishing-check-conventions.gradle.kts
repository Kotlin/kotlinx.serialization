object TestPublishing {
    const val configurationName = "testRepository"
}

val testRepositoryDependency = configurations.dependencyScope(TestPublishing.configurationName)


val testRepositories = configurations.resolvable("testRepositories") {
    attributes {
        attribute(Attribute.of("kotlinx.serialization.repository", String::class.java), "test")
        attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>("repo-testing"))
    }
    extendsFrom(testRepositoryDependency.get())
}
