package com.eternalcode.minions.database;

import com.google.gson.Gson;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

public class MinionsLibraryLoader implements PluginLoader {

    private static final String LIBRARIES_RESOURCE = "/paper-libraries.json";
    private static final String REPOSITORY_TYPE = "default";

    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        PluginLibraries libraries = loadLibraries();

        // Repositories first and in file order: Paper tries them in the order they are added, and
        // the generated list puts the primary Maven Central mirror ahead of the fallback proxy.
        libraries.asRepositories().forEach(resolver::addRepository);
        libraries.asDependencies().forEach(resolver::addDependency);

        classpathBuilder.addLibrary(resolver);
    }

    private PluginLibraries loadLibraries() {
        try (InputStream resource = MinionsLibraryLoader.class.getResourceAsStream(LIBRARIES_RESOURCE)) {
            if (resource == null) {
                throw new IllegalStateException(
                        "Missing bundled resource " + LIBRARIES_RESOURCE + "; the build must enable generateLibrariesJson");
            }

            return new Gson().fromJson(new InputStreamReader(resource, StandardCharsets.UTF_8), PluginLibraries.class);
        }
        catch (IOException exception) {
            throw new UncheckedIOException("Failed to read " + LIBRARIES_RESOURCE, exception);
        }
    }

    private record PluginLibraries(Map<String, String> repositories, List<String> dependencies) {

        Stream<Dependency> asDependencies() {
            return this.dependencies.stream()
                    .map(dependency -> new Dependency(new DefaultArtifact(dependency), null));
        }

        Stream<RemoteRepository> asRepositories() {
            return this.repositories.entrySet().stream()
                    .map(repository -> new RemoteRepository.Builder(
                            repository.getKey(), REPOSITORY_TYPE, repository.getValue()).build());
        }
    }
}
