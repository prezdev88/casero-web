module.exports = {
  types: [
    { type: 'feat', section: 'Features', hidden: false },
    { type: 'fix', section: 'Bug Fixes', hidden: false },
    { type: 'perf', section: 'Performance Improvements', hidden: false },
    { type: 'refactor', section: 'Refactors', hidden: false },
    { type: 'style', section: 'Styles', hidden: false },
    { type: 'docs', section: 'Documentation', hidden: false },
    { type: 'test', section: 'Tests', hidden: false },
    { type: 'build', section: 'Build System', hidden: false },
    { type: 'ci', section: 'Continuous Integration', hidden: false },
    { type: 'revert', section: 'Reverts', hidden: false }
  ],
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
