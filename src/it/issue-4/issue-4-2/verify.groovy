/**
 * original model
 */

File originalPom = new File( basedir, 'pom.xml' )
assert originalPom.exists()

def originalProject = new XmlSlurper().parse( originalPom )
assert 1 == originalProject.dependencies.dependency.size()

def originalDependency = originalProject.dependencies.dependency[0]
assert 0 == originalDependency.exclusions.size()
assert "[0.6,0.7)" == originalDependency.version.text()

/**
 * flattened model
 */

File flattenedPom = new File( basedir, 'target/flatten/pom.xml.flatten' )
assert flattenedPom.exists()

def flattendProject = new XmlSlurper().parse( flattenedPom )
assert 1 == flattendProject.dependencies.dependency.size()

def flattenedDependency = flattendProject.dependencies.dependency[0]
assert 1 == flattenedDependency.exclusions.size()
assert "0.6.1" == flattenedDependency.version.text()
