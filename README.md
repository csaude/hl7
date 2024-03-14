# HL7 tool

Extract patient demographic data from an EPTS server in HL7 format.

## Project structure

This is a maven project composed of two modules: configurer and web.
The configurer allows administrators to set encrypted configuration properties used by
the web app.
The web app is the interface for lab technicians to extract and search through
patient demographic data.
Since all data is stored in an encrypted format, a keyStore (`app.keyStore`)
and password (`app.keyStore.password`) configuration parameters must be
provided in order for the apps to function. The keyStore must contain two
entries that are the DISA secret key (`disaSecretKeyAlais`) and the C-Saúde
secret key (`cSaudeSecretKeyAlias`).

## Configurer

The configurer is a Spring Boot application with React as the frontend.
In order to develop the configurer, `Java 8`, `maven`, `nodejs v20` and `npm`
need to be installed.

It is recommended to use [Node Version Manager](https://github.com/nvm-sh/nvm)
to install nodejs.

Frontend code resides in the `src/main/js` directory, and it is built using
[frontend-maven-plugin](https://github.com/eirslett/frontend-maven-plugin) and
bundled using [webpack](https://webpack.js.org/concepts). Build artifacts will
be created in the `public/built` directory.

During development, it is necessary to watch the frontend code source directory
for changes and then rebuild the project using the following command:

```
npm run watch
```

This ensures that the application will load the latest assets.

For production builds, it is only necessary to run maven as
`frontend-maven-plugin` will take care of downloading and installing `nodejs`
and `npm` by itself. And then install dependencies before running the `webpack`
build.

## Web

The web app is a Sprint Boot application that allows the user to select health
facilities from which to extract demographic data. The data is stored in an
encrypted format, with the keys loaded from a keyStore.
