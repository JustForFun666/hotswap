bootstrap:
	mvn archetype:generate \
	-DgroupId=org.inxar \
	-DartifactId=hotswap \
	-DversionId=0.8.0 \
	-DarchetypeArtifactId=maven-archetype-quickstart \
	-DinteractiveMode=false

deps:
	mvn dependency:build-classpath -Dmdep.outputFile=target/deps.classpath

example:
	bin/example
