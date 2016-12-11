const fs = require('fs');
const vm = require("vm");
global.importScripts = (...files) => {
  let scripts;

  if (files.length > 0) {
    scripts = files.map(file => {
      //Essentially, we copy pasted from tiny-worker but changed it to use app/scripts/compiler as the source
      return fs.readFileSync("app/scripts/compiler/"+file, "utf8");
    }).join("\n");

    vm.createScript(scripts).runInThisContext();
  }
};
importScripts("includes.js");
onmessage = function (e) {

  //Node appears to handle exceptions differently. Lets catch them and pass them back instead of killing the app.
  try {
    postMessage({result:Compiler.localCompile(e.data.ast, e.data.context)});
  } catch (ex) {
    postMessage({result:{type: 'error', message: ex.toString(), stack: ex.stack}});
  }
  //Kill the worker as we start a new worker for each compilation
  terminate();
}