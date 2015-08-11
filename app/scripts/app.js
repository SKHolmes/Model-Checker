(function(document) {
  'use strict';

  var app = document.querySelector('#app');

  window.addEventListener('WebComponentsReady', function() {

    app.data = { automatas: [] };

    app.compile = function(){
      app.$.parser.code = app.$['text-editor'].getCode();
      var automatas = app.$.parser.parse();

      app.data = { automatas: [] };

      setTimeout(function(){
        app.data.automatas = automatas;
        app.notifyPath('data.automatas', app.data.automatas);
      }, 0);
    };

  });
})(document);