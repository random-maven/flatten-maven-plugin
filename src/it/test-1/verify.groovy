/**
 * original model is as expected
 */

File originalPom = new File( basedir, 'pom.xml' )
assert originalPom.exists()

def originalProject = new XmlSlurper().parse( originalPom )

assert '4.0.0' ==  originalProject.modelVersion.text()
assert 'com.carrotgarden.maven' == originalProject.groupId.text()
assert 'flatten-maven-plugin-test-1' == originalProject.artifactId.text()
assert '${revision}' == originalProject.version.text()

assert 1 == originalProject.url.size()
assert 1 == originalProject.build.size()
assert 1 == originalProject.profiles.size()
assert 2 == originalProject.dependencies.dependency.size()

/**
 * flattened model is transformed
 */

File flattenedPom = new File( basedir, 'target/flatten/pom.xml.flatten' )
assert flattenedPom.exists()

def flattendProject = new XmlSlurper().parse( flattenedPom )

assert '4.0.0' ==  flattendProject.modelVersion.text()
assert 'com.carrotgarden.maven' == flattendProject.groupId.text()
assert 'flatten-maven-plugin-test-1' == flattendProject.artifactId.text()
assert '0.0.0' == flattendProject.version.text()

assert 1 == flattendProject.url.size()
assert 0 == flattendProject.build.size()
assert 0 == flattendProject.profiles.size()
assert 1 == flattendProject.dependencies.dependency.size()
