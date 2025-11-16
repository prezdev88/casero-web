const projectVersionRegex = /(\<artifactId\>\s*casero-web\s*\<\/artifactId\>\s*\n?\s*\<version\>)([^<]+)(\<\/version\>)/i;

module.exports.readVersion = (contents) => {
  const match = contents.match(projectVersionRegex);
  if (!match) {
    throw new Error('Version tag not found in pom.xml');
  }
  return match[2].trim();
};

module.exports.writeVersion = (contents, version) => {
  if (!projectVersionRegex.test(contents)) {
    throw new Error('Version tag not found in pom.xml');
  }
  return contents.replace(projectVersionRegex, `$1${version}$3`);
};
