package ru.lewis.battlepass;

import com.google.gson.Gson;
import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@SuppressWarnings("UnstableApiUsage")
public class BattlePassLoader implements PluginLoader {

    private static final String MAVEN_CENTRAL_RAW_URL = "https://repo.maven.apache.org/maven2/";

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();

        PluginLibraries generatedLibraries = loadGeneratedLibraries();
        generatedLibraries.asRepositories().forEach(resolver::addRepository);
        generatedLibraries.asDependencies().forEach(resolver::addDependency);

        addExtraRepositories(resolver);
        addExtraDependencies(resolver);

        classpathBuilder.addLibrary(resolver);
    }

    private PluginLibraries loadGeneratedLibraries() {
        try (InputStream in = getClass().getResourceAsStream("/paper-libraries.json")) {
            if (in == null) {
                throw new IllegalStateException(
                        "paper-libraries.json not found. Did you set generateLibrariesJson = true?"
                );
            }
            PluginLibraries libraries = new Gson().fromJson(
                    new InputStreamReader(in, StandardCharsets.UTF_8),
                    PluginLibraries.class
            );
            return libraries.orEmpty();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addExtraRepositories(MavenLibraryResolver resolver) {
        resolver.addRepository(mavenCentralMirror());
        resolver.addRepository(paper());
        resolver.addRepository(sonatypeSnapshots());
        resolver.addRepository(jitpack());
        resolver.addRepository(xenondevs());
        resolver.addRepository(pandaLang());
    }

    private void addExtraDependencies(MavenLibraryResolver resolver) {
        resolver.addDependency(dependency("xyz.xenondevs.invui:invui:pom:1.49"));
    }

    private Dependency dependency(String coordinates) {
        return new Dependency(new DefaultArtifact(coordinates), null);
    }

    private RemoteRepository mavenCentralMirror() {
        return new RemoteRepository.Builder(
                "central",
                "default",
                MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR
        ).build();
    }

    private RemoteRepository paper() {
        return new RemoteRepository.Builder(
                "papermc",
                "default",
                "https://repo.papermc.io/repository/maven-public/"
        ).build();
    }

    private RemoteRepository sonatypeSnapshots() {
        return new RemoteRepository.Builder(
                "sonatype",
                "default",
                "https://s01.oss.sonatype.org/content/repositories/snapshots/"
        ).build();
    }

    private RemoteRepository jitpack() {
        return new RemoteRepository.Builder(
                "jitpack",
                "default",
                "https://jitpack.io"
        ).build();
    }

    private RemoteRepository xenondevs() {
        return new RemoteRepository.Builder(
                "xenondevs",
                "default",
                "https://repo.xenondevs.xyz/releases/"
        ).build();
    }

    private RemoteRepository pandaLang() {
        return new RemoteRepository.Builder(
                "panda-lang",
                "default",
                "https://repo.panda-lang.org/releases"
        ).build();
    }

    private record PluginLibraries(Map<String, String> repositories, List<String> dependencies) {

        PluginLibraries orEmpty() {
            return new PluginLibraries(
                    repositories != null ? repositories : Collections.emptyMap(),
                    dependencies != null ? dependencies : Collections.emptyList()
            );
        }

        Stream<Dependency> asDependencies() {
            return dependencies.stream()
                    .map(d -> new Dependency(new DefaultArtifact(d), null));
        }

        Stream<RemoteRepository> asRepositories() {
            return repositories.entrySet().stream()
                    .filter(e -> !e.getValue().startsWith(MAVEN_CENTRAL_RAW_URL))
                    .map(e -> new RemoteRepository.Builder(e.getKey(), "default", e.getValue()).build());
        }
    }
}
