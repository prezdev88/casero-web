# Changelog

All notable changes to this project will be documented in this file. See [standard-version](https://github.com/conventional-changelog/standard-version) for commit guidelines.

## [1.10.0](https://github.com/prezdev88/casero-web/compare/v1.9.0...v1.10.0) (2025-11-21)


### Features

* add fault discount functionality and UI integration ([c3ef8c4](https://github.com/prezdev88/casero-web/commit/c3ef8c45714e94e7532dbffb6fa96bd22f85b249))
* add font-family inheritance to textarea styling ([c80cf97](https://github.com/prezdev88/casero-web/commit/c80cf975cadffea507e285ec01028dda1be37bc9))
* add future balance preview to payment, refund, and sale forms ([8ef5d42](https://github.com/prezdev88/casero-web/commit/8ef5d422e794fd0043474b62c8193ce944f85280))
* add loading text and disable on submit functionality to forms in fault discount, forgiveness, payment, refund, and sale templates ([d74f64a](https://github.com/prezdev88/casero-web/commit/d74f64aafc8490ea53ea6d47578f5ddda079a022))
* implement money input formatting for payment, refund, and sale forms ([3e5c36d](https://github.com/prezdev88/casero-web/commit/3e5c36d310e3565f9111523cbe6b9a87e66dba31))
* standardize debt display styling to use red color for negative debts ([a659c85](https://github.com/prezdev88/casero-web/commit/a659c8524074d576eea033291ff1e8e8db6aca36))
* update debt display styling to use color coding for positive and negative debts ([e65f083](https://github.com/prezdev88/casero-web/commit/e65f083a5538cf0101ac9acde83a4284d3fbb613))

## [1.9.0](https://github.com/prezdev88/casero-web/compare/v1.8.0...v1.9.0) (2025-11-21)


### Features

* add filtering by transaction type in transaction listing ([e87ef73](https://github.com/prezdev88/casero-web/commit/e87ef73ca1ba7ae8241e26e0ec313560b5485c79))
* add monthly transaction summary feature with chart visualization ([327bfdf](https://github.com/prezdev88/casero-web/commit/327bfdf708c5b80fe151245c94543292aa35fa14))
* add theme toggle functionality with dark/light mode support ([299d601](https://github.com/prezdev88/casero-web/commit/299d601b2ea26ca5e4790651ce2da49db375d2bd))
* enhance form layout with flexbox for date inputs in monthly statistics ([e783586](https://github.com/prezdev88/casero-web/commit/e78358616bce3387f4908f3e2f86d08f89a562be))
* enhance transaction type display with icons and labels in customer and transaction views ([90db587](https://github.com/prezdev88/casero-web/commit/90db587b34cb031cb8bdcaabc6b7df6bd5a00208))
* implement Kafka setup and teardown scripts with configuration support ([a6c9b61](https://github.com/prezdev88/casero-web/commit/a6c9b61123f7c91a47b495853a26a35b5640408b))


### Bug Fixes

* update stylesheet link to use current timestamp for cache busting ([2d99df4](https://github.com/prezdev88/casero-web/commit/2d99df4ac7762951bbcf5c7f5b28861e39b2eff0))

## [1.8.0](https://github.com/prezdev88/casero-web/compare/v1.7.0...v1.8.0) (2025-11-19)


### Features

* add Docker and deployment configurations for production and QA environments ([d2a2d50](https://github.com/prezdev88/casero-web/commit/d2a2d50ba5f32bedebc5ab0565684c3667faf9d8))
* add Docker configuration for production and QA environments, including deployment scripts and PostgreSQL database setup ([a8344dd](https://github.com/prezdev88/casero-web/commit/a8344dde74fce92a8bf43a7abdab6c532b2eaece))
* add production and QA deployment scripts, and initial PostgreSQL database dump ([953c392](https://github.com/prezdev88/casero-web/commit/953c392ff6d9009d8527774aafd006c084c6ba43))
* add restore-database script for backup restoration in QA and production environments ([2cc8f10](https://github.com/prezdev88/casero-web/commit/2cc8f106d38e459338fd0ecad0a9bbbb6bdfcc01))
* enhance DateTimeUtil to format OffsetDateTime with Santiago timezone ([d8f4f13](https://github.com/prezdev88/casero-web/commit/d8f4f13c1a5d17b5af9033dc7c424e663c29a4e5))
* implement transaction listing functionality with pagination and create transaction list view ([c1ea741](https://github.com/prezdev88/casero-web/commit/c1ea741e7e3ccdaf947edd65c5ad1e42704b280d))
* update docker-compose.yml to add QA database and application services ([4c7519c](https://github.com/prezdev88/casero-web/commit/4c7519cfd510bb6631b84adef30146e16c7197a0))


### Bug Fixes

* correct file paths in Dockerfile for pom.xml and src directory ([e094bf5](https://github.com/prezdev88/casero-web/commit/e094bf5ceef9b84060b1f4fecfe5051631a76412))
* update database service ports to allow external access ([a9339e7](https://github.com/prezdev88/casero-web/commit/a9339e752d89aba5b0ad39e21da4f6ac831f229f))
* update MAILGUN_TO email address in mailgun-export-example ([f351c4c](https://github.com/prezdev88/casero-web/commit/f351c4c6c9a97891679cf58e7777c142dab41dc6))
* update QA app database credentials to match production settings ([2e08c4e](https://github.com/prezdev88/casero-web/commit/2e08c4e1610678e398f0dde49209db402305eb0c))
* update QA database credentials to match production settings ([44ea125](https://github.com/prezdev88/casero-web/commit/44ea12545c0d3f6142a63d41fb57d5b63abb8304))

## [1.7.0](https://github.com/prezdev88/casero-web/compare/v1.6.0...v1.7.0) (2025-11-17)


### Features

* add Mailgun integration scripts for automated backup notifications ([16c97e9](https://github.com/prezdev88/casero-web/commit/16c97e907367bb43e409acb455e6a2ea26a5b257))

## [1.6.0](https://github.com/prezdev88/casero-web/compare/v1.5.4...v1.6.0) (2025-11-16)


### Features

* add standard version ([79d4cd3](https://github.com/prezdev88/casero-web/commit/79d4cd3d4dea9432842e7a9c179fa62cb30eefcd))
* replace .versionrc.json with .versionrc.js and add pom-updater script for version management ([1e29576](https://github.com/prezdev88/casero-web/commit/1e29576839ab4e54ca4f3d81bc9c8c8e317a431a))
* update color variables in styles and change header title in customer creation form ([3c4cbc5](https://github.com/prezdev88/casero-web/commit/3c4cbc53777afba8147ab8485413525952302a29))
* update version to 1.5.5 in pom.xml ([48af3f2](https://github.com/prezdev88/casero-web/commit/48af3f27493e322fe707d9ef7ceec63465e97650))
