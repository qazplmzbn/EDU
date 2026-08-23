package com.xyz.question_bank_management_system;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ModulePackageStructureTest {

    private static final String MODULE_PACKAGE_PATH =
            "com/xyz/question_bank_management_system/modules";
    private static final String SERVICE_ANNOTATION =
            "org.springframework.stereotype.Service";

    private final PathMatchingResourcePatternResolver resolver =
            new PathMatchingResourcePatternResolver();
    private final CachingMetadataReaderFactory metadataReaderFactory =
            new CachingMetadataReaderFactory(resolver);

    @Test
    void serviceRootContainsOnlyInterfaces() throws IOException {
        Resource[] resources = resolver.getResources(
                "classpath*:" + MODULE_PACKAGE_PATH + "/*/service/*.class"
        );

        for (Resource resource : resources) {
            MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
            String className = metadataReader.getClassMetadata().getClassName();
            if (className.contains("$")) {
                continue;
            }
            assertTrue(
                    metadataReader.getClassMetadata().isInterface(),
                    () -> className + " must be an interface because it is in a service root package"
            );
        }
    }

    @Test
    void serviceImplementationsLiveUnderImplPackages() throws IOException {
        Resource[] resources = resolver.getResources(
                "classpath*:" + MODULE_PACKAGE_PATH + "/**/*.class"
        );

        for (Resource resource : resources) {
            MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
            String className = metadataReader.getClassMetadata().getClassName();
            if (metadataReader.getAnnotationMetadata().hasAnnotation(SERVICE_ANNOTATION)) {
                assertTrue(
                        className.contains(".service.impl."),
                        () -> className + " must be located in a service.impl package"
                );
            }
        }
    }
}
