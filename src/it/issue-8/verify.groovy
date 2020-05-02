/**
 * original model
 */

File originalPom = new File( basedir, 'pom.xml' )
assert originalPom.exists()

def originalProject = new XmlSlurper().parse( originalPom )
assert 0 == originalProject.dependencies.dependency.size()

/**
 * flattened model
 */

File flattenedPom = new File( basedir, 'target/flatten/pom.xml.flatten' )
assert flattenedPom.exists()

def flattendProject = new XmlSlurper().parse( flattenedPom )
assert 1 == flattendProject.dependencies.dependency.size()

def flattenedDependency = flattendProject.dependencies.dependency[0]
assert "true" == flattenedDependency.optional.text()
