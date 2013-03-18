hibernate4-maven-plugin
===================

To use the plugin:

You added the following snippet to your _pom_ file:

        <plugins>
            <plugin>
                <groupId>de.akquinet.maven.hibernate4</groupId>
                <artifactId>hibernate4-maven-plugin</artifactId>
                <version>${project.version}</version>

                <executions>
                    <execution>
                        <goals>
                            <goal>generate-ddl</goal>
                        </goals>

                        <configuration>
                            <persistenceUnitName>...</persistenceUnitName>
                        </configuration>
                    </execution>
                </executions>

                <dependencies>
                    <dependency>
                        <groupId>org.hibernate</groupId>
                        <artifactId>hibernate-entitymanager</artifactId>
                        <version>4.x</version>
                    </dependency>

                    <dependency>
                        <groupId>org.hibernate</groupId>
                        <artifactId>hibernate-core</artifactId>
                        <version>4.x</version>
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>

Now you can generate the DDL scripts:

    mvn package

After successful execution the generated files are available in the folder target/generated-resources/ddl