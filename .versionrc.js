module.exports = {
  bumpFiles: [
    {
      filename: 'package.json',
      type: 'json'
    },
    {
      filename: 'package-lock.json',
      type: 'json'
    },
    {
      filename: 'pom.xml',
      updater: 'scripts/pom-updater.js'
    }
  ]
};
