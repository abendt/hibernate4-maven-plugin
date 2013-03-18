package de.akquinet.maven.hibernate4;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;

import java.io.File;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.List;

/**
 * use Hibernate SchemaExport to generate DDL drop/create scripts.
 *
 * @author Alphonse Bendt
 * @author http://doingenterprise.blogspot.de/2012/05/schema-generation-with-hibernate-4-jpa.html
 */
@Mojo(name = "generate-ddl", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GenerateDdlMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;

    /**
     * Set the end of statement delimiter.
     */
    @Parameter(property = "hibernate4.delimiter", defaultValue = ";")
    private String delimiter;

    /**
     * The name of the file to which to write the create script.
     */
    @Parameter(property = "hibernate4.createFilename", defaultValue = "create.sql")
    private String createScriptFileName;

    /**
     * The name of the file to which to write the drop script.
     */
    @Parameter(property = "hibernate4.dropFilename", defaultValue = "drop.sql")
    private String dropScriptFileName;

    /**
     * Name of the Persistence Unit that should be processed.
     */
    @Parameter(property = "hibernate4.persistenceUnitName", required = true)
    private String persistenceUnitName;

    /**
     * generate the create script.
     */
    @Parameter(property = "hibernate4.generateCreateDdl", defaultValue = "true")
    private boolean generateCreateScript;

    /**
     * generate the drop script.
     */
    @Parameter(property = "hibernate4.generateDropDdl", defaultValue = "true")
    private boolean generateDropScript;

    /**
     * the encoding used to write the files.
     */
    @Parameter(property = "hibernate4.encoding", defaultValue = "UTF-8")
    private String encoding;

    /**
     * Specify where to place generated source files created by schema export.
     */
    @Parameter(property = "hibernate4.outputDirectory", defaultValue = "${project.build.directory}/generated-resources/ddl")
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        final ClassLoader old = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(getClassloader(old));

            Formatter formatter = FormatStyle.DDL.getFormatter();

            Ejb3Configuration jpaConfiguration = new Ejb3Configuration().configure(persistenceUnitName, null);
            Configuration hibernateConfiguration = jpaConfiguration.getHibernateConfiguration();

            getLog().info("process persistence unit: " + persistenceUnitName);

            String[] createSQL = hibernateConfiguration.generateSchemaCreationScript(
                    Dialect.getDialect(hibernateConfiguration.getProperties()));

            String[] dropSQL = hibernateConfiguration.generateDropSchemaScript(
                    Dialect.getDialect(hibernateConfiguration.getProperties()));

            if (generateCreateScript) {
                export(createScriptFileName, delimiter, formatter, createSQL);
            }

            if (generateDropScript) {
                export(dropScriptFileName, delimiter, formatter, dropSQL);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    private void export(String outFile, String delimiter, Formatter formatter, String[] createSQL) {

        outputDirectory.mkdirs();

        PrintWriter writer = null;
        try {
            File file = new File(outputDirectory, outFile);
            writer = new PrintWriter(file, Charset.forName(encoding).name());
            for (String string : createSQL) {
                writer.print(formatter.format(string) + "\n" + delimiter + "\n");
            }
            getLog().info("wrote: " + file);
        } catch (Exception e) {
            getLog().error(e.toString(), e);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private ClassLoader getClassloader(ClassLoader old) {
        return new URLClassLoader(getCompileClassPath().toArray(new URL[0]), old);
    }

    private List<URL> getCompileClassPath() {
        try {
            return Lists.transform(project.getCompileClasspathElements(), new Function<String, URL>() {
                @Override
                public URL apply(String input) {
                    try {
                        return new File(input).toURI().toURL();
                    } catch (MalformedURLException e) {
                        throw new AssertionError(e);
                    }
                }
            });
        } catch (DependencyResolutionRequiredException e) {
            throw new AssertionError(e);
        }
    }
}
