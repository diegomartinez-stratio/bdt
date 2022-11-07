const {defineConfig} = require('cypress');

module.exports = defineConfig({
  e2e: {
    setupNodeEvents(on, config) {
      require('./cypress/plugins/index.js')(on, config)
    },
    supportFile: false
  }
});
